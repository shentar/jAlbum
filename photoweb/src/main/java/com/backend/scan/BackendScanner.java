package com.backend.scan;

import com.backend.dirwathch.DirWatchService;
import com.backend.facer.FaceRecService;
import com.backend.facer.FaceSetManager;
import com.backend.threadpool.ThreadPoolFactory;
import com.utils.conf.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class BackendScanner
{
    private static final Logger logger = LoggerFactory.getLogger(BackendScanner.class);

    private static final BackendScanner instance = new BackendScanner();

    private final ExecutorService threadPool =
            ThreadPoolFactory.getThreadPool(ThreadPoolFactory.BACKEND_SCANNER);

    private Runnable scanallTask = new Runnable()
    {
        public void run()
        {
            try
            {
                ToolMain.scanfiles();
            }
            catch (Throwable e)
            {
                logger.error("caught: ", e);
            }
        }
    };
    private Future<?> scanallTaskFuture = null;

    private Runnable backupSyncTask = new Runnable()
    {
        public void run()
        {
            try
            {
                ToolMain.scanMetaTableForBackup();
            }
            catch (Throwable e)
            {
                logger.error("caught: ", e);
            }
        }
    };
    private Future<?> backupSyncTaskFuture = null;

    private Runnable facerScanTask = new Runnable()
    {
        public void run()
        {
            try
            {
                if (AppConfig.getInstance().isFacerConfigured())
                {
                    FaceSetManager.getInstance().checkFaceSet();
                    FaceRecService.getInstance().checkAllFacesID();
                    FaceRecService.getInstance().checkAndGetFaceidList();
                }
            }
            catch (Throwable e)
            {
                logger.error("caught: ", e);
            }
        }
    };

    private Future<?> facerScanTaskFuture = null;


    private Runnable watchServiceRestartTask = new Runnable()
    {
        public void run()
        {
            try
            {
                DirWatchService.getInstance().restartScanAllFolders();
            }
            catch (Throwable e)
            {
                logger.error("caught: ", e);
            }
        }
    };

    private Future<?> watchServiceRestartTaskFuture = null;

    private BackendScanner()
    {

    }

    public static BackendScanner getInstance()
    {
        return instance;
    }

    public synchronized boolean scheduleOneTask()
    {
        boolean isFirstTime = scanallTaskFuture == null && facerScanTaskFuture == null
                && watchServiceRestartTaskFuture == null;
        boolean isDone = scanallTaskFuture != null && scanallTaskFuture.isDone()
                && facerScanTaskFuture != null && facerScanTaskFuture.isDone()
                && watchServiceRestartTaskFuture != null && watchServiceRestartTaskFuture.isDone();

        if (isFirstTime || isDone)
        {
            ToolMain.setFirstRun(true);
            logger.warn("start a new Scan Task: isFirstTime {}, isDone {}.", isFirstTime, isDone);
            scanallTaskFuture = threadPool.submit(scanallTask);
            facerScanTaskFuture = threadPool.submit(facerScanTask);
            watchServiceRestartTaskFuture = threadPool.submit(watchServiceRestartTask);
            logger.warn("scheduled a new Scan Task.");
            return true;
        }

        logger.warn("the scan task is already scheduled.");

        return false;
    }

    public synchronized boolean scheduleOneBackupTask()
    {
        boolean isFirstTime = backupSyncTaskFuture == null;
        boolean isDone = !isFirstTime && backupSyncTaskFuture.isDone();

        if (backupSyncTaskFuture == null || backupSyncTaskFuture.isDone())
        {
            ToolMain.setFirstRun(true);
            logger.warn("start a new Sync Task: isFirstTime {}, isDone {}.", isFirstTime, isDone);
            backupSyncTaskFuture = threadPool.submit(backupSyncTask);
            logger.warn("scheduled a new Sync Task: isFirstTime {}, isDone {}.", isFirstTime,
                        isDone);
            return true;
        }

        logger.warn("the sync task is already scheduled: isFirstTime {}, isDone {}.", false, false);

        return false;
    }
}

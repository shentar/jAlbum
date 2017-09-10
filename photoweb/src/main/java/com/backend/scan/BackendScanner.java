package com.backend.scan;

import com.backend.dirwathch.DirWatchService;
import com.backend.facer.FaceRecService;
import com.backend.facer.FaceSetManager;
import com.utils.conf.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BackendScanner
{
    private static final Logger logger = LoggerFactory.getLogger(BackendScanner.class);

    private static final BackendScanner instance = new BackendScanner();

    private boolean isFirstTime = true;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(2);

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

    private BackendScanner()
    {

    }

    public static BackendScanner getInstance()
    {
        return instance;
    }

    public synchronized boolean scheduleOneTask()
    {
        boolean isDone = scanallTaskFuture != null && scanallTaskFuture.isDone()
                && facerScanTaskFuture != null && facerScanTaskFuture.isDone();

        if (isFirstTime || isDone)
        {
            ToolMain.setFirstRun(true);
            logger.warn("start a new Scan Task: isFirstTime {}, isDone {}.", isFirstTime, isDone);
            isFirstTime = false;
            scanallTaskFuture = threadPool.submit(scanallTask);
            facerScanTaskFuture = threadPool.submit(facerScanTask);
            DirWatchService.getInstance().restartScanAllFolders();
            logger.warn("scheduled a new Scan Task: isFirstTime {}, isDone {}.", isFirstTime,
                        isDone);
            return true;
        }

        logger.warn("the task is already scheduled: isFirstTime {}, isDone {}.", false,
                    false);

        return false;
    }
}

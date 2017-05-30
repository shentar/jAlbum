package com.backend.scan;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dirwathch.DirWatchService;
import com.backend.facer.FaceRecService;
import com.backend.facer.FaceSetManager;
import com.utils.conf.AppConfig;

public class BackendScaner
{
    private static final Logger logger = LoggerFactory.getLogger(BackendScaner.class);

    private static final BackendScaner instance = new BackendScaner();

    private boolean isRunning = false;

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

    private BackendScaner()
    {

    }

    public static BackendScaner getInstance()
    {
        return instance;
    }

    public synchronized boolean scheduleOneTask()
    {
        boolean isDone = scanallTaskFuture != null && scanallTaskFuture.isDone()
                && facerScanTaskFuture != null && facerScanTaskFuture.isDone();

        if (isRunning && !isDone)
        {
            return false;
        }

        scanallTaskFuture = threadPool.submit(scanallTask);
        facerScanTaskFuture = threadPool.submit(facerScanTask);
        DirWatchService.getInstance().restartScanAllFolders();

        return true;
    }
}

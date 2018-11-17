package com.service.listener;

import java.util.Calendar;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dao.SqliteConnManger;
import com.backend.scan.BackendScanner;
import com.backend.scan.FileTools;
import com.backend.scan.FreshAllData;

public class SpecialListener implements ServletContextListener
{
    private static Logger logger = LoggerFactory.getLogger(SpecialListener.class);

    private static Future<?> fFreshAllData = null;

    private static Future<?> fBackupScanTask = null;

    private static final int HOUR_TO_BACKUP = 22;

    static
    {
        DOMConfigurator.configureAndWatch("log4j.xml", 6000);
        SqliteConnManger.getInstance().init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0)
    {
        logger.warn("began to stop the web service.");
        fFreshAllData.cancel(true);
        fBackupScanTask.cancel(true);
        FileTools.threadPool.shutdownNow();
        FileTools.threadPool4Thumbnail.shutdownNow();
        logger.warn("stopped the web service");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0)
    {
        logger.warn("start!");
        startBackGroundTask();
    }

    private void startBackGroundTask()
    {
        // 每天备份数据到远端云存储。
        fBackupScanTask = new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(new Runnable()
        {
            public void run()
            {
                if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == HOUR_TO_BACKUP)
                {
                    BackendScanner.getInstance().scheduleOneTask();
                }
            }
        }, 10, 5 * 60, TimeUnit.SECONDS);


        // 5秒检查是否需要刷新数据表。
        fFreshAllData = new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(new Runnable()
        {
            public void run()
            {
                try
                {
                    FreshAllData.getInstance().freshAll();
                }
                catch (Throwable e)
                {
                    logger.error("caught: ", e);
                }
            }
        }, 10, 5, TimeUnit.SECONDS);
    }
}

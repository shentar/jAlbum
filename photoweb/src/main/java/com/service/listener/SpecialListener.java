package com.service.listener;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dao.SqliteConnManger;
import com.backend.scan.BackendScaner;
import com.backend.scan.FileTools;
import com.backend.scan.FreshAllData;

public class SpecialListener implements ServletContextListener
{
    private static Logger logger = LoggerFactory.getLogger(SpecialListener.class);

    private static Future<?> f = null;

    static
    {
        DOMConfigurator.configureAndWatch("log4j.xml", 6000);
        SqliteConnManger.getInstance().init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0)
    {
        logger.warn("began to stop the web service.");
        f.cancel(true);
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
        BackendScaner.getInstance().scheduleOneTask();

        // 5秒检查是否需要刷新数据表。
        f = new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(new Runnable()
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

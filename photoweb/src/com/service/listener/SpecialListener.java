package com.service.listener;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.SqliteConnManger;
import com.backend.ToolMain;

public class SpecialListener implements ServletContextListener
{
    private static Logger logger = LoggerFactory.getLogger(SpecialListener.class);

    private static Future<?> f = null;

    static
    {
        DOMConfigurator.configureAndWatch("log4j.xml");
        SqliteConnManger.getInstance().init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0)
    {
        f.cancel(true);
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0)
    {
        logger.warn("start!");
        startBackUpTasks();
    }

    private void startBackUpTasks()
    {
        // FileTools.inputdir = "\\\\10.10.10.101\\root\\Ent";
        f = new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(new Runnable()
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
        }, 10, 300, TimeUnit.SECONDS);
    }
}

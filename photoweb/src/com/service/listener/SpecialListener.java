package com.service.listener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.SqliteConnManger;
import com.backend.ToolMain;
import com.backend.dirwathch.WatchDir;
import com.utils.conf.AppConfig;

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
        // 全盘扫描，每次启动时执行一次，之后每天执行一次。
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
        }, 10, 86400, TimeUnit.SECONDS);

        try
        {
            final List<String> elst = AppConfig.getInstance().getExcludedir();
            for (final String dir : AppConfig.getInstance().getInputDir())
            {
                watchAnDir(elst, dir);
            }
        }
        catch (IOException e)
        {
            logger.warn("start watch the folders failed!");
        }
    }

    private void watchAnDir(List<String> elst, String dir)
    {
        // 每5秒中处理一次文件系统变化。
        FileAlterationMonitor monitor = new FileAlterationMonitor(5000);
        FileAlterationObserver observer = new FileAlterationObserver(new File(dir));
        FileAlterationListener lis = new WatchDir(dir, elst);
        monitor.addObserver(observer);
        observer.addListener(lis);
        try
        {
            monitor.start();
        }
        catch (Exception e)
        {
            logger.warn("caught: ", e);
        }
    }
}

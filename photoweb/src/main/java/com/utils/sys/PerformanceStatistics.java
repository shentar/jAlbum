package com.utils.sys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class PerformanceStatistics
{
    private static final Logger logger = LoggerFactory.getLogger(PerformanceStatistics.class);
    private volatile AtomicLong totalFileCount = new AtomicLong(0);
    private volatile AtomicLong careFileCount = new AtomicLong(0);
    private volatile AtomicLong timenow = new AtomicLong(System.currentTimeMillis());

    private static PerformanceStatistics instance = new PerformanceStatistics();

    private PerformanceStatistics()
    {

    }

    public static PerformanceStatistics getInstance()
    {
        return instance;
    }

    public void reset()
    {
        careFileCount.set(0);
        totalFileCount.set(0);
        timenow.set(System.currentTimeMillis());
    }

    public void addOneFile(boolean isCare)
    {
        totalFileCount.incrementAndGet();
        if (isCare)
        {
            careFileCount.incrementAndGet();
        }

        long now = System.currentTimeMillis();
        if (now - timenow.get() > 10000)
        {
            synchronized (this)
            {
                if (now - timenow.get() > 10000)
                {
                    printPerformanceLog(now);
                }
            }
        }
    }

    public void printPerformanceLog(long now)
    {
        timenow.set(now);
        logger.warn("total file count: " + totalFileCount.get() + " checked file count: "
                + careFileCount.get());
    }
}

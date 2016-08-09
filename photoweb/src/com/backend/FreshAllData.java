package com.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dirwathch.DirWatchService;

public class FreshAllData
{
    private static final Logger logger = LoggerFactory.getLogger(FreshAllData.class);

    private static final long FIVE_MINS_IN_MILLS = 5 * 60 * 1000;

    private static final long TEN_SECS_IN_MILLS = 10 * 1000;

    private static final FreshAllData instance = new FreshAllData();

    private static UniqPhotosStore photostore = UniqPhotosStore.getInstance();

    private static DateTableDao datestore = DateTableDao.getInstance();

    private long lastEventTime = 0;

    private long lastFreshTime = System.currentTimeMillis();

    private FreshAllData()
    {
    }

    public static FreshAllData getInstance()
    {
        return instance;
    }

    public void freshAll()
    {
        boolean needFresh = RefreshFlag.getInstance().getAndSet(false);

        if (needFresh)
        {
            lastEventTime = System.currentTimeMillis();
        }

        logger.info("the refresh info is: {}", this);

        if (lastEventTime != 0 && (System.currentTimeMillis() - lastEventTime >= TEN_SECS_IN_MILLS
                || System.currentTimeMillis() - lastFreshTime >= FIVE_MINS_IN_MILLS))
        {
            doRefresh();
        }

    }

    private void doRefresh()
    {
        logger.warn("start to refresh all tables: {}", this);
        photostore.getDupFiles();
        datestore.refreshDate();
        logger.warn("end to refresh all tables: {}", this);
        logger.warn("the count of dir which is monitered is "
                + DirWatchService.getInstance().getTheWatchDirCount() + ".");
        lastFreshTime = System.currentTimeMillis();
        lastEventTime = 0;
    }

    public String toString()
    {
        return String.format("[lastEventTime: %s, lastFreshTime: %s]", "" + lastEventTime,
                "" + lastFreshTime);
    }
}

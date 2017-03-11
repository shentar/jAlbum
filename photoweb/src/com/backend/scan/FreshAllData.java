package com.backend.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dao.DateTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.dirwathch.DirWatchService;
import com.utils.conf.AppConfig;

public class FreshAllData
{
    private static final Logger logger = LoggerFactory.getLogger(FreshAllData.class);

    private static final long IDLE_REFRESH_INTEVAL = AppConfig.getInstance().getIdleRefreshInteval();

    private static final long BUSY_REFRESH_INTERVAL = AppConfig.getInstance().getBusyRefreshInteval();

    private static final FreshAllData instance = new FreshAllData();

    private static UniqPhotosStore photostore = UniqPhotosStore.getInstance();

    private static DateTableDao datestore = DateTableDao.getInstance();

    private boolean firstTime = true;

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

        if (firstTime || lastEventTime != 0
                && (System.currentTimeMillis() - lastEventTime >= BUSY_REFRESH_INTERVAL
                        || System.currentTimeMillis() - lastFreshTime >= IDLE_REFRESH_INTEVAL))
        {
            doRefresh();
            firstTime = false;
        }
    }

    private void doRefresh()
    {
        logger.info("start to refresh all tables: {}", this);
        photostore.getDupFiles();
        datestore.refreshDate();
        logger.info("end to refresh all tables: {}", this);
        logger.info("the count of dir which is monitered is "
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

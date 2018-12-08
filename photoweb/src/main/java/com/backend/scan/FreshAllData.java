package com.backend.scan;

import com.backend.dao.DateTableDao;
import com.backend.dao.FaceTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.dirwathch.DirWatchService;
import com.backend.facer.FaceSetManager;
import com.utils.conf.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreshAllData
{
    private static final Logger logger = LoggerFactory.getLogger(FreshAllData.class);

    private static final long IDLE_REFRESH_INTERVAL = AppConfig.getInstance()
            .getIdleRefreshInteval();

    private static final long BUSY_REFRESH_INTERVAL = AppConfig.getInstance()
            .getBusyRefreshInteval();

    private static final FreshAllData instance = new FreshAllData();

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
                        || System.currentTimeMillis() - lastFreshTime >= IDLE_REFRESH_INTERVAL))
        {
            doRefresh();
            firstTime = false;
        }
    }

    private void doRefresh()
    {
        logger.info("start to refresh all tables: {}", this);

        // 剔除重复照片。
        UniqPhotosStore.getInstance().getDupFiles();

        // 刷新日期目录
        DateTableDao.getInstance().refreshDate();

        // 删除表中失效的照片。
        FaceTableDao.getInstance().deleteInvalidFaces();

        // 删除远端的失效facetokens
        FaceSetManager.getInstance().checkFaceSet();

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

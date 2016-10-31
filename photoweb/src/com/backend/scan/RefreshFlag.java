package com.backend.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshFlag
{
    private static final Logger logger = LoggerFactory.getLogger(RefreshFlag.class);

    private static RefreshFlag instance = new RefreshFlag();

    private boolean isNeedRefresh = false;

    public static RefreshFlag getInstance()
    {
        return instance;
    }

    public synchronized boolean getAndSet(boolean flag)
    {
        logger.info("try to set refresh: old:{}, new: {}", isNeedRefresh, flag);
        boolean f = isNeedRefresh;
        isNeedRefresh = flag;
        return f;
    }
}

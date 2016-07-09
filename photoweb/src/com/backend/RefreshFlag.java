package com.backend;

public class RefreshFlag
{
    private static RefreshFlag instance = new RefreshFlag();

    private boolean isNeedRefresh = false;

    public static RefreshFlag getInstance()
    {
        return instance;
    }

    public synchronized boolean getAndSet(boolean flag)
    {
        boolean f = isNeedRefresh;
        isNeedRefresh = flag;
        return f;
    }
}

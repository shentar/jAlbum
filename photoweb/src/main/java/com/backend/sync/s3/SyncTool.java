package com.backend.sync.s3;

import java.io.File;
import java.util.concurrent.ExecutorService;

import com.backend.entity.FileInfo;
import com.backend.threadpool.ThreadPoolFactory;
import com.utils.conf.AppConfig;

public class SyncTool
{
    private static final ExecutorService threadPool =
            ThreadPoolFactory.getThreadPool(ThreadPoolFactory.SYNC_TOOL);


    public static void submitSyncTask(FileInfo fi)
    {
        if ((AppConfig.getInstance().isS3Configed() || AppConfig.getInstance()
                .isHuaweiOBSConfiged()) && new File(fi.getPath()).exists())
        {
            threadPool.submit(new SyncS3Task(fi));
        }
    }
}

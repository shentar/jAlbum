package com.backend.sync.s3;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.backend.FileInfo;
import com.utils.conf.AppConfig;

public class SyncTool
{
    private static final ExecutorService threadPool = Executors
            .newFixedThreadPool(AppConfig.getInstance().getConcurrentThreads());

    public static void submitSyncTask(FileInfo fi)
    {
        if (AppConfig.getInstance().isS3Configed() && new File(fi.getPath()).exists())
        {
            threadPool.submit(new SyncS3Task(fi));
        }
    }
}

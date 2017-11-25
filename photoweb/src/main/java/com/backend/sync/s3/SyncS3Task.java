package com.backend.sync.s3;

import com.backend.FileInfo;
import com.utils.conf.AppConfig;

public class SyncS3Task implements Runnable
{
    private FileInfo fi = null;

    public SyncS3Task(FileInfo fi)
    {
        this.fi = fi;
    }

    @Override
    public void run()
    {
        if (fi == null)
        {
            return;
        }

        if (AppConfig.getInstance().isS3Configed())
        {
            AWSS3SyncService.getInstance().uploadObject(fi);
        }

        if (AppConfig.getInstance().isHuaweiOBSConfiged())
        {
            HuaweiOBSSyncService.getInstance().uploadObject(fi);
        }
    }
}

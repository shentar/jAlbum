package com.backend.sync.s3;

import com.backend.entity.FileInfo;

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

        AbstractSyncS3Service syncS3Service = S3ServiceFactory.getBackUpService();
        if (syncS3Service != null)
        {
            syncS3Service.uploadObject(fi);
        }
    }
}

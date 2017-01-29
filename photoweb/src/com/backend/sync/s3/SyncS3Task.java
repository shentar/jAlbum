package com.backend.sync.s3;

import com.backend.FileInfo;

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
        if (fi == null || !SyncS3Service.getInstane().isS3Configed())
        {
            return;
        }

        SyncS3Service.getInstane().uploadObject(fi);
    }

}

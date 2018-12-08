package com.backend.sync.s3;

import com.backend.dao.AWSBackupedFilesDao;
import com.backend.dao.BackupedFilesDao;
import com.backend.dao.HuaweiOBSBackupedFilesDao;
import com.utils.conf.AppConfig;

public class S3ServiceFactory
{
    public static AbstractSyncS3Service getBackUpService()
    {
        if (AppConfig.getInstance().isS3Configed())
        {
            return AWSS3SyncService.getInstance();
        }

        if (AppConfig.getInstance().isHuaweiOBSConfiged())
        {
            return HuaweiOBSSyncService.getInstance();
        }

        return null;
    }

    public static BackupedFilesDao getBackUpDao()
    {
        if (AppConfig.getInstance().isS3Configed())
        {
            return AWSBackupedFilesDao.getInstance();
        }

        if (AppConfig.getInstance().isHuaweiOBSConfiged())
        {
            return HuaweiOBSBackupedFilesDao.getInstance();
        }

        return null;
    }
}

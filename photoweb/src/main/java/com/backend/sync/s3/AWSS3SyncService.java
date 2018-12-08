package com.backend.sync.s3;

import com.backend.dao.AWSBackupedFilesDao;
import com.backend.dao.BackupedFilesDao;
import com.utils.conf.AppConfig;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

public class AWSS3SyncService extends AbstractSyncS3Service
{
    private AWSS3SyncService()
    {
    }

    private static class ServiceHolder
    {
        private static final AbstractSyncS3Service instance = new AWSS3SyncService();
    }

    public static AbstractSyncS3Service getInstance()
    {
        ServiceHolder.instance.init();
        return ServiceHolder.instance;
    }

    private Jets3tProperties getJets3tProperties()
    {
        Jets3tProperties jets3tproperties = new Jets3tProperties();
        jets3tproperties.setProperty("httpclient.connection-timeout-ms", "" + 60000);
        jets3tproperties.setProperty("httpclient.socket-timeout-ms", "" + 60000);
        // jets3tproperties.setProperty("s3service.s3-endpoint", "codefine.co");
        jets3tproperties
                .setProperty("s3service.https-only", "" + AppConfig.getInstance().useS3HTTPS());

        if (AppConfig.getInstance().isS3ProxyConfiged())
        {
            jets3tproperties
                    .setProperty("httpclient.proxy-host", AppConfig.getInstance().getS3ProxyHost());
            jets3tproperties.setProperty("httpclient.proxy-port",
                                         "" + AppConfig.getInstance().getS3ProxyPort());
            jets3tproperties
                    .setProperty("httpclient.proxy-user", AppConfig.getInstance().getS3ProxyUser());
            jets3tproperties.setProperty("httpclient.proxy-password",
                                         AppConfig.getInstance().getS3ProxyPWD());
            jets3tproperties.setProperty("httpclient.proxy-autodetect", "false");

        }
        return jets3tproperties;
    }

    @Override
    protected String getBucketName()
    {
        return AppConfig.getInstance().getS3BucketName();
    }

    @Override
    protected RestS3Service generateS3Service()
    {
        return new RestS3Service(new AWSCredentials(getAk(), getSk()), "jAlbum 0.2.2", null,
                                 getJets3tProperties());
    }

    @Override
    protected BackupedFilesDao getBackupedFileDao()
    {
        return AWSBackupedFilesDao.getInstance();
    }

    private String getAk()
    {
        return AppConfig.getInstance().getS3AK();
    }

    private String getSk()
    {
        return AppConfig.getInstance().getS3SK();
    }


}

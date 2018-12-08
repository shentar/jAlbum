package com.backend.sync.s3;

import com.backend.dao.BackupedFilesDao;
import com.backend.dao.HuaweiOBSBackupedFilesDao;
import com.utils.conf.AppConfig;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

public class HuaweiOBSSyncService extends AbstractSyncS3Service
{
    private HuaweiOBSSyncService()
    {
    }

    private static class ServiceHolder
    {
        private static final AbstractSyncS3Service instance = new HuaweiOBSSyncService();
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
        jets3tproperties
                .setProperty("s3service.s3-endpoint", AppConfig.getInstance().getHWOBSEndPoint());
        jets3tproperties.setProperty("s3service.https-only",
                                     "" + AppConfig.getInstance().useHuaweiOBSHTTPS());

        if (AppConfig.getInstance().isHWProxyConfiged())
        {
            jets3tproperties
                    .setProperty("httpclient.proxy-host", AppConfig.getInstance().getHWProxyHost());
            jets3tproperties.setProperty("httpclient.proxy-port",
                                         "" + AppConfig.getInstance().getHWProxyPort());
            jets3tproperties
                    .setProperty("httpclient.proxy-user", AppConfig.getInstance().getHWProxyUser());
            jets3tproperties.setProperty("httpclient.proxy-password",
                                         AppConfig.getInstance().getHWProxyPWD());
            jets3tproperties.setProperty("httpclient.proxy-autodetect", "false");

        }
        return jets3tproperties;
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
        return HuaweiOBSBackupedFilesDao.getInstance();
    }

    private String getAk()
    {
        return AppConfig.getInstance().getHuaweiOBSAK();
    }

    private String getSk()
    {
        return AppConfig.getInstance().getHuaweiOBSSK();
    }

    protected String getBucketName()
    {
        return AppConfig.getInstance().getHuaweiOBSBucketName();
    }
}

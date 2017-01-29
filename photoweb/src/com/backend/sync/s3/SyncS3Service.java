package com.backend.sync.s3;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.utils.conf.AppConfig;

public class SyncS3Service
{
    private static final String OBJECTPREFIX = "homenas/";
    private static final Logger logger = LoggerFactory.getLogger(SyncS3Service.class);

    private static Map<String, S3Object> allobjs = new ConcurrentHashMap<String, S3Object>();

    private static boolean isInit = false;

    private static class ServiceHolder
    {
        private static final SyncS3Service instance = new SyncS3Service();
    }

    public static SyncS3Service getInstane()
    {
        init();
        return ServiceHolder.instance;
    }

    private SyncS3Service()
    {
    }

    public boolean isS3Configed()
    {
        return StringUtils.isNotBlank(AppConfig.getInstance().getAK())
                && StringUtils.isNotBlank(AppConfig.getInstance().getSK())
                && StringUtils.isNotBlank(AppConfig.getInstance().getBucketName());
    }

    private static S3Service getS3Service()
    {
        return new RestS3Service(new AWSCredentials(AppConfig.getInstance().getAK(), AppConfig.getInstance().getSK()))
        {
            protected boolean getHttpsOnly()
            {
                return AppConfig.getInstance().useHTTPS();
            }

            public boolean isHttpsOnly()
            {
                return AppConfig.getInstance().useHTTPS();
            }
        };
    }

    public void uploadObject(FileInfo fi)
    {
        // TODO big file use Multi-Parts to upload big file.

        if (allobjs.containsKey(fi.getHash256()))
        {
            return;
        }

        S3Object so = new S3Object();
        so.setKey(genObjectKey(fi));
        so.setDataInputFile(new File(fi.getPath()));
        so.setContentLength(fi.getSize());
        try
        {
            S3Object o = getS3Service().putObject(AppConfig.getInstance().getBucketName(), so);
            logger.warn("upload the object successfully: {}", o);
            allobjs.put(o.getETag().toUpperCase(), o);
        }
        catch (S3ServiceException e)
        {
            logger.warn("caused by: ", e);
        }
    }

    private String genObjectKey(FileInfo fi)
    {
        return OBJECTPREFIX + timeToFolder(fi.getPhotoTime().getTime()) + fi.getHash256();
    }

    private String timeToFolder(long date)
    {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy/MM/dd/");
        return sf.format(new Date(date));
    }

    private synchronized static void init()
    {
        if (isInit)
        {
            return;
        }

        isInit = true;
        try
        {
            logger.warn("try to get the object list: start.");
            S3Object[] obs = getS3Service().listObjects(AppConfig.getInstance().getBucketName());
            for (S3Object o : obs)
            {
                if (o.getContentLength() > 0)
                {
                    allobjs.put(o.getETag().toUpperCase(), o);
                }
            }
            logger.warn("the objlist size is: " + allobjs.size());
            logger.warn("try to get the object list: end.");
        }
        catch (S3ServiceException e)
        {
            logger.warn("caused by: ", e);
        }

    }
}

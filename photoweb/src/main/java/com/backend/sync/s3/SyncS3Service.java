package com.backend.sync.s3;

import com.backend.FileInfo;
import com.backend.dao.BackupedFilesDao;
import com.utils.conf.AppConfig;
import com.utils.media.MediaTool;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SyncS3Service
{
    private static final String OBJECTPREFIX = "homenas/";
    private static final Logger logger = LoggerFactory.getLogger(SyncS3Service.class);

    private Map<String, StorageObject> allobjs = new ConcurrentHashMap<>();

    private boolean isInit = false;

    private AtomicLong backedUpSize = new AtomicLong();

    private S3Service s3service = null;

    private AtomicInteger failedTimes = new AtomicInteger(0);

    private static class ServiceHolder
    {
        private static final SyncS3Service instance = new SyncS3Service();
    }

    public static SyncS3Service getInstance()
    {
        ServiceHolder.instance.init();
        return ServiceHolder.instance;
    }

    private SyncS3Service()
    {
        Jets3tProperties jets3tproperties = new Jets3tProperties();
        jets3tproperties.setProperty("httpclient.connection-timeout-ms", "" + 60000);
        jets3tproperties.setProperty("httpclient.socket-timeout-ms", "" + 60000);
        // jets3tproperties.setProperty("s3service.s3-endpoint", "codefine.co");
        jets3tproperties.setProperty("s3service.https-only",
                                     "" + AppConfig.getInstance().useHTTPS());

        if (AppConfig.getInstance().isS3ProxyConfiged())
        {
            jets3tproperties.setProperty("httpclient.proxy-host",
                                         AppConfig.getInstance().getS3ProxyHost());
            jets3tproperties.setProperty("httpclient.proxy-port",
                                         "" + AppConfig.getInstance().getS3ProxyPort());
            jets3tproperties.setProperty("httpclient.proxy-user",
                                         AppConfig.getInstance().getS3ProxyUser());
            jets3tproperties.setProperty("httpclient.proxy-password",
                                         AppConfig.getInstance().getS3ProxyPWD());
            jets3tproperties.setProperty("httpclient.proxy-autodetect", "false");

        }
        s3service = new RestS3Service(new AWSCredentials(AppConfig.getInstance().getAK(),
                                                         AppConfig.getInstance().getSK()), "jAlbum 0.2.2", null,
                                      jets3tproperties);
    }

    public boolean checkAlreadyBackuped(FileInfo fi)
    {
        if (fi != null && allobjs.containsKey(fi.getHash256()))
        {
            logger.info("already exist: {}", fi);
            return true;
        }
        else
        {
            logger.info("need to upload the object: {}", fi);
            return false;
        }
    }

    private void delayRetry()
    {
        try
        {
            int ft = failedTimes.incrementAndGet();

            ft = ft > 8 ? 8 : ft;

            Thread.sleep((long) (AppConfig.getInstance().getRetryInitS3() * Math.pow(2, ft)));
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
    }

    public void uploadObject(FileInfo fi)
    {
        try
        {
            // TODO use Multi-Parts to upload big file.
            if (!isInit)
            {
                logger.warn("the sync service is not prepared now!");
                delayRetry();
                SyncTool.submitSyncTask(fi);
                return;
            }

            if (fi == null || checkAlreadyBackuped(fi))
            {
                return;
            }

            S3Object so = new S3Object();
            so.setKey(genObjectKey(fi));
            so.setDataInputFile(new File(fi.getPath()));
            so.setContentLength(fi.getSize());
            so.addMetadata(Constants.REST_METADATA_PREFIX + "type",
                           HeadUtils.getFileType(fi.getPath()).name());

            S3Object o = s3service.putObject(AppConfig.getInstance().getBucketName(), so);
            logger.warn("upload the object successfully: {}", o);
            logger.warn("the size of all object in s3 is: space size: {}, object count: [{}]",
                        String.format("%4.3fGB", ((float) backedUpSize.get()) / (1024 * 1024 * 1024)),
                        allobjs.size());
            failedTimes.set(0);
            if (StringUtils.equalsIgnoreCase(o.getETag(), fi.getHash256())
                    || MediaTool.isVideo(fi.getPath()))
            {
                // 使用jalbum提取的指纹计算得到的etag来做key，而不是使用真实的文件的etag。
                allobjs.put(fi.getHash256().toUpperCase(), o);
                backedUpSize.addAndGet(o.getContentLength());
                BackupedFilesDao.getInstance().checkAndaddOneRecords(fi.getHash256().toUpperCase(),
                                                                     o.getETag(), o.getKey());
            }
            else
            {
                logger.warn("the object etag is not equals file info is: {}", fi);
            }
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
            // retry this task. limit retry times?
            delayRetry();
            SyncTool.submitSyncTask(fi);
        }

    }

    private String genObjectKey(FileInfo fi)
    {
        return OBJECTPREFIX + timeToFolder(fi.getPhotoTime().getTime()) + fi.getHash256();
    }

    private static String getHashStrFromObjectKey(String objKey)
    {
        int pos = StringUtils.lastIndexOf(objKey, "/");
        if (pos > 0)
        {
            String hashStr = StringUtils.substring(objKey, pos + 1);
            if (StringUtils.isNotBlank(hashStr) && hashStr.length() == 32)
            {
                return hashStr;
            }
        }

        return null;
    }

    private String timeToFolder(long date)
    {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy/MM/dd/");
        return sf.format(new Date(date));
    }

    private synchronized void init()
    {
        if (isInit)
        {
            return;
        }

        BackupedFilesDao.getInstance().checkAndCreateTable();

        logger.warn("start to init sync srvice. try to get the object list.");

        List<String> lst = BackupedFilesDao.getInstance().getAllHashStr();
        for (String s : lst)
        {
            allobjs.put(s.toUpperCase(), new StorageObject());
        }

        StorageObjectsChunk sc;
        String priorLastKey = null;
        while (true)
        {
            try
            {
                sc = s3service.listObjectsChunked(AppConfig.getInstance().getBucketName(),
                                                  "homenas/", null, 1000, priorLastKey, false);
                failedTimes.set(0);
                StorageObject[] obs = sc.getObjects();

                if (obs == null)
                {
                    isInit = true;
                    logger.warn("there is no object in the bucket: {}",
                                AppConfig.getInstance().getBucketName());
                    break;
                }

                logger.warn("get obj list size: {}", obs.length);
                for (StorageObject o : obs)
                {
                    if (o.getContentLength() <= 0)
                    {
                        continue;
                    }

                    backedUpSize.addAndGet(o.getContentLength());
                    String hashStr = getHashStrFromObjectKey(o.getKey());
                    if (hashStr == null)
                    {
                        hashStr = o.getETag().toUpperCase();
                    }

                    if (allobjs.containsKey(hashStr))
                    {
                        continue;
                    }

                    logger.warn("the file is in s3 but not in the local table: {}", o);
                    BackupedFilesDao.getInstance().checkAndaddOneRecords(hashStr, o.getETag(),
                                                                         o.getKey());
                    allobjs.put(hashStr, o);

                }

                if (sc.isListingComplete())
                {
                    isInit = true;
                    logger.warn(
                            "init successfully! the size of all object in s3 is: space size: {}, object count: [{}]",
                            String.format("%4.3fGB",
                                          ((float) backedUpSize.get()) / (1024 * 1024 * 1024)),
                            allobjs.size());
                    break;
                }
                priorLastKey = sc.getPriorLastKey();
            }
            catch (Exception e)
            {
                logger.warn("caused by: ", e);
                delayRetry();
                isInit = true;
                break;
            }
        }
    }

}

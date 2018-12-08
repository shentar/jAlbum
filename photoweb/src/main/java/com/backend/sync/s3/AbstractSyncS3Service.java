package com.backend.sync.s3;

import com.backend.dao.BackupedFilesDao;
import com.backend.entity.FileInfo;
import com.utils.conf.AppConfig;
import com.utils.media.MediaTool;
import com.utils.web.HeadUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jets3t.service.Constants;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractSyncS3Service
{
    private static final String OBJECT_PREFIX = "homenas/";

    private static final Logger logger = LoggerFactory.getLogger(AbstractSyncS3Service.class);

    private volatile boolean isInit = false;

    private AtomicLong backedUpSize = new AtomicLong();

    private AtomicLong backedUpCount = new AtomicLong();

    private RestS3Service s3service = null;

    private AtomicInteger failedTimes = new AtomicInteger(0);

    private boolean checkAlreadyBackuped(FileInfo fi)
    {
        BackupedFilesDao backupedFilesDao = S3ServiceFactory.getBackUpDao();
        if (fi != null && backupedFilesDao != null && backupedFilesDao.isBackup(fi.getHash256()))
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

    public void restoreObject(FileInfo fi)
    {

    }

    public boolean downloadObject(FileInfo fi)
    {

        if (fi == null)
        {
            return false;
        }

        if (fi.exist())
        {
            logger.warn("the file is already exist: {}", fi);
            return false;
        }

        try
        {
            S3Object so = s3service.getObject(getBucketName(), genObjectKey(fi));
            if (!MediaTool.isVideo(fi.getPath()))
            {
                if (!StringUtils.equalsIgnoreCase(so.getETag(), fi.getHash256()))
                {
                    logger.warn("some error file: {}, s3object: {}", fi, so);
                }
            }

            if (so.getContentLength() != 0)
            {
                writeToFile(fi, so);
                return true;
            }
            else
            {
                logger.warn("an empty s3Object, file: {}, s3object: {}", fi, so);
            }


        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
            delayRetry();
        }

        return false;
    }

    public boolean objectExist(FileInfo fi)
    {
        if (fi == null)
        {
            return false;
        }
        try
        {
            StorageObject so = s3service.getObjectDetails(getBucketName(), genObjectKey(fi));
            return true;
        }
        catch (ServiceException e)
        {
            if (404 == e.getResponseCode() || "NoSuchKey".equals(e.getErrorCode()) || "NoSuchBucket"
                    .equals(e.getErrorCode()))
            {
                return false;
            }

            if ("AccessDenied".equals(e.getErrorCode()))
            {
                logger.warn("403 forbidden, fi: {}", fi);
                return true;
            }

            logger.warn("caused by: ", e);
        }
        return false;
    }

    private void writeToFile(FileInfo fi, S3Object so) throws ServiceException, IOException
    {
        InputStream in = null;
        OutputStream ou = null;
        try
        {
            in = so.getDataInputStream();
            ou = new FileOutputStream(fi.getPath());
            IOUtils.copy(in, ou, 1024 * 16);
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
            }
            catch (Exception e)
            {
                logger.warn("caused by: ", e);
            }

            try
            {
                if (ou != null)
                {
                    ou.close();
                }
            }
            catch (Exception e)
            {
                logger.warn("caused by: ", e);
            }
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

            // 避免同一个文件备份两次。
            synchronized (fi.getHash256().intern())
            {
                if (checkAlreadyBackuped(fi))
                {
                    logger.warn("the file is already backedUp: {}", fi);
                    return;
                }

                S3Object so = new S3Object();
                so.setKey(genObjectKey(fi));
                so.setDataInputFile(new File(fi.getPath()));
                so.setContentLength(fi.getSize());
                so.addMetadata(Constants.REST_METADATA_PREFIX + "type",
                               HeadUtils.getFileType(fi.getPath()).name());
                S3Object o = s3service.putObject(getBucketName(), so);
                failedTimes.set(0);

                if (StringUtils.equalsIgnoreCase(o.getETag(), fi.getHash256()) || MediaTool
                        .isVideo(fi.getPath()))
                {
                    // 使用jalbum提取的指纹计算得到的etag来做key，而不是使用真实的文件的etag。
                    // 注意此处，有可能两个线程同时上传同一张照片，但是文件不同。此时可能导致总存量计算不正确。
                    if (getBackupedFileDao()
                            .addOneRecords(fi.getHash256().toUpperCase(), o.getETag(), o.getKey()))
                    {
                        addStatistics(o.getContentLength());
                    }
                }
                else
                {
                    logger.error("the object etag is not equals file info is: {}", fi);
                }
                logger.warn("upload the object successfully: {}", o);
                logger.warn("the size of all object in s3 is: space size: {}, object count: [{}]",
                            String.format("%4.3fGB",
                                          ((float) backedUpSize.get()) / (1024 * 1024 * 1024)),
                            backedUpCount.get());
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

    private void addStatistics(long contentLength)
    {
        backedUpSize.addAndGet(contentLength);
        backedUpCount.incrementAndGet();
    }


    private String genObjectKey(FileInfo fi)
    {
        return OBJECT_PREFIX + timeToFolder(fi.getPhotoTime().getTime()) + fi.getHash256();
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

    protected synchronized void init()
    {
        if (isInit)
        {
            return;
        }

        s3service = generateS3Service();

        logger.warn("start to init sync srvice. try to get the object list.");
        getBackupedFileDao().checkAndCreateTable();
        isInit = true;
        logger.warn(
                "init successfully! the size of all object in s3 is: space size: {}, object count: [{}]",
                String.format("%4.3fGB", ((float) backedUpSize.get()) / (1024 * 1024 * 1024)),
                backedUpCount.get());

        /*
         StorageObjectsChunk sc;
         String priorLastKey = null;
         while (true)
         {
         try
         {
         sc = s3service
         .listObjectsChunked(getBucketName(), "homenas/", null, 1000, priorLastKey,
         false);
         failedTimes.set(0);
         StorageObject[] obs = sc.getObjects();

         if (obs == null)
         {
         isInit = true;
         logger.warn("there is no object in the bucket: {}", getBucketName());
         break;
         }

         logger.warn("get obj list size: {}", obs.length);
         for (StorageObject o : obs)
         {
         if (o.getContentLength() <= 0)
         {
         logger.warn("the object is abnormal: {}", o);
         continue;
         }

         addStatistics(o.getContentLength());
         String hashStr = getHashStrFromObjectKey(o.getKey());
         if (hashStr == null)
         {
         logger.warn("the object is abnormal: {}", o);
         hashStr = o.getETag().toUpperCase();
         }

         if (getBackupedFileDao().isBackup(hashStr))
         {
         continue;
         }

         logger.warn("the file is in s3 but not in the local table: {}", o);
         if (!getBackupedFileDao().addOneRecords(hashStr, o.getETag(), o.getKey()))
         {
         logger.warn("add the record failed: {}", o);
         }
         }

         if (sc.isListingComplete())
         {
         isInit = true;
         logger.warn(
         "init successfully! the size of all object in s3 is: space size: {}, object count: [{}]",
         String.format("%4.3fGB",
         ((float) backedUpSize.get()) / (1024 * 1024 * 1024)),
         backedUpCount.get());
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
         */
    }

    protected abstract String getBucketName();

    protected abstract RestS3Service generateS3Service();

    protected abstract BackupedFilesDao getBackupedFileDao();
}

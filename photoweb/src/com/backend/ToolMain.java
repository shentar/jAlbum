package com.backend;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolMain
{
    private static final Logger logger = LoggerFactory.getLogger(ToolMain.class);

    private static BaseSqliteStore metaDataStore = BaseSqliteStore.getInstance();

    private static UniqPhotosStore photostore = UniqPhotosStore.getInstance();

    private static DateTableDao datestore = DateTableDao.getInstance();

    private static AtomicLong filecount = new AtomicLong(0);

    public static void scanfiles()
    {
        try
        {
            logger.warn("start one roundle.");
            filecount.set(0);

            metaDataStore.scanAllRecords();

            mapAllfiles(new File(FileTools.inputdir));

            while (filecount.get() != 0)
            {
                Thread.sleep(100);
            }

            PerformanceStatistics.getInstance().printPerformanceLog(System.currentTimeMillis());

            photostore.getDupFiles();

            datestore.refreshDate();

            logger.warn("completed one roundle.");
        }
        catch (Throwable th)
        {
            logger.error("caught: ", th);
        }
    }

    public static void mapAllfiles(final File f) throws IOException, InterruptedException, NoSuchAlgorithmException
    {
        if (f == null)
        {
            return;
        }
        else
        {
            if (f.isFile())
            {
                filecount.incrementAndGet();
                FileTools.threadPool.submit(new Runnable()
                {
                    public void run()
                    {
                        checkOneFile(f);
                        filecount.decrementAndGet();
                    }
                });
            }
            else
            {
                File[] files = f.listFiles();
                if (files == null)
                {
                    logger.warn("some empty folder: " + f.getCanonicalPath());
                    return;
                }

                for (File cf : files)
                {
                    mapAllfiles(cf);
                }
            }
        }
    }

    private static void checkOneFile(File f)
    {
        try
        {
            boolean isCare = false;
            for (String s : FileTools.filesufixs)
            {
                if (f.getName().toLowerCase().endsWith(s) && f.length() > FileTools.minfilesize)
                {
                    isCare = true;
                    if (metaDataStore.checkIfAlreadyExist(f))
                    {
                        return;
                    }
                    else
                    {
                        metaDataStore.dealWithOneHash(f, FileSHA256Caculater.calFileSha256(f));
                    }
                    break;
                }
            }
            PerformanceStatistics.getInstance().addOneFile(isCare);
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
    }
}

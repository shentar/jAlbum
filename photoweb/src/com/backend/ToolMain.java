package com.backend;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.utils.conf.AppConfig;

public class ToolMain
{
    private static final Logger logger = LoggerFactory.getLogger(ToolMain.class);

    private static BaseSqliteStore metaDataStore = BaseSqliteStore.getInstance();

    private static UniqPhotosStore photostore = UniqPhotosStore.getInstance();

    private static DateTableDao datestore = DateTableDao.getInstance();

    private static AtomicLong filecount = new AtomicLong(0);

    private static boolean firstRun = true;

    public static void scanfiles()
    {
        try
        {
            if (firstRun)
            {
                firstRun = false;
                logger.warn("start one roundle.");
                filecount.set(0);
                PerformanceStatistics.getInstance().reset();
                List<String> excludeDirs = AppConfig.getInstance().getExcludedir();
                logger.warn("the exclude dirs are: " + excludeDirs);
                metaDataStore.scanAllRecords(excludeDirs);

                PerformanceStatistics.getInstance().reset();
                logger.warn("start to scan the filesystem which specified by the config file: "
                        + AppConfig.getInstance().getInputDir());
                for (String dir : AppConfig.getInstance().getInputDir())
                {
                    mapAllfiles(new File(dir), excludeDirs);
                }

                while (filecount.get() != 0)
                {
                    Thread.sleep(100);
                }
                PerformanceStatistics.getInstance().printPerformanceLog(System.currentTimeMillis());
                logger.warn("end to scan the filesystem.");
            }
        }
        catch (Throwable th)
        {
            logger.error("caught: ", th);
        }
    }

    public static void renewTheData()
    {
        boolean needFresh = RefreshFlag.getInstance().getAndSet(false);
        if (needFresh)
        {
            logger.warn("start to refresh all tables.");
            photostore.getDupFiles();
            datestore.refreshDate();
            logger.warn("completed one roundle.");
        }
    }

    public static void mapAllfiles(final File f, List<String> excludeDirs)
            throws IOException, InterruptedException, NoSuchAlgorithmException
    {
        if (f == null || excludeDirs == null)
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
                for (String s : excludeDirs)
                {
                    if (f.getCanonicalPath().startsWith(s))
                    {
                        logger.info("this folder is execluded: " + f.getCanonicalPath());
                        return;
                    }
                }

                File[] files = f.listFiles();
                if (files == null)
                {
                    logger.warn("some empty folder: " + f.getCanonicalPath());
                    return;
                }

                for (File cf : files)
                {
                    mapAllfiles(cf, excludeDirs);
                }
            }
        }
    }

    public static void checkOneFile(File f)
    {
        try
        {
            boolean isCare = false;
            for (String s : AppConfig.getInstance().getFileSuffix())
            {
                if (f.getName().toLowerCase().endsWith(s) && f.length() > AppConfig.getInstance().getMinFileSize())
                {
                    if (metaDataStore.checkIfAlreadyExist(f))
                    {
                        break;
                    }
                    else
                    {
                        isCare = true;
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

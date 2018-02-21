package com.backend.scan;

import com.backend.FileInfo;
import com.backend.PicStatus;
import com.backend.dao.BaseSqliteStore;
import com.utils.conf.AppConfig;
import com.utils.media.FileSHA256Caculater;
import com.utils.media.MediaTool;
import com.utils.sys.PerformanceStatistics;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ToolMain
{
    private static final Logger logger = LoggerFactory.getLogger(ToolMain.class);

    private static BaseSqliteStore metaDataStore = BaseSqliteStore.getInstance();

    private static AtomicLong filecount = new AtomicLong(0);

    private static boolean firstRun = true;

    public static void scanfiles()
    {
        synchronized (ToolMain.class)
        {
            try
            {
                if (!firstRun)
                {
                    return;
                }

                firstRun = false;
                List<String> excludeDirs = AppConfig.getInstance().getExcludedir();

                PerformanceStatistics.getInstance().reset();
                filecount.set(0);
                logger.warn("start to scan the filesystem which specified by the config file: "
                                    + AppConfig.getInstance().getInputDir());
                for (String dir : AppConfig.getInstance().getInputDir())
                {
                    mapAllfiles(new File(dir), excludeDirs);
                }

                logger.warn("all file count is " + filecount.get());
                while (filecount.get() != 0)
                {
                    Thread.sleep(100);
                }
                PerformanceStatistics.getInstance().printPerformanceLog(System.currentTimeMillis());
                logger.warn("end to scan the filesystem.");

                logger.warn("start to scan the base table one time after the program start.");
                PerformanceStatistics.getInstance().reset();
                logger.warn("the exclude dirs are: " + excludeDirs);
                metaDataStore.scanAllRecords(excludeDirs);
                logger.warn("end to scan the base table.");
            }
            catch (Throwable th)
            {
                logger.error("caught: ", th);
            }
        }
    }

    public static void scanMetaTableForBackup()
    {
        synchronized (ToolMain.class)
        {
            try
            {
                if (!firstRun)
                {
                    return;
                }

                List<String> excludeDirs = AppConfig.getInstance().getExcludedir();
                logger.warn("start to scan the base table one time after the program start.");
                PerformanceStatistics.getInstance().reset();
                logger.warn("the exclude dirs are: " + excludeDirs);
                metaDataStore.scanAllRecords(excludeDirs, true);
                logger.warn("end to scan the base table.");
            }
            catch (Throwable th)
            {
                logger.error("caught: ", th);
            }
        }
    }

    public static void mapAllfiles(final File f, List<String> excludeDirs)
    {
        if (f == null || excludeDirs == null)
        {
            return;
        }

        if (f.isFile())
        {
            logger.debug("find a file: " + f);
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
            String fpath = null;
            try
            {
                fpath = f.getCanonicalPath();
            }
            catch (Exception e)
            {
                logger.warn("caught: ", e);
            }

            if (StringUtils.isBlank(fpath))
            {
                return;
            }

            for (String s : excludeDirs)
            {
                if (fpath.startsWith(s))
                {
                    logger.info("this folder is execluded: " + fpath);
                    return;
                }
            }

            File[] files = f.listFiles();
            if (files == null)
            {
                logger.info("some empty folder: " + fpath);
                return;
            }

            for (File cf : files)
            {
                mapAllfiles(cf, excludeDirs);
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
                if (f.getName().toLowerCase().endsWith(s))
                {
                    if (!FileTools.checkFileLengthValid(f.getCanonicalPath()))
                    {
                        logger.info("the size is too small or too big. "
                                            + "it maybe not a normal photo file: " + f);
                        break;
                    }

                    if (PicStatus.EXIST == metaDataStore.checkIfAlreadyExist(f))
                    {
                        break;
                    }

                    isCare = true;

                    FileInfo fi = MediaTool.genFileInfo(f.getCanonicalPath());
                    if (fi == null)
                    {
                        logger.warn("error file" + f.getCanonicalPath());
                        return;
                    }

                    if (!MediaTool.isVideo(f.getCanonicalPath()))
                    {
                        fi.setHash256(FileSHA256Caculater.calFileSha256(f));
                    }
                    else
                    {
                        // 视频文件通常比较大，采用提取的特征值代替整文件计算MD5值
                        fi.setHash256(FileSHA256Caculater.calFileSha256(fi.getExtrInfo()));
                    }

                    metaDataStore.insertOneRecord(fi);
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

    public static void setFirstRun(boolean firstRun)
    {
        ToolMain.firstRun = firstRun;
    }

}

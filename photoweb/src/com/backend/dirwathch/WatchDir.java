package com.backend.dirwathch;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.BaseSqliteStore;
import com.backend.FileInfo;
import com.backend.ToolMain;
import com.utils.conf.AppConfig;

public class WatchDir implements FileAlterationListener
{
    private static final Logger logger = LoggerFactory.getLogger(WatchDir.class);

    private String dirPath;

    private List<String> excludeDirs;

    private boolean needRefreshAll;

    public WatchDir(String dir, List<String> elst)
    {
        this.setDirPath(dir);
        this.excludeDirs = elst;
    }

    @Override
    public void onDirectoryChange(File arg0)
    {

    }

    @Override
    public void onDirectoryCreate(File arg0)
    {

    }

    @Override
    public void onDirectoryDelete(File arg0)
    {
        try
        {
            BaseSqliteStore.getInstance().deleteRecordsInDirs(arg0.getCanonicalPath());
            setNeedRefreshAll(true);
        }
        catch (Exception e)
        {
            logger.warn("get the file's path failed: " + arg0, e);
        }
    }

    @Override
    public void onFileChange(File arg0)
    {
        try
        {
            if (!isCareFile(arg0))
            {
                return;
            }

            ToolMain.checkOneFile(arg0);
            setNeedRefreshAll(true);
        }
        catch (IOException e)
        {
            logger.warn("get the file's path failed: " + arg0, e);
        }
    }

    @Override
    public void onFileCreate(File arg0)
    {
        try
        {
            if (!isCareFile(arg0))
            {
                return;
            }

            ToolMain.checkOneFile(arg0);
            setNeedRefreshAll(true);
        }
        catch (IOException e)
        {
            logger.warn("get the file's path failed: " + arg0, e);
        }
    }

    @Override
    public void onFileDelete(File arg0)
    {
        try
        {
            if (!isCareFile(arg0))
            {
                return;
            }

            FileInfo fi = new FileInfo();
            fi.setPath(arg0.getCanonicalPath());
            BaseSqliteStore.getInstance().deleteOneRecord(fi);
            setNeedRefreshAll(true);
        }
        catch (IOException e)
        {
            logger.warn("get the file's path failed: " + arg0, e);
        }
    }

    @Override
    public void onStart(FileAlterationObserver arg0)
    {

    }

    @Override
    public void onStop(FileAlterationObserver arg0)
    {

    }

    private boolean isCareFile(File checkFile) throws IOException
    {
        if (checkFile == null)
        {
            return false;
        }
        String checkPath = checkFile.getCanonicalPath();

        if (excludeDirs != null && !excludeDirs.isEmpty())
        {
            if (checkPath != null)
            {
                for (String exc : excludeDirs)
                {
                    if (checkPath.startsWith(exc))
                    {
                        logger.info("the file is in exclude dirs: " + checkPath);
                        return false;
                    }
                }
            }
        }

        List<String> suffixes = AppConfig.getInstance().getFileSuffix();
        for (String suf : suffixes)
        {
            if (checkPath.toLowerCase().endsWith(suf.toLowerCase()))
            {
                if (checkFile.length() < AppConfig.getInstance().getMinFileSize())
                {
                    return false;
                }

                return true;
            }
        }

        logger.info("the file is not care: " + checkPath);
        return false;
    }

    public boolean isNeedRefreshAll()
    {
        return needRefreshAll;
    }

    public void setNeedRefreshAll(boolean needRefreshAll)
    {
        this.needRefreshAll = needRefreshAll;
    }

    public String getDirPath()
    {
        return dirPath;
    }

    public void setDirPath(String dirPath)
    {
        this.dirPath = dirPath;
    }
}

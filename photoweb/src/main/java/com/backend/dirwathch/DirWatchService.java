package com.backend.dirwathch;

import com.backend.dao.BaseSqliteStore;
import com.backend.scan.FileTools;
import com.backend.scan.ToolMain;
import com.utils.conf.AppConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DirWatchService
{
    private static final Logger logger = LoggerFactory.getLogger(DirWatchService.class);

    private ConcurrentHashMap<WatchKey, String> keyMap = new ConcurrentHashMap<>();
    private WatchService ws = null;
    private static DirWatchService instance = new DirWatchService();

    private static final int IS_RUNNING = 1;
    private static final int IS_STOPPING = 2;
    private static final int IS_STOPPED = 3;
    private volatile int status = IS_STOPPED;

    private DirWatchService()
    {
    }

    public static DirWatchService getInstance()
    {
        return instance;
    }

    public synchronized void restartScanAllFolders()
    {
        try
        {
            logger.warn("restaret a new watch filesystem service now: {}", status);
            switch (status)
            {
            case IS_RUNNING:
                logger.warn("stop the old watch filesystem service now: {}", status);
                status = IS_STOPPING;
                break;

            default:
                break;
            }

            if (ws != null)
            {
                ws.close();
            }

            while (status != IS_STOPPED)
            {
                delayLoop(1000);
            }

            logger.warn("the old watch filesystem service stoped: {}", status);

            status = IS_RUNNING;

            logger.warn("the new watch filesystem service started: {}", status);
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        watch();
                    }
                    catch (Exception e)
                    {
                        logger.warn("caught: ", e);
                    }
                }
            }.start();
        }
        catch (Throwable th)
        {
            logger.warn("caught: ", th);
            status = IS_STOPPED;
        }
    }

    private void mapDirs(File f, List<String> excludeDirs) throws IOException
    {
        if (f == null || !f.exists() || f.isFile())
        {
            return;
        }

        String filepath = f.getCanonicalPath();
        if (excludeDirs != null)
        {
            for (String exd : excludeDirs)
            {
                if (filepath.startsWith(exd))
                {
                    return;
                }
            }
        }

        addListener(filepath);

        File[] files = f.listFiles();
        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            try
            {
                mapDirs(file, excludeDirs);
            }
            catch (Throwable th)
            {
                logger.warn("some error occured when scan all the folders: {}", file);
                if (th instanceof ClosedWatchServiceException)
                {
                    throw th;
                }
            }
        }
    }

    private void addListener(String path) throws IOException
    {
        if (StringUtils.isBlank(path))
        {
            logger.warn("input dir is null.");
            return;
        }

        logger.debug("add one path to listen: " + path);

        Path p = Paths.get(path);
        if (ws != null && p.toFile().isDirectory())
        {
            WatchKey k = p.register(ws, StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            if (StringUtils.isNotBlank(path))
            {
                keyMap.put(k, path);
            }
        }
    }

    private void watch() throws IOException
    {
        try
        {
            keyMap.clear();
            ws = FileSystems.getDefault().newWatchService();

            List<String> inputDirs = AppConfig.getInstance().getInputDir();
            List<String> excludeDirs = AppConfig.getInstance().getExcludedir();
            if (inputDirs.isEmpty())
            {
                logger.warn("input dirs is empty!");
                return;
            }

            for (String ip : inputDirs)
            {
                mapDirs(new File(ip), excludeDirs);
            }

            while (status == IS_RUNNING)
            {
                WatchKey watchKey;
                try
                {
                    watchKey = ws.take();
                    String rootPath = keyMap.get(watchKey) + File.separator;
                    if (StringUtils.isBlank(rootPath))
                    {
                        continue;
                    }

                    List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                    for (WatchEvent<?> event : watchEvents)
                    {
                        final String fullPath = rootPath + event.context();
                        @SuppressWarnings("unchecked")
                        Kind<Path> kd = (Kind<Path>) event.kind();

                        if (kd.equals(StandardWatchEventKinds.ENTRY_CREATE))
                        {
                            final File cf = new File(fullPath);
                            logger.info("add one warn: " + cf);
                            if (cf.isDirectory())
                            {
                                this.addListener(fullPath);
                            }

                            FileTools.threadPool.submit(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        ToolMain.mapAllfiles(cf,
                                                AppConfig.getInstance().getExcludedir());
                                    }
                                    catch (Exception e)
                                    {
                                        logger.warn("map all files of: " + fullPath + " failed, ",
                                                e);
                                    }
                                }
                            });
                        }
                        else if (kd.equals(StandardWatchEventKinds.ENTRY_DELETE))
                        {
                            FileTools.threadPool.submit(new Runnable()
                            {

                                @Override
                                public void run()
                                {
                                    logger.info("try to delete the records in path: " + fullPath);
                                    BaseSqliteStore.getInstance().deleteRecordsInDirs(fullPath);
                                    logger.info("deleteed the records in path: " + fullPath);
                                }
                            });
                        }
                        else if (kd.equals(StandardWatchEventKinds.ENTRY_MODIFY))
                        {
                            final File cf = new File(fullPath);
                            // 只需要处理文件变化，文件夹变化不予处理。
                            if (cf.isFile())
                            {
                                logger.info("the entry is modified: " + cf);
                                FileTools.threadPool.submit(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        BaseSqliteStore.getInstance().checkIfAlreadyExist(cf);
                                    }
                                });
                            }
                        }
                        else
                        {
                            logger.info("unknown event: " + event);
                        }
                    }

                    boolean valid = watchKey.reset();
                    {
                        if (!valid)
                        {
                            logger.warn("the key is invalid: " + rootPath);
                            // 删除失效的KEY。
                            keyMap.remove(watchKey);
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    logger.warn("caused: ", e);
                }
                catch (Throwable th)
                {
                    logger.warn("throwable :", th);
                    delayLoop(3000);
                }

            }
        }
        catch (ClosedWatchServiceException e1)
        {
            // need restart.
            logger.warn("caused: ", e1);
        }
        finally
        {
            status = IS_STOPPED;
            logger.warn("stop the old watch service now: {}", status);
        }
    }

    private void delayLoop(long timemills)
    {
        if (timemills <= 0)
        {
            timemills = 3000;
        }

        try
        {
            Thread.sleep(timemills);
        }
        catch (InterruptedException e)
        {
            logger.warn("caused: ", e);
        }
    }

    public long getTheWatchDirCount()
    {
        return keyMap.size();
    }
}

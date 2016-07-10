package com.backend.dirwathch;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.BaseSqliteStore;
import com.backend.FileTools;
import com.backend.ToolMain;
import com.utils.conf.AppConfig;

public class DirWatchService
{
    private static final Logger logger = LoggerFactory.getLogger(DirWatchService.class);
    private ConcurrentHashMap<WatchKey, String> keyMap = new ConcurrentHashMap<WatchKey, String>();
    private WatchService ws = null;
    private static DirWatchService instance = new DirWatchService();

    private DirWatchService()
    {

    }

    public static DirWatchService getInstance()
    {
        return instance;
    }

    public void init(final List<String> inputDirs, final List<String> excludeDirs)
    {
        try
        {
            if (inputDirs.isEmpty())
            {
                logger.warn("input dirs is empty!");
                return;
            }

            ws = FileSystems.getDefault().newWatchService();
            new Thread()
            {
                public void run()
                {
                    for (String ip : inputDirs)
                    {
                        mapDirs(new File(ip), excludeDirs);
                    }
                    watch();
                }
            }.start();
        }
        catch (IOException e)
        {
            logger.warn("init the ws failed!", e);
        }
    }

    private void mapDirs(File f, List<String> excludeDirs)
    {
        if (f == null || !f.exists() || f.isFile())
        {
            return;
        }

        if (excludeDirs != null)
        {
            for (String exd : excludeDirs)
            {
                String filepath = null;
                try
                {
                    filepath = f.getCanonicalPath();
                    if (filepath.startsWith(exd))
                    {
                        return;
                    }
                }
                catch (IOException e)
                {
                    logger.warn("caught exception when add listener: " + f);
                    continue;
                }

                addListener(filepath);
            }
        }

        File[] files = f.listFiles();
        if (files == null)
        {
            return;
        }
        for (File file : files)
        {
            mapDirs(file, excludeDirs);
        }
    }

    public void addListener(String path)
    {
        if (StringUtils.isBlank(path))
        {
            logger.warn("input dir is null.");
            return;
        }

        Path p = Paths.get(path);
        if (ws != null && p.toFile().isDirectory())
        {
            try
            {
                WatchKey k = p.register(ws, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE);
                if (StringUtils.isNotBlank(path))
                {
                    keyMap.put(k, path);
                }
            }
            catch (Exception e)
            {
                logger.warn("add a dir to the listener failed: " + path, e);
            }
        }
    }

    private void watch()
    {
        while (true)
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
                                    ToolMain.mapAllfiles(cf, AppConfig.getInstance().getExcludedir());
                                }
                                catch (Exception e)
                                {
                                    logger.warn("map all files of: " + fullPath + " failed, ", e);
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
                                BaseSqliteStore.getInstance().deleteRecordsInDirs(fullPath);
                            }
                        });
                    }
                    else if (kd.equals(StandardWatchEventKinds.ENTRY_MODIFY))
                    {
                        final File cf = new File(fullPath);
                        if (cf.isFile())
                        {
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
                    }
                }
            }
            catch (InterruptedException e)
            {
                logger.warn("caused: ", e);
            }

        }
    }
}

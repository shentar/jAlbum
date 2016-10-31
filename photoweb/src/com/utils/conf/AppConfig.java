package com.utils.conf;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig
{
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    public static final String configFilePath = "jalbum.xml";

    private static XMLConfiguration config;

    private static final AppConfig instance = new AppConfig();

    private AppConfig()
    {

    }

    public static AppConfig getInstance()
    {
        return instance;
    }

    static
    {
        try
        {
            config = new XMLConfiguration(configFilePath);
            FileChangedReloadingStrategy strategy = new FileChangedReloadingStrategy();
            strategy.setRefreshDelay(5000);
            config.setReloadingStrategy(strategy);
        }
        catch (ConfigurationException e)
        {
            logger.warn("caused: ", e);
        }
    }

    public List<String> getInputDir()
    {
        List<String> dirlst = new LinkedList<String>();
        try
        {
            List<String> strList = new LinkedList<String>();
            List<Object> lst = config.getList("inputdir.dir");
            for (Object s : lst)
            {
                if (s instanceof String)
                {
                    strList.add((String) s);
                }
            }

            for (String s : strList)
            {
                File f = new File(s);
                if (f.exists() && f.isDirectory())
                {
                    dirlst.add(f.getCanonicalPath());
                }
            }
        }
        catch (Exception e)
        {
            logger.warn("exception: ", e);
        }

        return dirlst;
    }

    public List<String> getFileSuffix()
    {
        List<String> dirLst = new LinkedList<String>();
        List<Object> lst = config.getList("picfilesuffix.suffix");
        for (Object s : lst)
        {
            if (s instanceof String)
            {
                dirLst.add("." + (String) s);
            }
        }

        return dirLst;
    }

    public int getThreadCount()
    {
        return config.getInt("threadcount", 20);
    }

    public String getHashAlog()
    {
        return config.getString("hashalog", "MD5");
    }

    public long getMinFileSize()
    {
        return config.getLong("minfilesize", 1024 * 50);
    }

    public List<String> getExcludedir()
    {
        List<String> dirlst = new LinkedList<String>();

        try
        {
            List<String> strList = new LinkedList<String>();
            List<Object> lst = config.getList("excludedir.dir");
            for (Object s : lst)
            {
                if (s instanceof String)
                {
                    strList.add((String) s);
                }
            }

            for (String s : strList)
            {
                File f = new File(s);
                if (f.exists() && f.isDirectory())
                {
                    dirlst.add(f.getCanonicalPath());
                }
            }
        }
        catch (Exception e)
        {
            logger.warn("excption: ", e);
        }
        return dirlst;
    }

    public String getThumbnailDir()
    {
        return config.getString("thumbnaildir", "./thumbnail");
    }

    public int getMaxCountOfPicInOnePage(int defaultValue)
    {
        return config.getInt("maxpicsperonepage", defaultValue);
    }
}

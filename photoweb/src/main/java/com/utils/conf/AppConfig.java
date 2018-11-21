package com.utils.conf;

import com.utils.sys.SystemConstant;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class AppConfig
{
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private static final String configFilePath = "jalbum.xml";

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

    public String getVersion(String defaultver)
    {
        return config.getString("version", defaultver);
    }

    public List<String> getInputDir()
    {
        List<String> dirlst = new LinkedList<>();
        try
        {
            List<String> strList = new LinkedList<>();
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
        List<String> dirLst = new LinkedList<>();
        List<Object> lst = config.getList("picfilesuffix.suffix");
        for (Object s : lst)
        {
            if (s instanceof String)
            {
                dirLst.add("." + s);
            }
        }

        return dirLst;
    }

    public boolean isVideoConfigured()
    {
        List<String> slst = getFileSuffix();
        return slst != null && slst.contains(".mp4");
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

    public long getMaxFilesize()
    {
        return config.getLong("maxfilesize", 1024 * 1024 * 500);
    }

    public long getIdleRefreshInteval()
    {
        return config.getLong("idlerefreshinterval", 5 * 60 * 1000);
    }

    public String getGoogleAnalyticsID()
    {
        return config.getString("GoogleAnalyticsID");
    }

    public long getBusyRefreshInteval()
    {
        return config.getLong("busyrefreshinterval", 10 * 1000);
    }

    public List<String> getExcludedir()
    {
        List<String> dirlst = new LinkedList<>();

        try
        {
            List<String> strList = new LinkedList<>();
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

    public boolean needAccessAuth()
    {
        return config.getBoolean("accessAuth", true);
    }

    public int getMaxCountOfPicInOnePage(int defaultValue)
    {
        return config.getInt("maxpicsperonepage", defaultValue);
    }

    public boolean isAutoBackUp()
    {
        return config.getBoolean("autobackup", false);
    }

    // config for AWS S3.
    public String getS3AK()
    {
        return config.getString("s3.ak", "");
    }

    public String getS3SK()
    {
        return config.getString("s3.sk", "");
    }

    public boolean useS3HTTPS()
    {
        return config.getBoolean("s3.useHttps", true);
    }

    public String getS3BucketName()
    {
        return config.getString("s3.bucketname", "");
    }

    public int getS3ConcurrentThreads()
    {
        return config.getInt("s3.threadperbucket", 20);
    }

    public boolean isS3Configed()
    {
        return StringUtils.isNotBlank(AppConfig.getInstance().getS3AK()) && StringUtils
                .isNotBlank(AppConfig.getInstance().getS3SK()) && StringUtils
                .isNotBlank(AppConfig.getInstance().getS3BucketName());
    }

    public long getRetryInitS3()
    {
        return config.getLong("s3.retryinits3", 15000);
    }

    public boolean isS3ProxyConfiged()
    {
        return StringUtils.isNotBlank(getS3ProxyHost()) && getS3ProxyPort() != -1;

    }

    public String getS3ProxyHost()
    {
        String defaultHost = config.getString("Proxy.host", "");
        return config.getString("s3.Proxy.host", defaultHost);
    }

    public int getS3ProxyPort()
    {
        int defaultport = config.getInt("Proxy.port", -1);
        return config.getInt("s3.Proxy.port", defaultport);
    }

    public String getS3ProxyUser()
    {
        String defaultUser = config.getString("Proxy.user", "");
        return config.getString("s3.Proxy.user", defaultUser);
    }

    public String getS3ProxyPWD()
    {
        String defaultPass = config.getString("Proxy.password", "");
        return config.getString("s3.Proxy.password", defaultPass);
    }

    // config for Huawei Cloud OBS.
    public String getHuaweiOBSAK()
    {
        return config.getString("HuaweiOBS.ak", "");
    }

    public String getHuaweiOBSSK()
    {
        return config.getString("HuaweiOBS.sk", "");
    }


    public boolean useHuaweiOBSHTTPS()
    {
        return config.getBoolean("HuaweiOBS.useHttps", true);
    }

    public String getHuaweiOBSBucketName()
    {
        return config.getString("HuaweiOBS.bucketname", "");
    }

    public int getHuaweiOBSConcurrentThreads()
    {
        return config.getInt("HuaweiOBS.threadperbucket", 20);
    }

    public boolean isHuaweiOBSConfiged()
    {
        return StringUtils.isNotBlank(AppConfig.getInstance().getHuaweiOBSAK()) && StringUtils
                .isNotBlank(AppConfig.getInstance().getHuaweiOBSSK()) && StringUtils
                .isNotBlank(AppConfig.getInstance().getHuaweiOBSBucketName());
    }

    public boolean isHWProxyConfiged()
    {
        return StringUtils.isNotBlank(getHWProxyHost()) && getHWProxyPort() != -1;
    }

    public String getHWProxyHost()
    {
        String defaultHost = config.getString("Proxy.host", "");
        return config.getString("HuaweiOBS.Proxy.host", defaultHost);
    }

    public int getHWProxyPort()
    {
        int defaultport = config.getInt("Proxy.port", -1);
        return config.getInt("HuaweiOBS.Proxy.port", defaultport);
    }

    public String getHWProxyUser()
    {
        String defaultUser = config.getString("Proxy.user", "");
        return config.getString("HuaweiOBS.Proxy.user", defaultUser);
    }

    public String getHWOBSEndPoint()
    {
        return config.getString("HuaweiOBS.EndPoint", "obs.myhwclouds.com");
    }

    public String getHWProxyPWD()
    {
        String defaultPass = config.getString("Proxy.password", "");
        return config.getString("HuaweiOBS.Proxy.password", defaultPass);
    }

    // config for Face++
    public String getFacerProxyHost()
    {
        String defaultHost = config.getString("Proxy.host", "");
        return config.getString("Facer.Proxy.host", defaultHost);
    }

    public int getFacerProxyPort()
    {
        int defaultport = config.getInt("Proxy.port", -1);
        return config.getInt("Facer.Proxy.port", defaultport);
    }

    public String getFacerProxyUser()
    {
        String defaultUser = config.getString("Proxy.user", "");
        return config.getString("Facer.Proxy.user", defaultUser);
    }

    public String getFacerProxyPWD()
    {
        String defaultPass = config.getString("Proxy.password", "");
        return config.getString("Facer.Proxy.password", defaultPass);
    }

    public String getApiKey()
    {
        return config.getString("Facer.ak", "");
    }

    public String getSecret()
    {
        return config.getString("Facer.sk", "");
    }

    public boolean isFacerConfigured()
    {
        return StringUtils.isNotBlank(getApiKey()) && StringUtils.isNotBlank(getSecret());

    }

    public boolean facerUseHttps()
    {
        return config.getBoolean("Facer.useHttps", true);
    }

    public String getFaceSetPrefix()
    {
        return config.getString("Facer.facesetprefix", "jalbum_faceset_id_");
    }

    public int getMaxFacesCount()
    {
        return config.getInt("Facer.maxfacescount", 25);
    }

    public int getFacerConcurrentThreads()
    {
        return config.getInt("Facer.threadperbucket", 20);
    }

    public int getMaxExpireAge()
    {
        return config.getInt("expiretime", SystemConstant.MAX_EXPIRE_AGE);
    }
}

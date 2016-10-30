package com.utils.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.utils.conf.AppConfig;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;

public class HeadUtils
{
    private static final Logger logger = LoggerFactory.getLogger(HeadUtils.class);

    public static boolean isMobile()
    {
        Boolean ismobile = (Boolean) SystemProperties.get(SystemConstant.IS_MOBILE_KEY);
        return ismobile != null && ismobile.booleanValue();
    }

    public static boolean isIOS()
    {
        Boolean ismobile = (Boolean) SystemProperties.get(SystemConstant.IS_IOS);
        return ismobile != null && ismobile.booleanValue();
    }

    public static boolean needRotatePic(FileInfo f, int size)
    {
        if (size >= 400 && !isIOS() && f.getRoatateDegree() != 0)
        {
            return true;
        }

        return false;
    }

    public static void checkMobile(String ua)
    {
        boolean isMobile = true;
        boolean isIOS = false;
        if (StringUtils.isNotBlank(ua))
        {
            ua = ua.toLowerCase();
            if ((ua.contains("windows") && !ua.contains("windows phone"))
                    || ua.contains("macintosh") || (ua.contains("linux") && !ua.contains("android"))
                    || ua.contains("ipad"))
            {
                isMobile = false;
            }

            if (ua.contains("ios"))
            {
                isIOS = true;
            }
        }

        SystemProperties.add(SystemConstant.IS_MOBILE_KEY, new Boolean(isMobile));
        SystemProperties.add(SystemConstant.IS_IOS, new Boolean(isIOS));
    }

    public static void setExpiredTime(ResponseBuilder builder)
    {
        if (builder == null)
        {
            return;
        }

        long expirAge = 3600 * 1000 * 24 * 7;
        long expirtime = System.currentTimeMillis() + expirAge;
        builder.header("Expires", new Date(expirtime));
        builder.header("Cache-Control", "max-age=" + expirAge);
    }

    public static int getMaxCountOfOnePage()
    {
        if (isMobile())
        {
            return 9;
        }
        else
        {
            return AppConfig.getInstance().getMaxCountOfPicInOnePage(25);
        }
    }

    public static String judgeMIME(String filePath)
    {
        filePath = filePath.toLowerCase();
        String contentType;
        if (filePath.endsWith(".js"))
        {
            contentType = "application/javascript";
        }
        else if (filePath.endsWith(".css"))
        {
            contentType = "text/css";
        }
        else if (filePath.endsWith(".gif"))
        {
            contentType = "image/gif";
        }
        else if (filePath.endsWith(".mp4"))
        {
            contentType = "video/mp4";
        }
        else
        {
            contentType = "text/html";
        }
        return contentType;
    }

    public static String formatDate(java.sql.Date d)
    {
        if (d == null)
        {
            return null;
        }

        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        return sf.format(d);
    }

    public static Date parseDate(String dayStr)
    {
        if (StringUtils.isBlank(dayStr))
        {
            return null;
        }

        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        try
        {
            return sf.parse(dayStr);
        }
        catch (ParseException e)
        {
            logger.warn("caught: ", e);
        }

        return null;
    }

    public static boolean isVideo()
    {
        Boolean isvideo = (Boolean) SystemProperties.get(SystemConstant.IS_VIDEO);
        return isvideo != null && isvideo.booleanValue();
    }
}

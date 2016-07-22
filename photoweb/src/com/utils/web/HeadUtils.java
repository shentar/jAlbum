package com.utils.web;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;

public class HeadUtils
{
    public static boolean checkMobile(HttpServletRequest req)
    {
        String ua = req.getHeader("User-Agent");
        if (StringUtils.isNotBlank(ua))
        {
            ua = ua.toLowerCase();
            if ((ua.contains("windows") && !ua.contains("windows phone"))
                    || ua.contains("macintosh") || (ua.contains("linux") && ua.contains("android")))
            {
                return false;
            }
        }

        return true;
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
    
    public static String judgeMIME(String filePath)
    {
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
        else
        {
            contentType = "text/html";
        }
        return contentType;
    }
}

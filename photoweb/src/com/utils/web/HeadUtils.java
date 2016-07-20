package com.utils.web;

import javax.servlet.http.HttpServletRequest;

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
                    || ua.contains("macintosh")
                    || (ua.contains("linux") && ua.contains("android")))
            {
                return false;
            }
        }

        return true;
    }
}

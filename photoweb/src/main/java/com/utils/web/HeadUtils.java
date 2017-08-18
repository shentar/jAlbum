package com.utils.web;

import com.backend.FileInfo;
import com.backend.FileType;
import com.service.filter.LoginStatus;
import com.utils.conf.AppConfig;
import com.utils.media.MediaTool;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HeadUtils
{
    private static final Logger logger = LoggerFactory.getLogger(HeadUtils.class);

    public static boolean isMobile()
    {
        Boolean ismobile = (Boolean) SystemProperties.get(SystemConstant.IS_MOBILE_KEY);
        return ismobile != null && ismobile;
    }

    public static boolean isFaces()
    {
        Boolean isFaces = (Boolean) SystemProperties.get(SystemConstant.IS_FACES_KEY);
        return isFaces != null && isFaces;
    }

    public static boolean isNoFaces()
    {
        Boolean isFaces = (Boolean) SystemProperties.get(SystemConstant.IS_NO_FACES_KEY);
        return isFaces != null && isFaces;
    }

    public static boolean isIOS()
    {
        Boolean ismobile = (Boolean) SystemProperties.get(SystemConstant.IS_IOS);
        return ismobile != null && ismobile;
    }

    public static boolean needRotatePic(FileInfo f)
    {
        return !isIOS() && f.getRoatateDegree() != 0;

    }

    public static void checkMobile(String ua)
    {
        boolean isMobile = false;
        boolean isIOS = false;
        if (StringUtils.isNotBlank(ua))
        {
            ua = ua.toLowerCase();

            if (ua.contains("mobile") || ua.contains("android")
                    || (ua.contains("ios") && !ua.contains("ipad")) || ua.contains("windows phone"))
            {
                isMobile = true;
            }

            if (ua.contains("ios"))
            {
                isIOS = true;
            }
        }

        SystemProperties.add(SystemConstant.IS_MOBILE_KEY, isMobile);
        SystemProperties.add(SystemConstant.IS_IOS, isIOS);
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
        else if (filePath.endsWith(".mkv"))
        {
            contentType = "video/x-matroska";
        }
        else if (filePath.endsWith(".avi"))
        {
            contentType = "video/x-msvideo";
        }
        else if (filePath.endsWith(".rmvb"))
        {
            contentType = "video/vnd.rn-realvideo";
        }
        else
        {
            contentType = "text/html";
        }
        return contentType;
    }

    public static FileType getFileType(String filePath)
    {
        if (StringUtils.isBlank(filePath))
        {
            return FileType.JPG;
        }

        filePath = filePath.toLowerCase();
        FileType contentType;

        if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv") || filePath.endsWith(".avi")
                || filePath.endsWith(".rmvb"))
        {
            contentType = FileType.VIDEO;
        }
        else if (filePath.endsWith(".png"))
        {
            contentType = FileType.PNG;
        }
        else if (filePath.endsWith(".jpeg"))
        {
            contentType = FileType.JPEG;
        }
        else if (filePath.endsWith(".jpg"))
        {
            contentType = FileType.JPG;
        }
        else
        {
            contentType = FileType.JPG;
        }

        return contentType;
    }

    public static String getContentType(String pathToFile) throws IOException
    {
        String mime = Files.probeContentType(Paths.get(pathToFile));
        if (StringUtils.isBlank(mime))
        {
            mime = MediaTool.isVideo(pathToFile) ? "video/mp4" : "application/octet-stream";
        }

        return mime;
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
        return isvideo != null && isvideo;
    }

    public static int judgeCountPerOnePage(HttpServletRequest req)
    {
        int count = 0;
        int maxCount = AppConfig.getInstance().getMaxCountOfPicInOnePage(25);
        if (req == null)
        {
            return maxCount;
        }

        String countStr = req.getParameter("count");
        if (StringUtils.isNotBlank(countStr))
        {
            count = Integer.parseInt(countStr);
        }

        if (count == 0 || count > maxCount)
        {
            count = maxCount;
        }

        if (count > 9)
        {
            if (HeadUtils.isMobile())
            {
                count = 9;
            }
        }

        boolean isvideo = HeadUtils.isVideo();
        if (isvideo)
        {
            if (count > 6)
            {
                count = 6;
            }
        }

        return count;
    }

    public static boolean isSuperLogin()
    {
        Object o = SystemProperties.get(SystemConstant.USER_LOGIN_STATUS);
        if (o != null && o instanceof LoginStatus)
        {
            return o.equals(LoginStatus.SuperLogin);
        }

        return false;
    }

    public static boolean isLocalLogin()
    {
        Object o = SystemProperties.get(SystemConstant.USER_LOGIN_STATUS);
        if (o != null && o instanceof LoginStatus)
        {
            return o.equals(LoginStatus.LocalLoin);
        }

        return false;
    }
}

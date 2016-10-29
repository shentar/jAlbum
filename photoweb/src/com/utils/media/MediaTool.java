package com.utils.media;

import org.apache.commons.lang.StringUtils;

import com.backend.FileInfo;

public class MediaTool
{
    public static FileInfo genFileInfo(String filePath)
    {
        if (StringUtils.isBlank(filePath))
        {
            return null;
        }

        if (isVideo(filePath))
        {
            return VideoFFProbeTool.genFileInfos(filePath);
        }
        else
        {
            return ReadEXIF.genAllInfos(filePath, true);
        }
    }

    public static boolean isVideo(String filePath)
    {
        if (StringUtils.isBlank(filePath))
        {
            return false;
        }

        return filePath.toLowerCase().endsWith(".mp4");
    }
}

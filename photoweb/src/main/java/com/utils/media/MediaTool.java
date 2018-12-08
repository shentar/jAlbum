package com.utils.media;

import com.backend.entity.FileInfo;
import com.backend.entity.FileType;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;

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
        return FileType.VIDEO.equals(HeadUtils.getFileType(filePath));
    }
}

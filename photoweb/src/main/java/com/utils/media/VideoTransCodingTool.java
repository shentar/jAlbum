package com.utils.media;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoTransCodingTool
{
    private static final Logger logger = LoggerFactory.getLogger(VideoTransCodingTool.class);

    private static final List<String> SUPPORTED_SUFFIX =
            new ArrayList<>(Arrays.asList(".mov", ".avi", ".mkv"));

    private static final String DST_FORMAT = ".mp4";


    public static boolean needConvert(String fpath)
    {
        for (String suffix : SUPPORTED_SUFFIX)
        {
            if (StringUtils.endsWithIgnoreCase(fpath, suffix))
            {
                return true;
            }
        }

        return false;
    }

    public static String checkAndConvertToMP4(String fpath)
    {
        if (!needConvert(fpath))
        {
            return fpath;
        }

        try
        {
            String dstFpath = fpath + "_" + System.currentTimeMillis() + DST_FORMAT;
            String dstFpathTmp = dstFpath + ".tmp";
            File fdst = new File(dstFpath);
            File fdstTmp = new File(dstFpathTmp);
            if (fdst.exists() || fdstTmp.exists())
            {
                logger.warn("the file [{}] has already been converted to [{}].", fpath, dstFpath);
                return null;
            }

            FFmpeg fFmpeg = new FFmpeg();
            FFmpegBuilder fFmpegBuilder = fFmpeg.builder();
            fFmpegBuilder.addInput(fpath).addOutput(dstFpathTmp).setVideoCodec("copy")
                    .setAudioCodec("copy").setFormat("mp4")
                    .addExtraArgs("-c:a", "aac", "-map_metadata:s:a", "0:s:a", "-map_metadata",
                                  "0:g", "-map_metadata:s:v", "0:s:v").done();
            fFmpeg.run(fFmpegBuilder, null);
            logger.warn("convert the file [{}] to [{}] successfully.", fpath, dstFpathTmp);

            File bakfile = new File(fpath + "_" + System.currentTimeMillis() + ".bak");
            if (bakfile.exists())
            {
                logger.warn("the file is already exist: {}", bakfile.getCanonicalPath());
                return null;
            }

            File fin = new File(fpath);
            if (fdstTmp.renameTo(fdst) && fin.renameTo(bakfile))
            {
                return dstFpath;
            }

            logger.warn("rename the file [{}] to [{}] failed!", fpath, bakfile);
            return dstFpath;
        }
        catch (Throwable e)
        {
            logger.warn("some error, ", e);
        }

        return null;
    }
}

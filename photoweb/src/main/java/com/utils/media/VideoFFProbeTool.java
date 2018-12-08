package com.utils.media;

import com.backend.entity.FileInfo;
import com.backend.entity.FileType;
import com.backend.entity.PicStatus;
import com.backend.scan.FileTools;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.probe.FFmpegStream.CodecType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class VideoFFProbeTool
{
    private static final long INVALID_TIME_IN_MILLS = -1;

    private static final Logger logger = LoggerFactory.getLogger(VideoFFProbeTool.class);

    public static String getFFprobeInfo(String filePath)
    {
        if (StringUtils.isBlank(filePath))
        {
            return null;
        }

        try
        {
            String query = "ffprobe -v quiet -print_format json -show_format -show_streams \""
                    + filePath + "\"";
            String[] command = {"/bin/sh", "-c", query};
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            process.getErrorStream().close();
            process.getOutputStream().close();

            InputStream in = process.getInputStream();
            BufferedInputStream bin = new BufferedInputStream(in);

            int nlen = 1024 * 64;
            byte[] buffer = new byte[nlen];
            int rlen;
            int off = 0;
            StringBuilder sb = new StringBuilder();
            while (true)
            {
                rlen = bin.read(buffer, 0, 1024 * 64);
                if (rlen == -1)
                {
                    if (off != 0)
                    {
                        sb.append(new String(buffer, "UTF-8").substring(0, off));
                    }
                    break;
                }

                off += rlen;
                nlen -= rlen;
                if (nlen == 0)
                {
                    sb.append(new String(buffer, "UTF-8"));
                }
            }

            bin.close();

            return sb.toString();
        }
        catch (Exception e)
        {
            logger.warn("caused: ", e);
        }

        return null;
    }

    public static FFmpegFormat getFileFormat(String filePath)
    {
        if (StringUtils.isBlank(filePath))
        {
            return null;
        }

        try
        {
            FFprobe ffprobe = new FFprobe("ffprobe");
            FFmpegProbeResult probeResult = ffprobe.probe(filePath);
            return probeResult.getFormat();
        }
        catch (Exception e)
        {
            logger.warn("caused: ", e);
        }

        return null;
    }

    private static FFmpegStream getVideoStream(String filePath)
    {
        if (StringUtils.isBlank(filePath))
        {
            return null;
        }

        try
        {
            FFprobe ffprobe = new FFprobe("ffprobe");
            FFmpegProbeResult probeResult = ffprobe.probe(filePath);

            if (probeResult != null && probeResult.getStreams() != null)
            {
                for (FFmpegStream fs : probeResult.getStreams())
                {
                    if (fs.codec_type.equals(CodecType.VIDEO))
                    {
                        return fs;
                    }
                }
            }

        }
        catch (Throwable e)
        {
            logger.warn("caused: ", e);
        }

        return null;
    }

    private static long getVideoCreateTime(FFmpegStream fs)
    {
        if (fs != null && fs.tags != null)
        {
            Object ti = fs.tags.get("creation_time");
            if (ti != null)
            {
                String dateStr = ti.toString();
                if (StringUtils.isBlank(dateStr))
                {
                    return INVALID_TIME_IN_MILLS;
                }

                SimpleDateFormat sf;
                if (dateStr.contains("T"))
                {
                    sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
                }
                else
                {
                    sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                }

                try
                {
                    return sf.parse(dateStr).getTime();
                }
                catch (ParseException e)
                {
                    logger.warn("caused: ", e);
                }
            }
        }

        return INVALID_TIME_IN_MILLS;
    }

    private static int getVideoRotate(FFmpegStream fs)
    {
        if (fs != null && fs.tags != null)
        {
            Object ti = fs.tags.get("rotate");
            if (ti != null)
            {
                try
                {
                    return Integer.parseInt(ti.toString());
                }
                catch (Exception e)
                {
                    logger.warn("caused: ", e);
                }
            }
        }

        return 0;
    }

    private static int getWidth(FFmpegStream fs)
    {
        return fs.width;
    }

    private static int getHeight(FFmpegStream fs)
    {
        return fs.height;
    }

    public static FileInfo genFileInfos(String fpath)
    {
        try
        {
            FFmpegStream fs = getVideoStream(fpath);
            if (fs != null)
            {
                File f = new File(fpath);

                FileInfo fi = new FileInfo();
                fi.setcTime(new Date(FileTools.getFileCreateTime(f)));
                fi.setStatus(PicStatus.EXIST);
                fi.setHeight(getHeight(fs));
                fi.setWidth(getWidth(fs));
                fi.setPath(fpath);
                fi.setSize(new File(fpath).length());
                fi.setFtype(FileType.VIDEO);

                long ptime = getVideoCreateTime(fs);
                fi.setPhotoTime(ptime == INVALID_TIME_IN_MILLS ? fi.getcTime() : new Date(ptime));

                fi.setRoatateDegree(getVideoRotate(fs));
                fi.setExtrInfo(generateFingerStringForVideo(fs));
                return fi;
            }
        }
        catch (Exception e)
        {
            logger.warn("Caused by: ", e);
        }

        return null;
    }

    private static String generateFingerStringForVideo(FFmpegStream fs)
    {
        return String.format("[%s,%s,%s,%s,%s,%s,%s,%s,%s,%s]", fs.avg_frame_rate.toString(),
                             fs.bit_rate + "", fs.bits_per_raw_sample + "", fs.codec_name + "", fs.duration + "",
                             fs.duration_ts + "", fs.display_aspect_ratio + "", fs.max_bit_rate + "",
                             fs.width + "", fs.height + "");
    }

}

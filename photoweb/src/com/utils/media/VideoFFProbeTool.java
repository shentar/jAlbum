package com.utils.media;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.backend.FileType;
import com.backend.scan.FileTools;

import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.probe.FFmpegStream.CodecType;

public class VideoFFProbeTool
{
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
            String[] command = { "/bin/sh", "-c", query };
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            process.getErrorStream().close();
            process.getOutputStream().close();

            InputStream in = process.getInputStream();
            BufferedInputStream bin = new BufferedInputStream(in);

            int nlen = 1024 * 64;
            byte[] buffer = new byte[nlen];
            int rlen = 0;
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

    public static FFmpegStream getVideoStream(String filePath)
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

    public static String getVideoCreateTime(FFmpegStream fs)
    {
        if (fs != null)
        {
            Object ti = fs.tags.get("creation_time");
            if (ti != null)
            {
                // logger.warn("class type is: " + ti.getClass());
                return ti.toString();
            }
        }

        return null;
    }

    public static double getDuration(FFmpegFormat fmt)
    {
        return fmt.duration;
    }

    public static long getBitrate(FFmpegFormat fmt)
    {
        return fmt.bit_rate;
    }

    public static long getSize(FFmpegFormat fmt)
    {
        return fmt.size;
    }

    public static int getWidth(FFmpegStream fs)
    {
        return fs.width;
    }

    public static int getHeight(FFmpegStream fs)
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
                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                FileInfo fi = new FileInfo();
                fi.setcTime(new Date(FileTools.getFileCreateTime(f)));
                fi.setDel(false);
                fi.setHeight(getHeight(fs));
                fi.setWidth(getWidth(fs));
                fi.setPath(fpath);
                fi.setSize(new File(fpath).length());
                fi.setFtype(FileType.MP4);
                String ptime = getVideoCreateTime(fs);
                if (StringUtils.isNotBlank(ptime))
                {
                    fi.setPhotoTime(new Date(sf.parse(ptime).getTime()));
                }
                else
                {
                    fi.setPhotoTime(fi.getcTime());
                }

                fi.setRoatateDegree(0);
                String extrInfo = String.format("[%s,%s,%s,%s,%s,%s,%s,%s,%s,%s]",
                        fs.avg_frame_rate.toString(), fs.bit_rate + "", fs.bits_per_raw_sample + "",
                        fs.codec_name + "", fs.duration + "", fs.duration_ts + "",
                        fs.display_aspect_ratio + "", fs.max_bit_rate + "", fs.width + "",
                        fs.height + "");
                fi.setExtrInfo(extrInfo);
                return fi;
            }
        }
        catch (Exception e)
        {
            logger.warn("Caused by: ", e);
        }

        return null;
    }
}

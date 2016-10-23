package com.utils.web;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoFFProbeTool
{
    private static final Logger logger = LoggerFactory.getLogger(VideoFFProbeTool.class);

    public static String getFFprobeInfo(String filePath)
    {
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
}

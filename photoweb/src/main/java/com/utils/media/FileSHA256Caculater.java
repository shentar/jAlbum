package com.utils.media;

import com.utils.conf.AppConfig;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileSHA256Caculater
{
    public static final int BUFFERED_SIZE = 1024 * 128;

    public static String calFileSha256(File f) throws NoSuchAlgorithmException, IOException
    {
        if (f == null || !f.exists() || f.isDirectory())
        {
            return null;
        }

        MessageDigest md = MessageDigest.getInstance(AppConfig.getInstance().getHashAlog());
        InputStream in = null;
        try
        {
            in = new BufferedInputStream(new FileInputStream(f));

            byte buffer[] = new byte[BUFFERED_SIZE];
            while (true)
            {
                int offset = 0;
                boolean bcomplete = false;
                while (offset < BUFFERED_SIZE)
                {
                    int readlen = in.read(buffer, offset, BUFFERED_SIZE - offset);
                    if (readlen < 0)
                    {
                        bcomplete = true;
                        break;
                    }
                    else
                    {
                        offset += readlen;
                    }
                }

                md.update(buffer, 0, offset);

                if (bcomplete)
                {
                    break;
                }
            }

            return byte2hex(md.digest());
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
        }
    }

    public static String calFileSha256(String str) throws NoSuchAlgorithmException, IOException
    {
        if (StringUtils.isBlank(str))
        {
            return null;
        }

        MessageDigest md = MessageDigest.getInstance(AppConfig.getInstance().getHashAlog());
        byte[] buf = str.getBytes("UTF-8");
        md.update(buf, 0, buf.length);
        return byte2hex(md.digest());
    }

    private static String byte2hex(byte[] bytes)
    {
        if (bytes == null || bytes.length == 0)
        {
            return null;
        }

        StringBuilder hs = new StringBuilder();
        String stmp;
        for (byte b : bytes)
        {
            stmp = (Integer.toHexString(b & 0XFF));
            if (stmp.length() == 1)
            {
                hs.append("0").append(stmp);
            }
            else
            {
                hs.append(stmp);
            }
        }
        return hs.toString().toUpperCase();
    }
}

package com.utils.media;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang.StringUtils;

import com.utils.conf.AppConfig;

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

    private static String byte2hex(byte[] b)
    {
        if (b == null || b.length == 0)
        {
            return null;
        }

        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++)
        {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1)
            {
                hs = hs + "0" + stmp;
            }
            else
            {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }
}

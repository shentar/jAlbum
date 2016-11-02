package com.service.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangesFileInputStream extends InputStream
{
    private static final Logger logger = LoggerFactory.getLogger(RangesFileInputStream.class);
    private static final long MAX_DEFAULT_RANGELEN = 1024 * 1024;
    private RandomAccessFile raf;
    private long end;
    private boolean erroroccured = false;
    private long pos = 0;

    public RangesFileInputStream(File in, long start, long end)
    {
        if (start == -1 && end == -1)
        {
            erroroccured = true;
        }

        if (start == -1)
        {
            start = 0;
        }

        if (end == -1)
        {
            if (in != null)
            {
                end = (in.length() > (start + MAX_DEFAULT_RANGELEN) ? (start + MAX_DEFAULT_RANGELEN)
                        : in.length());
                --end;
            }
        }

        if (end <= start || start < 0 || end >= in.length())
        {
            erroroccured = true;
        }
        else
        {
            this.pos = start;
            this.end = end;
            try
            {
                raf = new RandomAccessFile(in, "r");
                raf.seek(start);
            }
            catch (Exception e)
            {
                erroroccured = true;
                logger.warn("caused: ", e);
            }
        }
    }

    @Override
    public int read() throws IOException
    {
        if (pos >= end)
        {
            return -1;
        }
        pos++;
        return raf.read();
    }

    public int read(byte[] buf, int off, int len) throws IOException
    {
        if (erroroccured || buf == null || off < 0 || len < 0 || off + len > buf.length)
        {
            throw new IOException("error in put buffer!");
        }

        if (pos >= end)
        {
            raf.close();
            return -1;
        }

        int readLen = raf.read(buf, off, len);
        if (readLen == -1)
        {
            raf.close();
            return -1;
        }

        pos += readLen;

        return readLen;
    }

    public int read(byte[] buf) throws IOException
    {
        if (erroroccured || buf == null)
        {
            throw new IOException("error in put buffer!");
        }
        return read(buf, 0, buf.length);
    }

    public long getEnd()
    {
        return end;
    }

    public void setEnd(long end)
    {
        this.end = end;
    }

    public long getPos()
    {
        return pos;
    }

    public void setPos(long pos)
    {
        this.pos = pos;
    }
}

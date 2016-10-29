package com.backend;

import java.sql.Date;

public class VideoFile
{
    private String path;
    private long size;
    private double duration;
    private long bitrate;
    private Date ctime;
    private String digest;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public double getDuration()
    {
        return duration;
    }

    public void setDuration(double duration)
    {
        this.duration = duration;
    }

    public long getBitrate()
    {
        return bitrate;
    }

    public void setBitrate(long bitrate)
    {
        this.bitrate = bitrate;
    }

    public Date getCtime()
    {
        return ctime;
    }

    public void setCtime(Date ctime)
    {
        this.ctime = ctime;
    }

    public String getDigest()
    {
        return digest;
    }

    public void setDigest(String digest)
    {
        this.digest = digest;
    }
}

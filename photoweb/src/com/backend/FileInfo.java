package com.backend;

import java.sql.Date;

public class FileInfo
{
    private String path;

    private String hash256;

    private Date photoTime;

    private Date cTime;

    private long width = 0;

    private long height = 0;

    private long size;
    
    private boolean isDel = false;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getHash256()
    {
        return hash256;
    }

    public void setHash256(String hash256)
    {
        this.hash256 = hash256;
    }

    public Date getPhotoTime()
    {
        return photoTime;
    }

    public void setPhotoTime(Date photoTime)
    {
        this.photoTime = photoTime;
    }

    public String toString()
    {
        return "[path: " + path + "; hahstr: " + hash256 + "; phototime: "
                + photoTime + "]";
    }

    public long getHeight()
    {
        return height;
    }

    public void setHeight(long height)
    {
        this.height = height;
    }

    public long getWidth()
    {
        return width;
    }

    public void setWidth(long width)
    {
        this.width = width;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public Date getcTime()
    {
        return cTime;
    }

    public void setcTime(Date cTime)
    {
        this.cTime = cTime;
    }

    public boolean isDel()
    {
        return isDel;
    }

    public void setDel(boolean isDel)
    {
        this.isDel = isDel;
    }
}

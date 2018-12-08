package com.backend.entity;

import java.io.File;
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

    private PicStatus status = PicStatus.ERRORFILE;

    private int roatateDegree = 0;

    private String extrInfo;

    private FileType ftype;

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

    public String getHashStr()
    {
        return String.format(
                "[ptime: %s, hashstr: %s, x/y: %s/%s, size: %s, roatate: %s, extrInfo: %s]",
                photoTime + "", hash256, width + "", height + "", size + "", roatateDegree + "",
                extrInfo);
    }

    public String toString()
    {
        return String.format(
                "[path: %s, ptime: %s, ctime: %s, hashstr: %s, x/y: %s/%s, size: %s, roatate: %s, isDel: %s, filetype: %s]",
                path, photoTime + "", cTime + "", hash256, width + "", height + "", size + "",
                roatateDegree + "", status + "", ftype + "");
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

    public PicStatus getStatus()
    {
        return status;
    }

    public void setStatus(PicStatus status)
    {
        this.status = status;
    }

    public int getRoatateDegree()
    {
        return roatateDegree;
    }

    public void setRoatateDegree(int roatateDegree)
    {
        this.roatateDegree = roatateDegree;
    }

    public String getExtrInfo()
    {
        return extrInfo;
    }

    public void setExtrInfo(String extrInfo)
    {
        this.extrInfo = extrInfo;
    }

    public FileType getFtype()
    {
        return ftype;
    }

    public void setFtype(FileType ftype)
    {
        this.ftype = ftype;
    }

    public boolean exist()
    {
        return new File(this.getPath()).isFile();
    }
}

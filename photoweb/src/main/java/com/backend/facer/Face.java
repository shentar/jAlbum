package com.backend.facer;

import com.backend.entity.FileInfo;

public class Face
{
    private String etag;

    private String facetoken;

    private long faceid;

    private String pos;

    private String quality;

    private String gender;

    private String age;

    private long ptime;

    private FileInfo fi;

    public Face()
    {
        faceid = -1;
        quality = "0";
        facetoken = "null";
        gender = "null";
        age = "null";
        pos = "null";
    }

    public String getEtag()
    {
        return etag;
    }

    public void setEtag(String etag)
    {
        this.etag = etag;
    }

    public String getFacetoken()
    {
        return facetoken;
    }

    public void setFacetoken(String facetoken)
    {
        this.facetoken = facetoken;
    }

    public long getFaceid()
    {
        return faceid;
    }

    public void setFaceid(long faceid)
    {
        this.faceid = faceid;
    }

    public String getPos()
    {
        return pos;
    }

    public void setPos(String pos)
    {
        this.pos = pos;
    }

    public String getQuality()
    {
        return quality;
    }

    public void setQuality(String quality)
    {
        this.quality = quality;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getAge()
    {
        return age;
    }

    public void setAge(String age)
    {
        this.age = age;
    }

    public FileInfo getFi()
    {
        return fi;
    }

    public void setFi(FileInfo fi)
    {
        this.fi = fi;
    }

    @Override
    public String toString()
    {
        return "Face [etag=" + etag + ", facetoken=" + facetoken + ", faceid=" + faceid + ", pos="
                + pos + ", quality=" + quality + ", gender=" + gender + ", age=" + age + ", fi="
                + fi + "]";
    }

    public long getPtime()
    {
        return ptime;
    }

    public void setPtime(long ptime)
    {
        this.ptime = ptime;
    }
}

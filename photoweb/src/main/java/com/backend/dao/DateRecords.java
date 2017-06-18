package com.backend.dao;

public class DateRecords
{
    private String datetype = null;
    private String datestr = null;
    private long piccount = 0;
    private String firstpic = null;

    public String getDatetype()
    {
        return datetype;
    }

    public void setDatetype(String datetype)
    {
        this.datetype = datetype;
    }

    public String getDatestr()
    {
        return datestr;
    }

    public void setDatestr(String datestr)
    {
        this.datestr = datestr;
    }

    public long getPiccount()
    {
        return piccount;
    }

    public void setPiccount(long piccount)
    {
        this.piccount = piccount;
    }

    public String getFirstpic()
    {
        return firstpic;
    }

    public void setFirstpic(String firstpic)
    {
        this.firstpic = firstpic;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((datestr == null) ? 0 : datestr.hashCode());
        result = prime * result + ((datetype == null) ? 0 : datetype.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DateRecords other = (DateRecords) obj;
        if (datestr == null)
        {
            if (other.datestr != null)
                return false;
        }
        else if (!datestr.equals(other.datestr))
            return false;
        if (datetype == null)
        {
            if (other.datetype != null)
                return false;
        }
        else if (!datetype.equals(other.datetype))
            return false;
        return true;
    }

}

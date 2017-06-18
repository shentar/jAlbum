package com.utils.web;

public class Range
{
    private long start;
    private long end;

    public long getStart()
    {
        return start;
    }

    public void setStart(long l)
    {
        this.start = l;
    }

    public long getEnd()
    {
        return end;
    }

    public void setEnd(long end)
    {
        this.end = end;
    }

    @Override
    public String toString()
    {
        return "Range [start=" + start + ", end=" + end + "]";
    }
}

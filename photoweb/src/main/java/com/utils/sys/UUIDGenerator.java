package com.utils.sys;

import java.util.UUID;

public class UUIDGenerator
{
    public static String getUUID()
    {
        String uuid = UUID.randomUUID().toString();
        String[] splits = uuid.split("-");
        StringBuilder retBuilder = new StringBuilder();
        for (String s : splits)
        {
            retBuilder.append(s);
        }
        String ret = retBuilder.toString();

        if (ret.isEmpty())
        {
            return uuid.toUpperCase();
        }
        else
        {
            return ret.toUpperCase();
        }
    }
}

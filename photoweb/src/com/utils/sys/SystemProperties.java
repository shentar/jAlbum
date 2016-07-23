package com.utils.sys;

import java.util.HashMap;

public final class SystemProperties
{
    private static ThreadLocal<HashMap<String, Object>> contentLocal;

    static
    {
        init();
    }

    public static void add(String key, Object value)
    {
        contentLocal.get().put(key, value);
    }

    public static Object get(String key)
    {
        return contentLocal.get().get(key);
    }

    public static void init()
    {
        contentLocal = new ThreadLocal<HashMap<String, Object>>()
        {
            public HashMap<String, Object> initialValue()
            {
                return new HashMap<String, Object>();
            }
        };
    }

    private SystemProperties()
    {
    }
}

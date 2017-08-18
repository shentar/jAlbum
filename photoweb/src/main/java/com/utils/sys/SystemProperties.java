package com.utils.sys;

import java.util.HashMap;

public final class SystemProperties
{
    private ThreadLocal<HashMap<String, Object>> contentLocal;

    private static SystemProperties instance = new SystemProperties();

    static
    {
        instance.init();
    }

    public static void add(String key, Object value)
    {
        instance.contentLocal.get().put(key, value);
    }

    public static Object get(String key)
    {
        return instance.contentLocal.get().get(key);
    }

    private void init()
    {
        contentLocal = new ThreadLocal<HashMap<String, Object>>()
        {
            public HashMap<String, Object> initialValue()
            {
                return new HashMap<>();
            }
        };
    }

    public static void clear()
    {
        instance.contentLocal.get().clear();
    }

    private SystemProperties()
    {
    }
}

package com.utils.sys;

import java.util.HashMap;
import java.util.Map;

public class GlobalLockBaseOnString
{
    public static final String PIC_THUMBNAIL_LOCK = "PIC_THUMBNAIL_LOCK";

    public static final String FACE_THUMBNAIL_LOCK = "FACE_THUMBNAIL_LOCK";

    public static final String FACE_DETECT_LOCK = "FACE_DETECT_LOCK";

    private final Object lock = new Object();

    private HashMap<String, Object> lmap = new HashMap<>();

    private static Map<String, GlobalLockBaseOnString> instances = new HashMap<>();

    private GlobalLockBaseOnString()
    {

    }

    static
    {
        instances.put(PIC_THUMBNAIL_LOCK, new GlobalLockBaseOnString());
        instances.put(FACE_THUMBNAIL_LOCK, new GlobalLockBaseOnString());
        instances.put(FACE_DETECT_LOCK, new GlobalLockBaseOnString());

    }

    public static GlobalLockBaseOnString getInstance(String lockType)
    {
        return instances.get(lockType);
    }

    public boolean tryToDo(String id)
    {
        synchronized (lock)
        {
            if (lmap.containsKey(id))
            {
                return false;
            }
            else
            {
                lmap.put(id, new Object());
                return true;
            }

        }
    }

    public void done(String id)
    {
        synchronized (lock)
        {
            lmap.remove(id);
        }
    }
}

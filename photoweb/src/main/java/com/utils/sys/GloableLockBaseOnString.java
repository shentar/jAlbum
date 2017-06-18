package com.utils.sys;

import java.util.HashMap;
import java.util.Map;

public class GloableLockBaseOnString
{
    public static final String PIC_THUMBNAIL_LOCK = "PIC_THUMBNAIL_LOCK";

    public static final String FACE_THUMBNAIL_LOCK = "FACE_THUMBNAIL_LOCK";

    public static final String FACE_DETECT_LOCK = "FACE_DETECT_LOCK";

    private HashMap<String, Object> lmap = new HashMap<String, Object>();

    private Object lock = new Object();

    private static Map<String, GloableLockBaseOnString> instances = new HashMap<String, GloableLockBaseOnString>();

    private GloableLockBaseOnString()
    {

    }

    static
    {
        instances.put(PIC_THUMBNAIL_LOCK, new GloableLockBaseOnString());
        instances.put(FACE_THUMBNAIL_LOCK, new GloableLockBaseOnString());
        instances.put(FACE_DETECT_LOCK, new GloableLockBaseOnString());

    }

    public static GloableLockBaseOnString getInstance(String lockType)
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

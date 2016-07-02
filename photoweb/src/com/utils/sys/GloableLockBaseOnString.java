package com.utils.sys;

import java.util.HashMap;

public class GloableLockBaseOnString
{
    private HashMap<String, Object> lmap = new HashMap<String, Object>();
    
    public static GloableLockBaseOnString instance = new GloableLockBaseOnString();
    
    private GloableLockBaseOnString()
    {
        
    }
    
    public static GloableLockBaseOnString getInstance()
    {
        return instance;
    }
    
    public synchronized boolean tryToDo(String id)
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
    
    public synchronized void done(String id)
    {
        lmap.remove(id);
    }
}

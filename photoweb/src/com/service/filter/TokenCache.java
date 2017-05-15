package com.service.filter;

import java.util.concurrent.CopyOnWriteArraySet;

public class TokenCache
{
    private CopyOnWriteArraySet<String> tokenset = new CopyOnWriteArraySet<String>();

    private static TokenCache instance = new TokenCache();

    private TokenCache()
    {
    }

    public static TokenCache getInstance()
    {
        return instance;
    }

    public boolean contains(String token)
    {
        return tokenset.contains(token);
    }

    public void addOneToken(String token)
    {
        tokenset.add(token);
    }

    public void removeToken(String token)
    {
        tokenset.remove(token);
    }

    public void clear()
    {
        tokenset.clear();
    }
}

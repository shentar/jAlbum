package com.service.filter;

import com.backend.dao.GlobalConfDao;
import com.utils.sys.SystemConstant;
import org.apache.commons.lang.StringUtils;

import java.util.concurrent.CopyOnWriteArraySet;

public class TokenCache
{
    private CopyOnWriteArraySet<String> tokenset = new CopyOnWriteArraySet<>();

    private String superToken = null;

    private static TokenCache instance = new TokenCache();

    private boolean isInit = false;

    private TokenCache()
    {
    }

    public static TokenCache getInstance()
    {
        instance.init();
        return instance;
    }

    private synchronized void init()
    {
        if (isInit)
        {
            return;
        }

        refreshToken();

        isInit = true;
    }

    public synchronized void refreshToken()
    {
        tokenset.clear();
        for (int i = 0; i != 5; i++)
        {
            String userToken = GlobalConfDao.getInstance()
                    .getConf(GlobalConfDao.getInstance().getOneUserKey(i));
            if (StringUtils.isNotBlank(userToken))
            {
                tokenset.add(userToken);
            }
        }

        superToken = GlobalConfDao.getInstance().getConf(SystemConstant.SUPER_TOKEN_KEY);
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
        superToken = null;
    }

    public boolean isSupper(String token)
    {
        return StringUtils.isNotBlank(token) && StringUtils.equals(token, superToken);
    }
}

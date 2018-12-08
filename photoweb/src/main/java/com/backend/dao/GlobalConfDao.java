package com.backend.dao;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GlobalConfDao extends AbstractRecordsStore
{
    private static final Logger logger = LoggerFactory.getLogger(GlobalConfDao.class);

    private static GlobalConfDao instance = new GlobalConfDao();

    private GlobalConfDao()
    {

    }

    public static GlobalConfDao getInstance()
    {
        return instance;
    }

    public String getConf(String key)
    {
        if (StringUtils.isBlank(key))
        {
            return null;
        }
        PreparedStatement prep = null;
        ResultSet res = null;
        try
        {
            lock.readLock().lock();
            prep = conn.prepareStatement("select value from globalconf where key=?;");
            prep.setString(1, key);

            res = prep.executeQuery();
            String value = null;
            if (res.next())
            {
                value = res.getString(1);
            }

            prep.close();
            res.close();

            return value;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }

        return null;
    }

    public void setConf(String key, String value)
    {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value))
        {
            return;
        }
        boolean isupdate = false;

        String oldvalue = this.getConf(key);
        if (!StringUtils.isBlank(oldvalue))
        {
            logger.warn("the conf [{}] changes from {} to {}.",
                        key, oldvalue, value);
            isupdate = true;
        }
        else
        {
            logger.warn("add the conf: [{}:{}]", key, value);
        }

        PreparedStatement prep = null;
        try
        {
            lock.writeLock().lock();

            if (isupdate)
            {
                prep = conn.prepareStatement("update globalconf set value=? where key=?;");
                prep.setString(1, value);
                prep.setString(2, key);

            }
            else
            {
                prep = conn.prepareStatement("insert into globalconf values(?,?);");
                prep.setString(1, key);
                prep.setString(2, value);

            }

            prep.execute();
            prep.close();
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    public void delete(String key)
    {
        PreparedStatement prep = null;
        try
        {
            lock.writeLock().lock();
            prep = conn.prepareStatement("delete from globalconf where key=?;");
            prep.setString(1, key);

            prep.execute();
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    public String getOneUserToken(int i)
    {
        return getConf(getOneUserKey(i));
    }

    public String getOneUserKey(int i)
    {
        return "usertoken" + i;
    }
}

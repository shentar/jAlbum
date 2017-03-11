package com.backend.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        try
        {
            lock.readLock().lock();
            PreparedStatement prep = conn
                    .prepareStatement("select value from globalconf where key=?;");
            prep.setString(1, key);

            ResultSet res = prep.executeQuery();
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
                    new Object[] { key, oldvalue, value });
            isupdate = true;
        }
        else
        {
            logger.warn("add the conf: [{}:{}]", key, value);
        }

        try
        {
            lock.writeLock().lock();
            PreparedStatement prep = null;
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
            lock.writeLock().unlock();
        }
    }

    public void delete(String key)
    {
        try
        {
            lock.writeLock().lock();
            PreparedStatement prep = conn.prepareStatement("delete from globalconf where key=?;");
            prep.setString(1, key);

            prep.execute();
            prep.close();
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
}

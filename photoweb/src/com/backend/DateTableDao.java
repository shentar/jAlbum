package com.backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateTableDao
{
    private static final Logger logger = LoggerFactory.getLogger(DateTableDao.class);

    private Connection conn = SqliteConnManger.getInstance().getConn();

    private ReadWriteLock lock = new ReentrantReadWriteLock(false);

    private static DateTableDao instance = new DateTableDao();

    private DateTableDao()
    {

    }

    public static DateTableDao getInstance()
    {
        return instance;
    }

    public Map<String, Map<String, Map<String, DateRecords>>> getAllDateRecord()
    {
        Map<String, Map<String, Map<String, DateRecords>>> allrecords = new HashMap<String, Map<String, Map<String, DateRecords>>>();
        lock.readLock().lock();
        PreparedStatement prep = null;
        ResultSet res = null;
        try
        {
            prep = conn.prepareStatement("select * from daterecords;");
            res = prep.executeQuery();

            while (res.next())
            {
                String date = res.getString("datestr");
                if (StringUtils.isBlank(date) || date.length() != 8)
                {
                    logger.error("one empty datestr record!");
                    continue;
                }

                String day = date.substring(6, 8);
                String month = date.substring(4, 6);
                String year = date.substring(0, 4);

                Map<String, Map<String, DateRecords>> myear = allrecords.get(year);
                if (myear == null)
                {
                    myear = new HashMap<String, Map<String, DateRecords>>();
                    allrecords.put(year, myear);
                }

                Map<String, DateRecords> mmonth = myear.get(month);
                if (mmonth == null)
                {
                    mmonth = new HashMap<String, DateRecords>();
                    myear.put(month, mmonth);
                }

                DateRecords dr = new DateRecords();
                dr.setDatestr(date);
                dr.setFirstpic(res.getString("firstpichashstr"));
                dr.setPiccount(res.getLong("piccoount"));
                mmonth.put(day, dr);
            }

            prep.close();
            res.close();
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            lock.readLock().unlock();
        }

        return allrecords;
    }

    public DateRecords getOneRecordsByDay(String day)
    {
        if (StringUtils.isBlank(day) || day.length() != 8)
        {
            return null;
        }

        lock.readLock().lock();
        PreparedStatement prep = null;
        ResultSet res = null;
        try
        {
            prep = conn.prepareStatement("select * from daterecords where datestr=?;");
            prep.setString(1, day);
            res = prep.executeQuery();

            if (res.next())
            {
                DateRecords dr = new DateRecords();
                dr.setDatestr(day);
                dr.setFirstpic(res.getString("firstpichashstr"));
                dr.setPiccount(res.getLong("piccoount"));
                return dr;
            }

            prep.close();
            res.close();
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

    public void refreshDate()
    {
        logger.warn("refresh all date record.");

        Map<String, DateRecords> dst = UniqPhotosStore.getInstance().genAllDateRecords();
        if (dst == null || dst.isEmpty())
        {
            logger.warn("there is no pic.");
            return;
        }

        lock.writeLock().lock();
        PreparedStatement prep = null;
        try
        {
            prep = conn.prepareStatement("delete from daterecords");
            prep.execute();
            prep.close();

            prep = conn.prepareStatement("insert into daterecords values(?,?,?);");
            for (Entry<String, DateRecords> dr : dst.entrySet())
            {
                prep.setString(1, dr.getKey());
                prep.setLong(2, dr.getValue().getPiccount());
                prep.setString(3, dr.getValue().getFirstpic());
                prep.addBatch();
            }
            prep.executeBatch();
            prep.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            logger.warn("caught: ", e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

}

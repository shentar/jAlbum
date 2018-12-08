package com.backend.dao;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class DateTableDao extends AbstractRecordsStore
{
    private static final Logger logger = LoggerFactory.getLogger(DateTableDao.class);

    private static DateTableDao instance = new DateTableDao();

    private static final String DATE_TABLE_NAME = "daterecords";

    private static final String DATE_BACK_TABLE_NAME = "daterecords2";

    private static final String DATE_TMP_TABLE_NAME = "daterecordstmp";

    private DateTableDao()
    {

    }

    public static DateTableDao getInstance()
    {
        return instance;
    }

    public TreeMap<String, TreeMap<String, TreeMap<String, DateRecords>>> getAllDateRecord()
    {
        TreeMap<String, TreeMap<String, TreeMap<String, DateRecords>>> allrecords = new TreeMap<>();
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
                    logger.error("one empty datestr record: " + (date == null ? "" : date));
                    continue;
                }

                String day = date.substring(6, 8);
                String month = date.substring(4, 6);
                String year = date.substring(0, 4);

                TreeMap<String, TreeMap<String, DateRecords>> myear = allrecords.get(year);
                if (myear == null)
                {
                    myear = new TreeMap<>();
                    allrecords.put(year, myear);
                }

                TreeMap<String, DateRecords> mmonth = myear.get(month);
                if (mmonth == null)
                {
                    mmonth = new TreeMap<>();
                    myear.put(month, mmonth);
                }

                DateRecords dr = new DateRecords();
                dr.setDatestr(date);
                dr.setFirstpic(res.getString("firstpichashstr"));
                dr.setPiccount(res.getLong("piccoount"));
                mmonth.put(day, dr);
            }

        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            closeResource(prep, res);
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

    public void refreshDate()
    {
        boolean tmpready = false;
        try
        {
            logger.info("start to prepare new date records.");
            prepareNewRecords();

            tmpready = true;

            logger.info("start to drop the date records");
            lock.writeLock().lock();
            if (checkTableExist(DATE_TABLE_NAME))
            {
                renameTable(DATE_TABLE_NAME, DATE_BACK_TABLE_NAME);
            }
            renameTable(DATE_TMP_TABLE_NAME, DATE_TABLE_NAME);

            if (checkTableExist(DATE_BACK_TABLE_NAME))
            {
                renameTable(DATE_BACK_TABLE_NAME, DATE_TMP_TABLE_NAME);
            }
            logger.info("refresh all date record.");
        }
        catch (Exception e)
        {
            logger.warn("caused by ", e);
        }
        finally
        {
            if (tmpready)
            {
                lock.writeLock().unlock();
            }
        }
    }

    private void prepareNewRecords() throws SQLException
    {
        /**
         * CREATE TABLE daterecords ( datestr STRING NOT NULL UNIQUE, piccoount
         * BIGINT NOT NULL, firstpichashstr STRING );
         */
        if (checkTableExist(DATE_TMP_TABLE_NAME))
        {
            cleanTable(DATE_TMP_TABLE_NAME);
        }
        else
        {
            // create the temp table.
            execute("CREATE TABLE " + DATE_TMP_TABLE_NAME + " ( datestr STRING NOT NULL UNIQUE,"
                    + " piccoount BIGINT NOT NULL, firstpichashstr STRING );");
        }

        if (checkTableExist(DATE_BACK_TABLE_NAME))
        {
            dropTable(DATE_BACK_TABLE_NAME);
        }

        Map<String, DateRecords> dst = UniqPhotosStore.getInstance().genAllDateRecords();
        if (dst == null || dst.isEmpty())
        {
            logger.warn("there is no pic.");
            return;
        }

        PreparedStatement prep = null;

        logger.info("start to insert the records to the tmp table.");
        for (Entry<String, DateRecords> dr : dst.entrySet())
        {
            try
            {
                prep = conn
                        .prepareStatement("insert into " + DATE_TMP_TABLE_NAME + " values(?,?,?);");
                prep.setString(1, dr.getKey());
                prep.setLong(2, dr.getValue().getPiccount());
                prep.setString(3, dr.getValue().getFirstpic());

                prep.execute();
            }
            catch (Exception e)
            {
                logger.warn("Exception ", e);
            }
            finally
            {
                closeResource(prep, null);
                prep = null;
            }
        }
        logger.info("end to insert the records to the tmp table.");
    }
}

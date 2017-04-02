package com.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractRecordsStore
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractRecordsStore.class);

    protected Connection conn = SqliteConnManger.getInstance().getConn();

    protected ReadWriteLock lock = new ReentrantReadWriteLock(false);

    protected boolean checkTableExist(String tablename)
    {
        if (StringUtils.isBlank(tablename))
        {
            return false;
        }

        PreparedStatement prep = null;
        ResultSet res = null;

        try
        {
            prep = conn.prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master where type='table' and name=?;");
            prep.setString(1, tablename);
            res = prep.executeQuery();

            if (res.next())
            {
                if (res.getInt(1) == 1)
                {
                    logger.info("the table {} is exist.", tablename);
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            if (prep != null)
            {
                try
                {
                    prep.close();
                }
                catch (SQLException e)
                {
                    logger.warn("caused by: ", e);
                }
            }
        }

        logger.info("the table {} is not exist.", tablename);
        return false;
    }

    protected void renameTable(String src, String dst) throws SQLException
    {
        PreparedStatement prep = null;
        try
        {
            prep = conn.prepareStatement("ALTER TABLE " + src + " RENAME TO " + dst + ";");
            prep.execute();
            logger.info("rename {} to {} successfully!", src, dst);
        }
        finally
        {
            if (prep != null)
            {
                prep.close();
            }
        }

    }

    protected void dropTable(String table) throws SQLException
    {
        PreparedStatement prep = null;
        try
        {
            prep = conn.prepareStatement("DROP TABLE " + table + ";");

            prep.execute();
            logger.info("drop the table {} successfully.", table);
        }
        finally
        {
            if (prep != null)
            {
                prep.close();
            }
        }

    }

    protected void cleanTable(String table) throws SQLException
    {
        PreparedStatement prep = null;
        try
        {
            prep = conn.prepareStatement("delete from " + table + ";");

            prep.execute();
            logger.info("drop the table {} successfully.", table);
        }
        finally
        {
            if (prep != null)
            {
                prep.close();
            }
        }

    }

    protected void closeResource(PreparedStatement prep, ResultSet res)
    {
        if (prep != null)
        {
            try
            {
                prep.close();
            }
            catch (Exception e)
            {
                logger.warn("caused by ", e);
            }
        }

        if (res != null)
        {
            try
            {
                res.close();
            }
            catch (Exception e)
            {
                logger.warn("caused by ", e);
            }
        }
    }
}

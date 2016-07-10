package com.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqliteConnManger
{
    private static final Logger logger = LoggerFactory
            .getLogger(SqliteConnManger.class);

    private Connection conn;

    private static boolean isinit = false;

    private static SqliteConnManger instance = new SqliteConnManger();

    private SqliteConnManger()
    {

    }

    public static SqliteConnManger getInstance()
    {
        instance.init();
        return instance;

    }

    public synchronized void init()
    {
        try
        {
            if (isinit)
            {
                return;
            }

            isinit = true;
            Class.forName("org.sqlite.JDBC");
            setConn(DriverManager.getConnection("jdbc:sqlite:dedup.db"));
        }
        catch (SQLException e)
        {
            logger.error("caught: ", e);
        }
        catch (ClassNotFoundException e)
        {
            logger.error("caught: ", e);
        }
    }

    public Connection getConn()
    {
        return conn;
    }

    public void setConn(Connection conn)
    {
        this.conn = conn;
    }
}

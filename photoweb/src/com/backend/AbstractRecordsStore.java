package com.backend;

import java.sql.Connection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AbstractRecordsStore
{
    protected Connection conn = SqliteConnManger.getInstance().getConn();

    protected ReadWriteLock lock = new ReentrantReadWriteLock(false);

}

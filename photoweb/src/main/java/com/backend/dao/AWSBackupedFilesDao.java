package com.backend.dao;

public class AWSBackupedFilesDao extends BackupedFilesDao
{
    private static BackupedFilesDao instance = new AWSBackupedFilesDao();

    private AWSBackupedFilesDao()
    {

    }

    public static BackupedFilesDao getInstance()
    {
        return instance;
    }

    protected String getBackupedTableName()
    {
        return "backupded";
    }
}

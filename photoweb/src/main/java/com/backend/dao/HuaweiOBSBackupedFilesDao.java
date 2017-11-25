package com.backend.dao;

public class HuaweiOBSBackupedFilesDao extends BackupedFilesDao
{
    private static BackupedFilesDao instance = new HuaweiOBSBackupedFilesDao();

    private HuaweiOBSBackupedFilesDao()
    {

    }

    public static BackupedFilesDao getInstance()
    {
        return instance;
    }

    protected String getBackupedTableName()
    {
        return "hwbackupded";
    }
}

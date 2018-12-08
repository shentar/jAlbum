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

    public String getBackupedTableName()
    {
        return "hwbackupded";
    }
}

package com.backend;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.utils.conf.AppConfig;
import com.utils.sys.GloableLockBaseOnString;

public class BaseSqliteStore
{
    private static final Logger logger = LoggerFactory.getLogger(BaseSqliteStore.class);

    private Connection conn = SqliteConnManger.getInstance().getConn();

    private ReadWriteLock lock = new ReentrantReadWriteLock(false);

    private static BaseSqliteStore instance = new BaseSqliteStore();

    // 对于树莓派等系统，最多只能4个线程同时计算缩略图。
    public static final ExecutorService threadPool = Executors.newFixedThreadPool(2);

    private BaseSqliteStore()
    {

    }

    public static BaseSqliteStore getInstance()
    {
        return instance;
    }

    public void dealWithOneHash(File f, String sha256)
    {
        if (f == null || sha256 == null)
        {
            logger.error("the input is invalid.");
            return;
        }

        if (checkIfAlreadyExist(f))
        {
            return;
        }

        insertOneRecord(f, sha256);
    }

    private void insertOneRecord(File f, String sha256)
    {
        PreparedStatement prep = null;
        try
        {
            FileInfo fi = ReadEXIF.genAllInfos(f.getCanonicalPath(), true);
            fi.setHash256(sha256);
            lock.writeLock().lock();
            prep = conn.prepareStatement("insert into files values(?,?,?,?,?,?,?);");
            prep.setString(1, fi.getPath());
            prep.setString(2, fi.getHash256());
            prep.setLong(3, fi.getSize());
            prep.setDate(4, fi.getcTime());
            prep.setDate(5, fi.getPhotoTime());
            prep.setLong(6, fi.getWidth());
            prep.setLong(7, fi.getHeight());
            prep.execute();

            submitAnThumbnailTask(fi);
        }
        catch (SQLException e)
        {
            logger.error("caught: " + f, e);
        }
        catch (IOException e)
        {
            logger.error("caught: " + f, e);
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
                    e.printStackTrace();
                }
            }
            lock.writeLock().unlock();
        }
    }

    public boolean checkIfAlreadyExist(File f)
    {
        lock.readLock().lock();
        ResultSet res = null;
        PreparedStatement prep = null;
        try
        {
            prep = conn.prepareStatement("select * from files where path=?;");
            prep.setString(1, f.getCanonicalPath());

            res = prep.executeQuery();
            lock.readLock().unlock();
            if (res.next())
            {
                FileInfo oldfi = getFileInfoFromTable(res);
                if (oldfi.getSize() == f.length() && oldfi.getcTime() != null
                        && oldfi.getcTime().getTime() == FileTools.getFileCreateTime(f))
                {
                    checkPhotoTime(oldfi);
                    return true;
                }
                else
                {
                    logger.warn("the file was changed, rebuild the record: " + oldfi);
                    deleteOneRecord(oldfi);
                    return false;
                }
            }
        }
        catch (SQLException e)
        {
            logger.error("caught: " + f, e);
        }
        catch (IOException e)
        {
            logger.error("caught: " + f, e);
        }
        finally
        {
            if (res != null)
            {
                try
                {
                    res.close();
                }
                catch (SQLException e)
                {
                    logger.error("caught: ", e);
                }
            }

            if (prep != null)
            {
                try
                {
                    prep.close();
                }
                catch (SQLException e)
                {
                    logger.error("caught: ", e);
                }
            }
        }
        return false;
    }

    private FileInfo getFileInfoFromTable(ResultSet res) throws SQLException
    {
        FileInfo fi = new FileInfo();
        fi.setPath(res.getString("path"));
        fi.setHash256(res.getString("sha256"));
        fi.setSize(res.getLong("size"));
        fi.setcTime(res.getDate("ctime"));
        fi.setPhotoTime(res.getDate("phototime"));
        fi.setWidth(res.getLong("width"));
        fi.setHeight(res.getLong("height"));
        return fi;
    }

    private void checkPhotoTime(FileInfo oldfi) throws IOException
    {
        List<String> suffixs = AppConfig.getInstance().getFileSuffix();
        for (String s : suffixs)
        {
            if (oldfi.getPath().toLowerCase().endsWith(s))
            {
                boolean bneedupdate = false;
                if (oldfi.getPhotoTime() == null || oldfi.getcTime() == null
                        || oldfi.getPhotoTime().getTime() > System.currentTimeMillis())
                {
                    oldfi = ReadEXIF.genAllInfos(oldfi.getPath(), true);
                    bneedupdate = true;
                }

                if (bneedupdate)
                {
                    updatePhotoInfo(oldfi);
                }

                break;
            }
        }
    }

    private void updatePhotoInfo(FileInfo fi)
    {
        try
        {
            lock.writeLock().lock();
            PreparedStatement prep = conn
                    .prepareStatement("update files set phototime=?,width=?,height=? where path=?;");
            prep.setDate(1, fi.getPhotoTime());
            prep.setLong(2, fi.getWidth());
            prep.setLong(3, fi.getHeight());
            prep.setString(4, fi.getPath());
            prep.execute();
            prep.close();
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void scanAllRecords(List<String> excludeDirs)
    {
        lock.readLock().lock();
        ResultSet res = null;
        PreparedStatement prep = null;
        try
        {
            logger.warn("start to check all records in the files table.");
            prep = conn.prepareStatement("select * from files;");
            res = prep.executeQuery();
            lock.readLock().unlock();

            while (res.next())
            {
                FileInfo fi = getFileInfoFromTable(res);
                if (FileTools.checkFileDeleted(fi, excludeDirs))
                {
                    deleteOneRecord(fi);
                    PerformanceStatistics.getInstance().addOneFile(true);
                }
                else
                {
                    if (ThumbnailManager.checkTheThumbnailExist(fi.getHash256()))
                    {
                        PerformanceStatistics.getInstance().addOneFile(false);
                        continue;
                    }
                    else
                    {
                        submitAnThumbnailTask(fi);
                    }
                }
            }
            prep.close();
            PerformanceStatistics.getInstance().printPerformanceLog(System.currentTimeMillis());            
            logger.warn("end checking all records in the files table.");
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
    }

    private void submitAnThumbnailTask(final FileInfo fi)
    {
        boolean isdone = false;

        isdone = GloableLockBaseOnString.getInstance().tryToDo(fi.getHash256());
        if (!isdone)
        {
            logger.info("the task of pic id [{}] is already being done.", fi.getHash256());
            PerformanceStatistics.getInstance().addOneFile(false);
            return;
        }

        threadPool.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ThumbnailManager.checkAndGenThumbnail(fi);
                }
                finally
                {
                    GloableLockBaseOnString.getInstance().done(fi.getHash256());
                }
            }
        });
        PerformanceStatistics.getInstance().addOneFile(true);
    }

    private void deleteOneRecord(FileInfo fi)
    {
        PreparedStatement prep = null;
        try
        {
            lock.writeLock().lock();
            prep = conn.prepareStatement("delete from files where path=?;");
            prep.setString(1, fi.getPath());
            prep.execute();
            prep.close();
        }
        catch (Exception e)
        {
            logger.error("caught: " + fi, e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
}

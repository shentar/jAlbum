package com.backend.dao;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.backend.FileType;
import com.backend.PicStatus;
import com.backend.scan.FileTools;
import com.backend.scan.RefreshFlag;
import com.backend.sync.s3.SyncTool;
import com.utils.conf.AppConfig;
import com.utils.media.MediaTool;
import com.utils.media.ThumbnailManager;
import com.utils.sys.PerformanceStatistics;
import com.utils.web.HeadUtils;

public class BaseSqliteStore extends AbstractRecordsStore
{
    private static final Logger logger = LoggerFactory.getLogger(BaseSqliteStore.class);

    private static BaseSqliteStore instance = new BaseSqliteStore();

    private BaseSqliteStore()
    {

    }

    public static BaseSqliteStore getInstance()
    {
        return instance;
    }

    public FileInfo getOneFileByHashID(String id)
    {
        if (StringUtils.isBlank(id))
        {
            return null;
        }

        PreparedStatement prep = null;
        ResultSet res = null;
        try
        {
            lock.readLock().lock();
            prep = conn
                    .prepareStatement("select * from files where sha256=? and (deleted <> 'true' or deleted is null);");
            prep.setString(1, id);
            res = prep.executeQuery();

            while (res.next())
            {
                FileInfo f = getFileInfoFromTable(res);
                if (new File(f.getPath()).isFile())
                {
                    return f;
                }
            }

            prep.close();
            res.close();
        }
        catch (SQLException e)
        {
            logger.error("caught: " + id, e);
        }

        finally
        {
            lock.readLock().unlock();
        }

        return null;
    }

    /**
     * 相比于并发性能，这里选择不做同步，数据表主键相同会自动互斥。
     * 
     * @param f
     *            文件对象
     * @param sha256
     *            文件hash值。
     */
    public void insertOneRecord(FileInfo fi)
    {
        PreparedStatement prep = null;
        try
        {
            // 检查是否已经存在隐藏的照片的记录。
            checkAndRefreshIfHidden(fi);

            try
            {
                lock.writeLock().lock();
                prep = conn.prepareStatement("insert into files values(?,?,?,?,?,?,?,?,?,?);");
                prep.setString(1, fi.getPath());
                prep.setString(2, fi.getHash256());
                prep.setLong(3, fi.getSize());
                prep.setDate(4, fi.getcTime());
                prep.setDate(5, fi.getPhotoTime());
                prep.setLong(6, fi.getWidth());
                prep.setLong(7, fi.getHeight());
                prep.setString(8, fi.isDel() ? "true" : "false");
                prep.setLong(9, fi.getRoatateDegree());
                prep.setInt(10, fi.getFtype().ordinal());
                prep.execute();
            }
            finally
            {
                lock.writeLock().unlock();
            }

            RefreshFlag.getInstance().getAndSet(true);
            SyncTool.submitSyncTask(fi);
            FileTools.submitAnThumbnailTask(fi);

        }
        catch (Exception e)
        {
            logger.error("caught: " + fi, e);
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
        }
    }

    public void deleteOneRecordByPath(FileInfo fi)
    {
        if (fi == null || StringUtils.isBlank(fi.getPath()))
        {
            logger.warn("input file's path is empty.");
            return;
        }
        deleteRecord(fi.getPath(), true);
    }

    public FileInfo getOneFileByPath(String path)
    {
        if (StringUtils.isBlank(path))
        {
            return null;
        }

        ResultSet res = null;
        PreparedStatement prep = null;
        try
        {
            lock.readLock().lock();
            prep = conn.prepareStatement("select * from files where path=?;");
            prep.setString(1, path);

            res = prep.executeQuery();

            if (res.next())
            {
                return getFileInfoFromTable(res);
            }

            lock.readLock().unlock();
        }
        catch (Exception e)
        {
            logger.warn("caught: ", e);
        }
        return null;
    }

    public PicStatus checkIfAlreadyExist(File f)
    {
        ResultSet res = null;
        PreparedStatement prep = null;
        try
        {
            lock.readLock().lock();
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
                    logger.info("the file is exist: " + f);
                    checkPhotoTime(oldfi);
                    return PicStatus.EXIST;
                }
                else
                {
                    logger.warn("the file was changed, rebuild the record: " + oldfi);
                    deleteOneRecordByPath(oldfi);
                    return PicStatus.NOT_EXIST;
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
        return PicStatus.NOT_EXIST;
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
        fi.setDel("true".equalsIgnoreCase(res.getString("deleted")));
        fi.setRoatateDegree(res.getInt("degree"));
        fi.setFtype(FileType.values()[res.getInt("ftype")]);
        logger.debug("the file info is: {}", fi);
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

                FileType ftype = HeadUtils.getFileType(oldfi.getPath());
                if (!oldfi.getFtype().equals(ftype))
                {
                    bneedupdate = true;
                }

                if (oldfi.getPhotoTime() == null || oldfi.getcTime() == null
                        || oldfi.getPhotoTime().getTime() > System.currentTimeMillis())
                {
                    bneedupdate = true;
                }

                if (bneedupdate)
                {
                    oldfi = MediaTool.genFileInfo(oldfi.getPath());
                    logger.info("need to update the file info: " + oldfi);
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
                    .prepareStatement("update files set phototime=?,width=?,height=?,ftype=? where path=?;");
            prep.setDate(1, fi.getPhotoTime());
            prep.setLong(2, fi.getWidth());
            prep.setLong(3, fi.getHeight());
            prep.setInt(4, fi.getFtype().ordinal());
            prep.setString(5, fi.getPath());
            prep.execute();
            prep.close();
            logger.warn("update the file to: {}", fi);
            RefreshFlag.getInstance().getAndSet(true);
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
            prep = conn.prepareStatement("select * from files where deleted is null or deleted <> 'true';");
            res = prep.executeQuery();
            lock.readLock().unlock();

            while (res.next())
            {
                FileInfo fi = getFileInfoFromTable(res);

                // 检查同步S3。
                SyncTool.submitSyncTask(fi);

                if (fi.isDel())
                {
                    // 处于deleted状态的图片不予检查。
                    continue;
                }

                if (FileTools.checkFileDeleted(fi, excludeDirs))
                {
                    setPhotoToBeHiden(fi);
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
                        FileTools.submitAnThumbnailTask(fi);
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

    public void deleteRecord(String str, boolean isPath)
    {
        if (str == null || StringUtils.isBlank(str))
        {
            logger.warn("input file's path is empty.");
            return;
        }

        PreparedStatement prep = null;
        try
        {
            lock.writeLock().lock();
            if (isPath)
            {
                prep = conn.prepareStatement("delete from files where path=?;");
            }
            else
            {
                prep = conn.prepareStatement("delete from files where sha256=?;");
            }
            prep.setString(1, str);
            prep.execute();
            prep.close();
            RefreshFlag.getInstance().getAndSet(true);
        }
        catch (Exception e)
        {
            logger.error("caught: " + str, e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void setPhotoToBeHiden(FileInfo fi)
    {
        if (fi == null || StringUtils.isBlank(fi.getHash256()))
        {
            logger.warn("input file's path is empty.");
            return;
        }
        setPhotoToBeHiden(fi.getHash256(), false);
    }

    public void deleteRecordsInDirs(String dir)
    {
        if (StringUtils.isBlank(dir))
        {
            return;
        }

        PreparedStatement prep = null;
        ResultSet res = null;
        boolean needDel = false;
        try
        {
            lock.readLock().lock();
            prep = conn.prepareStatement("select * from files where path like ?;");
            prep.setString(1, dir + "%");
            res = prep.executeQuery();
            if (res.next())
            {
                needDel = true;
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

        if (!needDel)
        {
            return;
        }

        try
        {
            lock.writeLock().lock();
            prep = conn.prepareStatement(
                    "update files set deleted='true' where path like ? and (deleted <>'true' or deleted is null);");
            prep.setString(1, dir + "%");

            prep.execute();
            prep.close();
            RefreshFlag.getInstance().getAndSet(true);
        }
        catch (Exception e)
        {
            logger.error("caught: " + dir, e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public void setPhotoToBeHiden(String file, boolean isPath)
    {
        if (StringUtils.isBlank(file))
        {
            logger.warn("input file is empty.");
            return;
        }

        PreparedStatement prep = null;
        try
        {
            lock.writeLock().lock();
            if (isPath)
            {
                prep = conn.prepareStatement("update files set deleted='true' where path=?;");
            }
            else
            {
                prep = conn.prepareStatement("update files set deleted='true' where sha256=?;");
            }
            prep.setString(1, file);
            prep.execute();
            prep.close();
            RefreshFlag.getInstance().getAndSet(true);
        }
        catch (Exception e)
        {
            logger.error("caught: " + file, e);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public boolean checkAndRefreshIfHidden(FileInfo fi)
    {
        boolean alreadyexist = false;
        if (fi == null || StringUtils.isBlank(fi.getHash256()))
        {
            logger.warn("input id is empty.");
            return alreadyexist;
        }
        String id = fi.getHash256();
        ResultSet res = null;
        PreparedStatement prep = null;
        try
        {
            lock.readLock().lock();
            // prep = conn.prepareStatement("select * from files where sha256=?
            // and deleted='true';");
            prep = conn.prepareStatement("select * from files where sha256=?;");
            prep.setString(1, id);
            res = prep.executeQuery();

            if (res.next())
            {
                FileInfo fexist = getFileInfoFromTable(res);
                // refresh the file info when the file is deleted
                fi.setDel(fexist.isDel());
                fi.setcTime(fexist.getcTime());
                fi.setPhotoTime(fexist.getPhotoTime());
                alreadyexist = true;
            }

            res.close();
            prep.close();
            // RefreshFlag.getInstance().getAndSet(true);

        }
        catch (Exception e)
        {
            logger.error("caught: " + id, e);
        }
        finally
        {
            lock.readLock().unlock();
        }

        return alreadyexist;
    }
}

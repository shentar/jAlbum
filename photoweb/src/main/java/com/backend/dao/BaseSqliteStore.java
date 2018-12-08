package com.backend.dao;

import com.backend.entity.FileInfo;
import com.backend.entity.FileType;
import com.backend.entity.PicStatus;
import com.backend.facer.FaceRecService;
import com.backend.scan.FileTools;
import com.backend.scan.RefreshFlag;
import com.backend.sync.s3.SyncTool;
import com.utils.conf.AppConfig;
import com.utils.media.MediaTool;
import com.utils.media.ThumbnailManager;
import com.utils.sys.PerformanceStatistics;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class BaseSqliteStore extends AbstractRecordsStore
{
    public static final String tableName = "files";

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
            prep = conn.prepareStatement(
                    "select * from files where sha256=? and (deleted <> 'true' or deleted is null);");
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

        }
        catch (SQLException e)
        {
            logger.error("caught: " + id, e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }

        return null;
    }

    /**
     * 相比于并发性能，这里选择不做同步，数据表主键相同会自动互斥。
     *
     * @param fi 文件对象
     */
    public void insertOneRecord(FileInfo fi)
    {
        PreparedStatement prep = null;
        try
        {
            // 检查是否已经存在隐藏的照片的记录。
            checkAndRefreshFileInfo(fi);

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
                prep.setString(8, fi.getStatus().name());
                prep.setLong(9, fi.getRoatateDegree());
                prep.setInt(10, fi.getFtype().ordinal());
                prep.execute();
                logger.warn("insert one file to the table successfully!" + fi);
            }
            finally
            {
                lock.writeLock().unlock();
            }

            RefreshFlag.getInstance().getAndSet(true);
            if (fi.getStatus() == PicStatus.EXIST)
            {
                // SyncTool.submitSyncTask(fi);
                FaceRecService.getInstance().detactFaces(fi);
            }
            FileTools.submitAnThumbnailTask(fi);

        }
        catch (Exception e)
        {
            logger.error("caught: " + fi, e);
        }
        finally
        {
            closeResource(prep, null);
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
        finally
        {
            closeResource(prep, res);
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

                long fileTime = FileTools.getFileCreateTime(f);
                if (oldfi.getSize() == f.length() && oldfi.getcTime() != null
                        && oldfi.getcTime().getTime() == fileTime)
                {
                    logger.info("the file is exist: " + f);
                    checkPhotoTime(oldfi);
                    return PicStatus.EXIST;
                }
                else
                {
                    logger.warn(
                            "the file was changed, the file info is: {}:{}, {}:{}], rebuild the record: {}",
                            oldfi.getSize(), f.length(), oldfi.getcTime().getTime(), fileTime,
                            oldfi);

                    // TODO 如过被覆盖的文件是已经屏蔽的文件如何处理？
                    deleteOneRecordByPath(oldfi);
                    return PicStatus.NOT_EXIST;
                }
            }
        }
        catch (SQLException | IOException e)
        {
            logger.error("caught: " + f, e);
        }
        finally
        {
            closeResource(prep, res);
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
        fi.setStatus(getFileStatus(res.getString("deleted")));
        fi.setRoatateDegree(res.getInt("degree"));
        fi.setFtype(FileType.values()[res.getInt("ftype")]);
        logger.debug("the file info is: {}", fi);
        return fi;
    }

    private PicStatus getFileStatus(String value)
    {
        if ("true".equalsIgnoreCase(value))
        {
            return PicStatus.HIDDEN;
        }

        if ("false".equalsIgnoreCase(value))
        {
            return PicStatus.EXIST;
        }

        return PicStatus.valueOf(value);
    }

    private void checkPhotoTime(FileInfo oldfi)
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

    public void updatePhotoInfo(FileInfo fi)
    {
        if (fi == null)
        {
            return;
        }

        PreparedStatement prep = null;
        try
        {
            lock.writeLock().lock();
            prep = conn.prepareStatement("update files set phototime=?,width=?,height=?,"
                                                 + "deleted=?,ftype=? where path=?;");
            prep.setDate(1, fi.getPhotoTime());
            prep.setLong(2, fi.getWidth());
            prep.setLong(3, fi.getHeight());
            prep.setString(4, fi.getStatus().name());
            prep.setInt(5, fi.getFtype().ordinal());
            prep.setString(6, fi.getPath());
            prep.execute();
            logger.warn("update the file to: {}", fi);
            RefreshFlag.getInstance().getAndSet(true);
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    public void scanAllRecords(List<String> excludeDirs)
    {
        scanAllRecords(excludeDirs, false);
    }

    public void scanAllRecords(List<String> excludeDirs, boolean needSyncS3)
    {
        lock.readLock().lock();
        ResultSet res = null;
        PreparedStatement prep = null;
        try
        {
            logger.warn("start to check all records in the files table.");
            prep = conn.prepareStatement(
                    "select * from files where deleted is null or deleted <> 'true';");
            res = prep.executeQuery();
            lock.readLock().unlock();

            while (res.next())
            {
                FileInfo fi = getFileInfoFromTable(res);

                PicStatus status = FileTools.checkFileDeleted(fi, excludeDirs);

                if (status == PicStatus.NOTCHANGED && fi.getStatus() == PicStatus.EXIST)
                {
                    // 检查同步S3。
                    if (needSyncS3)
                    {
                        SyncTool.submitSyncTask(fi);
                    }

                    FaceRecService.getInstance().detactFaces(fi);
                    if (ThumbnailManager.checkTheThumbnailExist(fi.getHash256()))
                    {
                        PerformanceStatistics.getInstance().addOneFile(false);
                    }
                    else
                    {
                        FileTools.submitAnThumbnailTask(fi);
                    }
                }
                else if (status != PicStatus.NOTCHANGED && fi.getStatus() != status)
                {
                    fi.setStatus(status);
                    updatePhotoInfo(fi);
                    PerformanceStatistics.getInstance().addOneFile(true);
                }

                if (needSyncS3 && fi.getStatus() == PicStatus.NOT_EXIST)
                {
                    //  if (HuaweiOBSSyncService.getInstance().objectExist(fi))
                    //  {
                    // 从远端云存储上面找回数据。
                    logger.info(
                            "the local file is not exist. please fetch it from the cloud storage: {}",
                            fi);
                    // }
                    // else
                    // {
                    //     logger.warn(
                    //             "the local file not exist, cloud storage file also not exist. fi: {}",
                    //             fi);
                    // }
                }
            }
            PerformanceStatistics.getInstance().printPerformanceLog(System.currentTimeMillis());
            logger.warn("end checking all records in the files table.");
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            closeResource(prep, res);
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
            RefreshFlag.getInstance().getAndSet(true);
        }
        catch (Exception e)
        {
            logger.error("caught: " + str, e);
        }
        finally
        {
            closeResource(prep, null);
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

        boolean deleteFile = false;
        // 以此来识别文件和文件夹。防止误删名称重叠的记录。
        // 删除IMG_0122.MOV时，不至于误删IMG_0122.MOV.mp4
        if (!StringUtils.endsWith(dir, File.separator))
        {
            for (String suffix : AppConfig.getInstance().getFileSuffix())
            {
                if (StringUtils.endsWithIgnoreCase(dir, suffix))
                {
                    deleteFile = true;
                    break;
                }
            }

            if (!deleteFile)
            {
                dir = dir + File.separator;
            }
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

        if (!needDel)
        {
            logger.info("there is not any item in the dir: {}", dir);
            return;
        }

        logger.warn("start to delete the items in the folder: {}", dir);
        try
        {
            lock.writeLock().lock();
            if (deleteFile)
            {
                prep = conn.prepareStatement("update files set deleted=? where path=?;");
            }
            else
            {
                prep = conn.prepareStatement("update files set deleted=? where path like ?;");
            }

            prep.setString(1, PicStatus.NOT_EXIST.name());
            prep.setString(2, dir + "%");
            prep.execute();
            RefreshFlag.getInstance().getAndSet(true);
            logger.warn("end to delete the items in the folder: {}", dir);
        }
        catch (Exception e)
        {
            logger.error("caught: " + dir, e);
        }
        finally
        {
            closeResource(prep, null);
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
            RefreshFlag.getInstance().getAndSet(true);
        }
        catch (Exception e)
        {
            logger.error("caught: " + file, e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    /**
     * @return 返回true，则为新插入，否则更新信息。
     */
    public boolean checkAndRefreshFileInfo(FileInfo fi)
    {
        if (fi == null || StringUtils.isBlank(fi.getHash256()))
        {
            logger.warn("input id is empty.");
            return false;
        }

        String id = fi.getHash256();
        ResultSet res = null;
        PreparedStatement prep = null;
        try
        {
            lock.readLock().lock();
            prep = conn.prepareStatement("select * from files where sha256=?;");
            prep.setString(1, id);
            res = prep.executeQuery();

            if (res.next())
            {
                // 同步已经存在的文件的时间和隐藏状态。
                FileInfo fexist = getFileInfoFromTable(res);

                if (fexist.getStatus() == PicStatus.HIDDEN)
                {
                    fi.setStatus(fexist.getStatus());
                    logger.warn("the file is already hidden: {}, orig: {}", fi, fexist);
                }
                fi.setPhotoTime(fexist.getPhotoTime());
            }

            // RefreshFlag.getInstance().getAndSet(true);
        }
        catch (Exception e)
        {
            logger.error("caught: " + id, e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }

        return true;
    }
}

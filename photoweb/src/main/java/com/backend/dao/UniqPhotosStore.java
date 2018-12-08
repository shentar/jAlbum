package com.backend.dao;

import com.backend.entity.FileInfo;
import com.backend.entity.FileType;
import com.backend.scan.RefreshFlag;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UniqPhotosStore extends AbstractRecordsStore
{
    public static final String tableName = "uniqphotos1";

    private static final Logger logger = LoggerFactory.getLogger(UniqPhotosStore.class);

    private static UniqPhotosStore instance = new UniqPhotosStore();

    private UniqPhotosStore()
    {

    }

    public static UniqPhotosStore getInstance()
    {
        return instance;
    }

    public FileInfo getOneFileByHashStr(String id)
    {
        PreparedStatement prep = null;
        ResultSet res = null;
        try
        {
            lock.readLock().lock();
            prep = conn.prepareStatement("select * from uniqphotos1 where hashstr=?;");
            prep.setString(1, id);
            res = prep.executeQuery();

            if (res.next())
            {
                return getFileInfoFromTable(res);
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

        return null;
    }

    public Map<String, DateRecords> genAllDateRecords()
    {

        PreparedStatement prep = null;
        ResultSet res = null;

        try
        {
            logger.debug("start to get all date records");
            lock.readLock().lock();
            prep = conn.prepareStatement("select * from uniqphotos1;");
            res = prep.executeQuery();

            Map<String, DateRecords> dst = new HashMap<>();
            while (res.next())
            {
                Date ptime = res.getDate("phototime");
                if (ptime == null)
                {
                    logger.warn("some error file: " + res.getString("hashstr"));
                    continue;
                }
                String datestr = HeadUtils.formatDate(ptime);

                DateRecords dr = dst.get(datestr);
                if (dr == null)
                {
                    dr = new DateRecords();
                    dr.setDatestr(datestr);
                    dr.setPiccount(1);
                    dr.setFirstpic(res.getString("hashstr"));
                    dst.put(datestr, dr);
                }
                else
                {
                    dr.setPiccount(dr.getPiccount() + 1);
                }
            }
            return dst;
        }
        catch (SQLException e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
            logger.debug("end to get all date records.");
        }

        return null;
    }

    public List<FileInfo> getNextNineFileByHashStr(String id, int count, boolean isnext,
                                                   boolean isvideo)
    {
        logger.debug("start to get file list: {}, {}, {}, {}", id, count, isnext, isvideo);
        PreparedStatement prep = null;
        ResultSet res = null;
        try
        {
            lock.readLock().lock();
            String sqlstr = "select * from uniqphotos1";

            FileInfo fi = null;
            if (id != null)
            {
                fi = getOneFileByHashStr(id);
            }

            if (fi != null)
            {
                sqlstr += " where phototime";
                if (isnext)
                {
                    sqlstr += "<?";
                }
                else
                {
                    sqlstr += ">?";
                }

                if (isvideo)
                {
                    sqlstr += " and ftype==?";
                }

                if (!isnext)
                {
                    sqlstr += " order by phototime asc";
                }
            }
            else
            {
                if (isvideo)
                {
                    sqlstr += " where ftype==?";
                }

                if (!isnext)
                {
                    sqlstr += " order by phototime asc";
                }
            }

            sqlstr += " limit " + count + ";";
            prep = conn.prepareStatement(sqlstr);

            if (fi != null)
            {
                prep.setDate(1, fi.getPhotoTime());
                if (isvideo)
                {
                    prep.setInt(2, FileType.VIDEO.ordinal());
                }
            }
            else
            {
                if (isvideo)
                {
                    prep.setInt(1, FileType.VIDEO.ordinal());
                }
            }

            res = prep.executeQuery();

            List<FileInfo> lst = new LinkedList<>();
            while (res.next())
            {
                FileInfo f = getFileInfoFromTable(res);
                lst.add(f);
            }

            if (!isnext && !lst.isEmpty())
            {
                Collections.reverse(lst);
            }

            return lst;
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
            logger.debug("end to get file list: {}, {}, {}, {}", id, count, isnext, isvideo);
        }

        return null;
    }

    public void getDupFiles()
    {
        /**
         * select * from files where sha256 in( select sha256 from files group
         * by sha256 having count(sha256)>1 ) ORDER BY sha256
         */
        PreparedStatement prep;
        try
        {
            boolean ut1 = checkTableExist("uniqphotos1");
            boolean ut2 = checkTableExist("uniqphotos2");
            boolean utmp = checkTableExist("uniqphotostmp");

            if (ut1 && ut2 && utmp)
            {
                logger.warn("three tables are both exist.");
                dropTable("uniqphotostmp");
            }
            else if ((!ut1) && ut2 && utmp)
            {
                lock.writeLock().lock();
                renameTable("uniqphotos2", "uniqphotos1");
                lock.writeLock().unlock();
                renameTable("uniqphotostmp", "uniqphotos2");
            }
            else if (ut1 && (!ut2) && utmp)
            {
                renameTable("uniqphotostmp", "uniqphotos2");
            }
            else if (!(ut1 && ut2))
            {
                logger.error("some error occured.");
                return;
            }

            cleanTable("uniqphotos2");
            logger.info("delete all records form uniqphotos2.");

            BaseSqliteStore.getInstance().lock.readLock().lock();
            prep = conn.prepareStatement("insert into uniqphotos2(path,hashstr,size,phototime,"
                                                 + "width,height,degree, ftype) select path,sha256,size,"
                                                 + "phototime,width,height,degree,ftype from files where "
                                                 + "(deleted=='false' or deleted=='EXIST' or deleted is null) and "
                                                 + "sha256 in(select sha256 from files group by sha256) "
                                                 + "group by sha256 ORDER BY phototime DESC");
            prep.execute();
            closeResource(prep, null);
            BaseSqliteStore.getInstance().lock.readLock().unlock();
            logger.info("insert new records to uniqphotos2.");

            lock.writeLock().lock();
            renameTable("uniqphotos1", "uniqphotostmp");
            logger.info("delete all records form uniqphotos1.");
            renameTable("uniqphotos2", "uniqphotos1");
            logger.info("insert new records to uniqphotos1.");
            lock.writeLock().unlock();

            renameTable("uniqphotostmp", "uniqphotos2");
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }

    }

    private FileInfo getFileInfoFromTable(ResultSet res) throws SQLException
    {
        FileInfo fi = new FileInfo();
        fi.setPath(res.getString("path"));
        fi.setHash256(res.getString("hashstr"));
        fi.setSize(res.getLong("size"));
        fi.setPhotoTime(res.getDate("phototime"));
        fi.setWidth(res.getLong("width"));
        fi.setHeight(res.getLong("height"));
        fi.setRoatateDegree(res.getInt("degree"));
        fi.setFtype(FileType.values()[res.getInt("ftype")]);
        logger.debug("the file info is: {}", fi);
        return fi;
    }

    public List<FileInfo> getAllPhotosBy(String day)
    {
        java.util.Date d;
        d = HeadUtils.parseDate(day);
        if (d == null)
        {
            return new ArrayList<>();
        }

        PreparedStatement prep = null;
        ResultSet res = null;
        Date dstart = new Date(d.getTime());
        Date dend = new Date(d.getTime() + 24 * 3600 * 1000);
        try
        {
            lock.readLock().lock();
            prep = conn.prepareStatement(
                    "select * from uniqphotos1 where phototime>=? and phototime<?;");
            prep.setDate(1, dstart);
            prep.setDate(2, dend);
            res = prep.executeQuery();

            List<FileInfo> flst = new LinkedList<>();
            while (res.next())
            {
                FileInfo f = getFileInfoFromTable(res);
                flst.add(f);
            }
            return flst;
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
        return null;
    }

    public void deleteRecordByID(String id)
    {
        if (id == null || StringUtils.isBlank(id))
        {
            logger.warn("input file's path is empty.");
            return;
        }

        PreparedStatement prep = null;
        try
        {
            lock.writeLock().lock();
            prep = conn.prepareStatement("delete from uniqphotos1 where hashstr=?;");
            prep.setString(1, id);
            prep.execute();
            prep.close();
            RefreshFlag.getInstance().getAndSet(true);
        }
        catch (Exception e)
        {
            logger.error("caught: " + id, e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    public long getVideoCount()
    {
        PreparedStatement prep = null;
        ResultSet res = null;
        try
        {
            lock.writeLock().lock();
            prep = conn.prepareStatement("select count(1) from uniqphotos1 where ftype=?;");
            prep.setInt(1, FileType.VIDEO.ordinal());
            res = prep.executeQuery();
            if (res.next())
            {
                return res.getLong(1);
            }
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

        return -1;
    }
}

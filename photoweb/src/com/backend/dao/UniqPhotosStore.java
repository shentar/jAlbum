package com.backend.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.backend.FileType;
import com.backend.scan.RefreshFlag;
import com.utils.web.HeadUtils;

public class UniqPhotosStore extends AbstractRecordsStore
{
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
        try
        {
            lock.readLock().lock();
            PreparedStatement prep = null;
            ResultSet res = null;
            try
            {
                prep = conn.prepareStatement("select * from uniqphotos1 where hashstr=?;");
                prep.setString(1, id);
                res = prep.executeQuery();

                if (res.next())
                {
                    FileInfo f = getFileInfoFromTable(res);
                    return f;
                }
                res.close();
                prep.close();
            }
            catch (Exception e)
            {
                logger.error("caught: ", e);
            }
        }
        finally
        {
            lock.readLock().unlock();
        }

        return null;
    }

    public Map<String, DateRecords> genAllDateRecords()
    {
        lock.readLock().lock();
        PreparedStatement prep = null;
        ResultSet res = null;

        try
        {
            prep = conn.prepareStatement("select * from uniqphotos1;");
            res = prep.executeQuery();
            lock.readLock().unlock();

            Map<String, DateRecords> dst = new HashMap<String, DateRecords>();
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
            prep.close();
            res.close();
            return dst;
        }
        catch (SQLException e)
        {
            logger.error("caught: ", e);
        }

        return null;
    }

    public List<FileInfo> getNextNineFileByHashStr(String id, int count, boolean isnext,
            boolean isvideo)
    {
        try
        {
            lock.readLock().lock();
            PreparedStatement prep = null;
            ResultSet res = null;
            try
            {
                String sqlstr = "select * from uniqphotos1";

                FileInfo fi = null;
                if (id != null)
                {
                    fi = getOneFileByHashStr(id);
                }

                if (fi != null)
                {
                    sqlstr += " where phototime<?";

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

                List<FileInfo> lst = new LinkedList<FileInfo>();
                while (res.next())
                {
                    FileInfo f = getFileInfoFromTable(res);
                    lst.add(f);
                }

                res.close();
                prep.close();

                return lst;
            }
            catch (Exception e)
            {
                logger.error("caught: ", e);
            }
        }
        finally
        {
            lock.readLock().unlock();
        }

        return null;
    }

    public void getDupFiles()
    {
        /**
         * select * from files where sha256 in( select sha256 from files group
         * by sha256 having count(sha256)>1 ) ORDER BY sha256
         */
        PreparedStatement prep = null;
        try
        {
            prep = conn.prepareStatement("delete from uniqphotos2;");
            prep.execute();
            prep.close();
            logger.warn("delete all records form uniqphotos2.");

            prep = conn.prepareStatement("insert into uniqphotos2(path,hashstr,size,phototime,"
                    + "width,height,degree, ftype) select path,sha256,size,"
                    + "phototime,width,height,degree,ftype from files where "
                    + "(deleted <>'true' or deleted is null) and "
                    + "sha256 in(select sha256 from files group by sha256) "
                    + "group by sha256 ORDER BY phototime DESC");
            prep.execute();
            prep.close();
            logger.warn("insert new records to uniqphotos2.");

            try
            {
                lock.writeLock().lock();
                prep = conn.prepareStatement("delete from uniqphotos1;");
                prep.execute();
                prep.close();
                logger.warn("delete all records form uniqphotos1.");

                prep = conn.prepareStatement("insert into uniqphotos1 select * from uniqphotos2;");
                prep.execute();
                prep.close();
                logger.warn("insert new records to uniqphotos1.");
            }
            finally
            {
                lock.writeLock().unlock();
            }
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            try
            {
                prep.close();
            }
            catch (Exception e)
            {
                logger.error("caught: ", e);
            }
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
        java.util.Date d = null;
        d = HeadUtils.parseDate(day);
        if (d == null)
        {
            return new ArrayList<FileInfo>();
        }

        try
        {
            lock.readLock().lock();
            PreparedStatement prep = null;
            ResultSet res = null;
            Date dstart = new Date(d.getTime());
            Date dend = new Date(d.getTime() + 24 * 3600 * 1000);
            try
            {
                prep = conn.prepareStatement(
                        "select * from uniqphotos1 where phototime>=? and phototime<?;");
                prep.setDate(1, dstart);
                prep.setDate(2, dend);
                res = prep.executeQuery();

                List<FileInfo> flst = new LinkedList<FileInfo>();
                while (res.next())
                {
                    FileInfo f = getFileInfoFromTable(res);
                    flst.add(f);
                }
                res.close();
                prep.close();
                return flst;
            }
            catch (Exception e)
            {
                logger.error("caught: ", e);
            }
        }
        finally
        {
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
            lock.writeLock().unlock();
        }
    }
}

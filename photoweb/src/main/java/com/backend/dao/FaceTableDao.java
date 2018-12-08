package com.backend.dao;

import com.backend.entity.FileInfo;
import com.backend.facer.Face;
import com.backend.facer.FacerUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FaceTableDao extends AbstractRecordsStore
{
    private static final Logger logger = LoggerFactory.getLogger(FaceTableDao.class);

    private static FaceTableDao instance = new FaceTableDao();

    private boolean isInit = false;

    private FaceTableDao()
    {

    }

    public static FaceTableDao getInstance()
    {
        instance.checkAndCreateTable();
        return instance;
    }

    private synchronized void checkAndCreateTable()
    {
        if (isInit)
        {
            return;
        }

        PreparedStatement prep;
        try
        {
            if (checkTableExist("faces"))
            {
                isInit = true;
                return;
            }

            logger.warn("the table [faces] is not exist, created.");

            prep = conn.prepareStatement(
                    "CREATE TABLE faces (facetoken STRING, etag STRING (32, 32), "
                            + "pos STRING, faceid BIGINT, quality STRING, gender STRING, age STRING, ptime DATE);");
            prep.execute();
            prep.close();

            prep = conn.prepareStatement(
                    "CREATE INDEX faceindex ON faces (facetoken, etag, faceid, ptime);");
            prep.execute();
            prep.close();
            isInit = true;
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
    }

    public void insertOneRecord(Face face)
    {
        if (face == null)
        {
            return;
        }

        PreparedStatement prep = null;
        lock.writeLock().lock();
        try
        {
            prep = conn.prepareStatement("insert into faces values(?,?,?,?,?,?,?,?);");
            prep.setString(1, face.getFacetoken());
            prep.setString(2, face.getEtag());
            prep.setString(3, face.getPos());
            prep.setLong(4, face.getFaceid());
            prep.setString(5, face.getQuality());
            prep.setString(6, face.getGender());
            prep.setString(7, face.getAge());
            if (face.getFi() != null)
            {
                prep.setDate(8, face.getFi().getPhotoTime());
            }
            else
            {
                prep.setDate(8, new Date(face.getPtime()));
            }

            prep.execute();
            logger.warn("add one face to the faces: {}", face);
        }
        catch (SQLException e)
        {
            logger.error("caught: " + face, e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    public void addRecords(List<Face> faces)
    {
        if (faces == null || faces.isEmpty())
        {
            return;
        }

        for (Face f : faces)
        {
            insertOneRecord(f);
        }
    }


    public boolean checkAlreadyDetect(String eTag)
    {
        if (StringUtils.isBlank(eTag))
        {
            return false;
        }

        PreparedStatement prep = null;
        ResultSet res = null;
        lock.readLock().lock();
        try
        {
            prep = conn.prepareStatement("select * from faces where etag=?;");
            prep.setString(1, eTag);
            res = prep.executeQuery();

            if (res.next())
            {
                logger.info("already exist: {}", eTag);
                return true;
            }
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }
        return false;
    }

    public void addEmptyRecords(FileInfo fi)
    {
        Face f = new Face();
        f.setEtag(fi.getHash256());
        f.setFaceid(-1);
        f.setPos("null");
        f.setFacetoken("null");
        f.setAge("null");
        f.setGender("null");
        f.setQuality("0");
        f.setFi(fi);
        logger.warn("add one empty record: [{}]", f);
        insertOneRecord(f);
    }

    public List<Face> getAllNewFaces()
    {
        PreparedStatement prep = null;
        ResultSet res = null;
        lock.readLock().lock();
        try
        {
            prep = conn.prepareStatement("select * from faces where faceid=? "
                                                 + "and facetoken <> ? order by quality asc;");
            prep.setLong(1, -1);
            prep.setString(2, "null");
            res = prep.executeQuery();

            List<Face> flst = new LinkedList<>();
            while (res.next())
            {
                flst.add(getFaceFromTableRecord(res));
            }

            return flst;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }

        return null;
    }

    public Face getFace(String token, boolean needFileInfo)
    {
        if (StringUtils.isBlank(token))
        {
            return null;
        }

        PreparedStatement prep = null;
        ResultSet res = null;
        lock.readLock().lock();
        try
        {
            prep = conn.prepareStatement("select * from faces where facetoken=?;");
            prep.setString(1, token);
            res = prep.executeQuery();

            Face f;
            if (res.next())
            {
                f = getFaceFromTableRecord(res);

                if (f != null && needFileInfo)
                {
                    f.setFi(UniqPhotosStore.getInstance().getOneFileByHashStr(f.getEtag()));
                }
                return f;
            }
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }

        return null;
    }

    public Face getFace(String token)
    {
        return getFace(token, true);
    }

    private Face getFaceFromTableRecord(ResultSet res) throws SQLException
    {
        /*
         * CREATE TABLE faces (facetoken STRING, etag STRING (32, 32), pos
         * STRING, faceid BIGINT, quality STRING, gender STRING, age STRING,
         * ptime DATE);
         * 
         */
        Face f = new Face();
        f.setEtag(res.getString("etag"));
        f.setFacetoken(res.getString("facetoken"));
        f.setPos(res.getString("pos"));
        f.setFaceid(res.getLong("faceid"));
        f.setGender(res.getString("gender"));
        f.setAge(res.getString("age"));
        f.setQuality(res.getString("quality"));
        Date pt = res.getDate("ptime");
        f.setPtime(pt == null ? 0L : pt.getTime());
        logger.debug("get a face record: {}", f);
        return f;
    }

    public void updateFaceID(Face f)
    {
        if (f == null)
        {
            return;
        }

        PreparedStatement prep = null;
        lock.writeLock().lock();
        try
        {
            prep = conn.prepareStatement("update faces set faceid=? where facetoken=?;");
            prep.setLong(1, f.getFaceid());
            prep.setString(2, f.getFacetoken());
            prep.execute();
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    public void deleteOneFile(String f)
    {
        if (f == null)
        {
            return;
        }

        PreparedStatement prep = null;
        lock.writeLock().lock();
        try
        {
            prep = conn.prepareStatement("delete from faces where etag=?;");
            prep.setString(1, f);
            prep.execute();
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    public void updateFaceID(List<Face> flst)
    {
        if (flst == null || flst.isEmpty())
        {
            return;
        }

        for (Face f : flst)
        {
            updateFaceID(f);
        }
    }

    public List<Long> getAllValidFaceID(int facecount)
    {
        List<Long> lst = new ArrayList<>();
        PreparedStatement prep = null;
        ResultSet res = null;
        lock.readLock().lock();
        try
        {
            prep = conn.prepareStatement(
                    "select faceid,count(faceid) " + "from faces where faceid!='-1' "
                            + "group by faceid order by count(faceid) desc limit " + (
                            (facecount > 0 && facecount < 300) ? facecount : 25) + ";");
            res = prep.executeQuery();

            while (res.next())
            {
                lst.add(res.getLong(1));
            }

            return lst;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }
        return lst;
    }

    public Face getNewestFaceByID(long id, boolean needFileInfo)
    {
        PreparedStatement prep = null;
        ResultSet res = null;
        lock.readLock().lock();
        try
        {
            prep = conn.prepareStatement(
                    "select * from faces where faceid=? order by quality desc limit 1;");
            prep.setLong(1, id);
            res = prep.executeQuery();

            if (res.next())
            {
                return getFaceFromTableRecord(res);
            }
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }

        return null;
    }

    public List<Face> getFacesByID(long id, boolean needFileInfo)
    {
        if (id < 0)
        {
            return null;
        }

        List<Face> lst = new ArrayList<>();
        PreparedStatement prep = null;
        ResultSet res = null;
        lock.readLock().lock();
        try
        {
            prep = conn.prepareStatement("select * from faces where faceid=?;");
            prep.setLong(1, id);
            res = prep.executeQuery();

            while (res.next())
            {
                Face f = getFaceFromTableRecord(res);

                if (needFileInfo)
                {
                    FileInfo fi = UniqPhotosStore.getInstance().getOneFileByHashStr(f.getEtag());
                    if (fi != null)
                    {
                        f.setFi(fi);
                    }
                }
                lst.add(f);
            }

            return lst;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }

        return lst;
    }

    public void updateFaceID(long oldfaceid, long newfaceid)
    {
        if (oldfaceid == -1 || newfaceid == -1)
        {
            return;
        }

        PreparedStatement prep = null;
        lock.writeLock().lock();
        try
        {
            prep = conn.prepareStatement("update faces set faceid=? where faceid=?;");
            prep.setLong(1, newfaceid);
            prep.setLong(2, oldfaceid);
            prep.execute();
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
        }
    }

    public List<Face> getNextNoFacesPics(String id, int count, boolean isNext)
    {
        if (count <= 0)
        {
            return null;
        }

        PreparedStatement prep = null;
        ResultSet res = null;

        try
        {
            lock.readLock().lock();
            FileInfo f = null;
            if (StringUtils.isNotBlank(id))
            {
                f = UniqPhotosStore.getInstance().getOneFileByHashStr(id);
            }

            String statment = "select * from faces where facetoken='null' ";
            if (f == null)
            {
                statment +=
                        " order by ptime " + (isNext ? "desc" : "asc") + " limit " + count + ";";
                prep = conn.prepareStatement(statment);
            }
            else
            {
                statment +=
                        " and ptime " + (isNext ? "<" : ">") + "?" + " order by ptime " + (isNext
                                                                                           ? "desc"
                                                                                           : "asc")
                                + " limit " + count + ";";
                prep = conn.prepareStatement(statment);
                prep.setDate(1, f.getPhotoTime());
            }
            res = prep.executeQuery();

            List<Face> flst = new LinkedList<>();
            while (res.next())
            {
                flst.add(getFaceFromTableRecord(res));
            }

            return flst;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }

        return null;
    }

    public List<Face> getNextNineFileByHashStr(String id, int count, boolean isnext)
    {
        if (count <= 0)
        {
            return null;
        }

        try
        {
            lock.readLock().lock();
            Face f = null;
            if (StringUtils.isNotBlank(id))
            {
                f = getFace(id);
            }

            if (f == null)
            {
                return null;
            }

            long faceID = f.getFaceid();
            if (faceID == -1)
            {
                return null;
            }

            /*
             * String sqlstr = "select * from faces where faceid=? and quality"
             * + (isnext ? "<" : ">") + "=? order by quality desc limit " +
             * count; prep = conn.prepareStatement(sqlstr); prep.setLong(1,
             * faceID); prep.setString(2, f.getQuality()); res =
             * prep.executeQuery();
             * 
             * List<Face> flst = new LinkedList<Face>(); while (res.next()) {
             * flst.add(getFaceFromTableRecord(res)); }
             */

            List<Face> allf = getFacesByID(faceID, false);
            FacerUtils.sortByTime(allf);
            if (!isnext)
            {
                Collections.reverse(allf);
            }

            int maxCount = allf.size() < count ? allf.size() : count;

            int c = 0;
            List<Face> flst = new LinkedList<>();
            boolean start = false;
            for (Face face : allf)
            {
                if (!start && face.getFacetoken().equals(f.getFacetoken()))
                {
                    start = true;
                    continue;
                }

                if (start)
                {
                    flst.add(face);
                    c++;
                    if (c >= maxCount)
                    {
                        break;
                    }
                }
            }

            // 当翻页到最后一页时，会出现找不到有效照片的情况，此时可能已经翻转到第一页。
            if (flst.isEmpty() && !allf.isEmpty())
            {
                c = 0;
                for (Face face : allf)
                {
                    flst.add(face);
                    c++;
                    if (c >= maxCount)
                    {
                        break;
                    }
                }
            }

            if (!isnext)
            {
                Collections.reverse(flst);
            }

            return flst;
        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
        finally
        {
            lock.readLock().unlock();
        }

        return null;
    }

    public void deleteInvalidFaces()
    {
        PreparedStatement prep = null;
        lock.writeLock().lock();
        UniqPhotosStore.getInstance().lock.readLock().lock();
        try
        {
            prep = conn.prepareStatement("delete from faces where etag not in"
                                                 + " (select hashstr from uniqphotos1 group by hashstr);");
            prep.execute();
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, null);
            lock.writeLock().unlock();
            UniqPhotosStore.getInstance().lock.readLock().unlock();
        }
    }

    public Face getFaceByEtag(FileInfo f)
    {
        if (f == null)
        {
            return null;
        }

        PreparedStatement prep = null;
        ResultSet res = null;
        lock.readLock().lock();
        try
        {
            prep = conn.prepareStatement("select * from faces where etag=?;");
            prep.setString(1, f.getHash256());

            res = prep.executeQuery();

            if (res.next())
            {
                Face face = getFaceFromTableRecord(res);
                face.setFi(f);
                return face;
            }

            return null;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            closeResource(prep, res);
            lock.readLock().unlock();
        }
        return null;
    }
}

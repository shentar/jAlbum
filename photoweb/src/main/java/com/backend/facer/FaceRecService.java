package com.backend.facer;

import com.backend.dao.FaceTableDao;
import com.backend.entity.FileInfo;
import com.backend.threadpool.ThreadPoolFactory;
import com.utils.conf.AppConfig;
import com.utils.media.MediaTool;
import com.utils.media.ThumbnailManager;
import com.utils.sys.GlobalLockBaseOnString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class FaceRecService
{
    private static final Logger logger = LoggerFactory.getLogger(FaceRecService.class);

    private static final ExecutorService threadPool =
            ThreadPoolFactory.getThreadPool(ThreadPoolFactory.FACE_REC_SERVICE);

    private static class ServiceHolder
    {
        private static final FaceRecService instance = new FaceRecService();
    }

    public static FaceRecService getInstance()
    {
        return ServiceHolder.instance;
    }

    public void detactFaces(final FileInfo fi)
    {
        if (MediaTool.isVideo(fi.getPath()))
        {
            return;
        }

        if (!AppConfig.getInstance().isFacerConfigured() || !new File(fi.getPath()).exists())
        {
            return;
        }

        if (!GlobalLockBaseOnString.getInstance(GlobalLockBaseOnString.FACE_DETECT_LOCK)
                .tryToDo(fi.getHash256()))
        {
            logger.warn("a task already asigned {}", fi);
            return;
        }

        threadPool.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (FaceTableDao.getInstance().checkAlreadyDetect(fi.getHash256()))
                    {
                        return;
                    }

                    detectOnePic(fi);
                }
                catch (Throwable e)
                {
                    logger.warn("caused by: ", e);
                }
                finally
                {
                    GlobalLockBaseOnString.getInstance(GlobalLockBaseOnString.FACE_DETECT_LOCK)
                            .done(fi.getHash256());
                }
            }
        });
    }

    private void detectOnePic(FileInfo fi)
    {
        try
        {
            logger.warn("start to detect one file {}", fi);
            List<Face> ls = FaceDetectClient.detectFace(fi);
            if (ls == null || ls.isEmpty())
            {
                FaceTableDao.getInstance().addEmptyRecords(fi);
                return;
            }

            boolean isSucc = true;
            for (Face f : ls)
            {
                isSucc = isSucc && (FaceSetManager.getInstance().addFaceToSet(f.getFacetoken()));
            }

            if (isSucc)
            {
                FaceTableDao.getInstance().addRecords(ls);
                checkFaces(ls);
            }
            else
            {
                logger.warn("add face to face set failed: {}, faces: {}", fi, ls);
            }
        }
        finally
        {
            logger.warn("end to detect the file {}", fi);
        }
    }

    public void checkAllFacesID()
    {
        checkFaces(FaceTableDao.getInstance().getAllNewFaces());
    }

    public void checkFaces(List<Face> lst)
    {
        if (lst == null || lst.isEmpty())
        {
            logger.info("there is no face to search.");
            return;
        }

        while (!lst.isEmpty())
        {
            Face f = lst.remove(0);
            checkOneFace(f);
        }
    }

    public void checkOneFace(Face f)
    {
        try
        {
            determineFaceID(f, FaceSearchClient.searchFaces(f.getFacetoken()));
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }

    }

    private void determineFaceID(Face f, List<Face> rlst)
    {
        if (rlst != null)
        {
            if (rlst.isEmpty())
            {
                logger.warn("search no faceid in the db. need acquire a new faiceid: {}", f);
                f.setFaceid(FaceIDManager.getInstance().acquireNewFaceID());
                FaceTableDao.getInstance().updateFaceID(f);
            }
            else
            {
                rlst.add(f);
                FacerUtils.sortByQuality(rlst);
                logger.warn("searched {} faces in the db of {}.", rlst.size(), f);
                long faceid = -1;

                for (Face rf : rlst)
                {
                    Face fc = FaceTableDao.getInstance().getFace(rf.getFacetoken(), false);

                    if (fc == null)
                    {
                        continue;
                    }

                    if (faceid == -1)
                    {
                        if (fc.getFaceid() != -1)
                        {
                            faceid = fc.getFaceid();
                        }
                    }
                    else
                    {
                        if (fc.getFaceid() != -1)
                        {
                            if (faceid != fc.getFaceid())
                            {
                                FaceTableDao.getInstance().updateFaceID(fc.getFaceid(), faceid);
                            }
                        }
                    }
                }

                if (faceid == -1)
                {
                    faceid = FaceIDManager.getInstance().acquireNewFaceID();
                }

                for (Face ft : rlst)
                {
                    ft.setFaceid(faceid);
                }

                FaceTableDao.getInstance().updateFaceID(rlst);
            }
        }
        else
        {
            logger.warn("search request execute failed: {}", f);
        }
    }

    public List<Face> getSortedFaces(long fid, int count, boolean needFileInfo)
    {
        if (fid > 0)
        {
            List<Face> flst = FaceTableDao.getInstance().getFacesByID(fid, needFileInfo);
            if (flst == null || flst.isEmpty())
            {
                return null;
            }

            FacerUtils.sortByTime(flst);

            if (count > 0)
            {
                List<Face> truncateList = new LinkedList<>();
                int needCount = count > flst.size() ? flst.size() : count;
                for (int i = 0; i != needCount; i++)
                {
                    truncateList.add(flst.get(i));
                }

                return truncateList;
            }

            return flst;
        }

        return null;
    }

    public List<Face> checkAndGetFaceidList()
    {
        logger.info("the special id is null, now gen the face index page.");
        List<Long> faceids = FaceTableDao.getInstance()
                .getAllValidFaceID(AppConfig.getInstance().getMaxFacesCount());
        if (faceids == null || faceids.isEmpty())
        {
            return null;
        }

        List<Face> fflist = new LinkedList<>();
        for (long fid : faceids)
        {
            Face f = FaceTableDao.getInstance().getNewestFaceByID(fid, true);
            if (f != null)
            {
                fflist.add(f);
                ThumbnailManager.checkAndGenFaceThumbnail(f);
            }
        }
        logger.info("the faceid size is: {}", fflist.size());

        return fflist;
    }

}

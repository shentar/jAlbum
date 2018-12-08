package com.utils.media;

import com.backend.dao.BaseSqliteStore;
import com.backend.entity.FileInfo;
import com.backend.facer.Face;
import com.backend.facer.FaceRecService;
import com.backend.facer.FacerUtils;
import com.utils.conf.AppConfig;
import com.utils.sys.GlobalLockBaseOnString;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ThumbnailManager
{
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailManager.class);

    private static boolean isBaseDriValid = false;

    private static String baseDir = null;

    private static String faceDir = null;

    private static String faceTmpDir = null;

    private static String baseTmpDir = null;

    static
    {
        intiDirs();
    }

    private static void intiDirs()
    {
        try
        {
            File f = new File(AppConfig.getInstance().getThumbnailDir());
            if (!f.exists())
            {
                f.mkdirs();
            }
            else
            {
                if (f.isFile())
                {
                    System.out.println("the thumbnail dir is not empty.");
                }
            }

            if (!f.exists() || !f.isDirectory())
            {
                logger.warn("some error folder: " + AppConfig.getInstance().getThumbnailDir());
                return;
            }

            baseDir = f.getCanonicalPath();
            baseTmpDir = baseDir + File.separator + "temp";
            faceDir = baseDir + File.separator + "faces";
            faceTmpDir = faceDir + File.separator + "temp";

            if (!checkAndMkdirs(baseTmpDir) || !checkAndMkdirs(faceDir) || !checkAndMkdirs(faceTmpDir))
            {
                logger.warn("some error folder: {}, {}, {}", baseTmpDir, faceDir, faceTmpDir);
                return;
            }

            isBaseDriValid = true;
        }
        catch (IOException e)
        {
            logger.warn("the input thumbnail dir path is invalid.");
        }
    }

    private static boolean checkAndMkdirs(String fpath)
    {
        File f = new File(fpath);

        if (!f.exists())
        {
            f.mkdirs();
        }
        else
        {
            if (f.isFile())
            {
                logger.warn("the input dir path is error: {}", baseDir);
                return false;
            }
        }

        return true;
    }

    private static String getFaceThumbnailPath(String id)
    {
        return getPicThumbnailPath(id, true);
    }

    private static String getPicThumbnailPath(String id)
    {
        return getPicThumbnailPath(id, false);
    }

    private static String getPicThumbnailPath(String id, boolean isFace)
    {
        String dir2 = id.substring(id.length() - 2, id.length());
        String dir1 = id.substring(id.length() - 4, id.length() - 2);
        return (isFace ? faceDir : baseDir) + File.separator + dir1 + File.separator + dir2
                + File.separator + id;
    }

    public static boolean checkTheThumbnailExist(String id)
    {
        return checkTheThumbnailExist(id, false);
    }

    public static boolean checkFaceThumbnailExist(String id)
    {
        return checkTheThumbnailExist(id, true);
    }

    private static boolean checkTheThumbnailExist(String id, boolean isFace)
    {
        if (!isBaseDriValid || StringUtils.isBlank(id) || id.length() < 4)
        {
            logger.warn("the pic file id is invalid.");
            return false;
        }

        return new File(getPicThumbnailPath(id, isFace)).isFile();
    }

    public static void checkAndGenThumbnail(FileInfo fi)
    {
        if (fi == null)
        {
            return;
        }

        String id = fi.getHash256();
        if (!isBaseDriValid || StringUtils.isBlank(id) || id.length() < 4)
        {
            logger.warn("the pic file id is invalid.");
            return;
        }

        if (checkTheThumbnailExist(id))
        {
            logger.debug("the thumbnail of pic ({}) is alread exist.", id);
            return;
        }

        logger.warn("now try to gen the thumbnail for: {}", fi);
        File thumbnailFile = new File(getPicThumbnailPath(id));
        File parentDir = thumbnailFile.getParentFile();
        if (!parentDir.exists())
        {
            parentDir.mkdirs();
        }

        String tmpFile = baseTmpDir + File.separator + fi.getHash256();
        if (ThumbnailGenerator.createThumbnail(fi, tmpFile, 400, 400, false))
        {
            File tmpF = new File(tmpFile);
            if (!tmpF.renameTo(new File(getPicThumbnailPath(id))))
            {
                logger.warn("generate the Thumbnail file failed!");
            }
            else
            {
                FaceRecService.getInstance().detactFaces(fi);
            }
        }
    }

    public static void checkAndGenFaceThumbnail(Face f)
    {
        if (f == null || StringUtils.isBlank(f.getFacetoken()))
        {
            return;
        }

        String id = f.getFacetoken();

        if (checkFaceThumbnailExist(id))
        {
            logger.debug("the thumbnail of pic ({}) is alread exist.", id);
            return;
        }

        if (!GlobalLockBaseOnString.getInstance(GlobalLockBaseOnString.FACE_THUMBNAIL_LOCK)
                .tryToDo(f.getFacetoken()))
        {
            logger.warn("already gen the file: {}", f);
            return;
        }

        try
        {
            if (!isBaseDriValid || id.length() < 4)
            {
                logger.warn("the pic file id is invalid.");
                return;
            }

            logger.warn("now try to gen the thumbnail for: {}", f);
            File thumbnailFile = new File(getFaceThumbnailPath(id));
            File parentDir = thumbnailFile.getParentFile();
            if (!parentDir.exists())
            {
                parentDir.mkdirs();
            }

            if (f.getFi() == null)
            {
                FileInfo fi = BaseSqliteStore.getInstance().getOneFileByHashID(f.getEtag());
                if (fi == null)
                {
                    return;
                }
                else
                {
                    f.setFi(fi);
                }
            }

            File origFile = new File(f.getFi().getPath());
            if (!origFile.exists() || !origFile.isFile())
            {
                logger.warn("the special file is not exist: {}", f);
                return;
            }

            String tmpFile = faceTmpDir + File.separator + id;
            File tmpF = new File(tmpFile);
            if (ThumbnailGenerator.createFaceThumbnail(FacerUtils.getFileForDetectFaces(f.getFi()),
                    HeadUtils.getFileType(f.getFi().getPath()).name(), f.getPos(), tmpFile))
            {
                if (!tmpF.renameTo(new File(getFaceThumbnailPath(id))))
                {
                    logger.warn("generate the Thumbnail file failed: {}", f);
                }
            }
        }
        finally
        {
            GlobalLockBaseOnString.getInstance(GlobalLockBaseOnString.FACE_THUMBNAIL_LOCK)
                    .done(f.getFacetoken());
        }
    }

    public static File getThumbnail(String id)
    {
        return getThumbnail(id, false);
    }

    public static File getFaceThumbnail(String id)
    {
        return getThumbnail(id, true);
    }

    private static File getThumbnail(String id, boolean isFace)
    {
        if (!isBaseDriValid)
        {
            return null;
        }

        String path = getPicThumbnailPath(id, isFace);
        File f = new File(path);
        if (f.isFile())
        {
            return f;
        }

        return null;
    }
}

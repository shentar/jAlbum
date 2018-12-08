package com.backend.scan;

import com.backend.entity.FileInfo;
import com.backend.entity.PicStatus;
import com.backend.threadpool.ThreadPoolFactory;
import com.utils.conf.AppConfig;
import com.utils.media.ExifCreator;
import com.utils.media.ThumbnailManager;
import com.utils.sys.GlobalLockBaseOnString;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class FileTools
{
    private static final Logger logger = LoggerFactory.getLogger(FileTools.class);

    public static final ExecutorService threadPool =
            ThreadPoolFactory.getThreadPool(ThreadPoolFactory.FILE_TOOL);

    // 对于树莓派等系统，最多只能2个线程同时计算缩略图。
    public static final ExecutorService threadPool4Thumbnail =
            ThreadPoolFactory.getThreadPool(ThreadPoolFactory.THREAD_POOL_4THUMBNAIL);

    public static void readShortFileContent(byte[] buffer, File f) throws IOException
    {
        int maxLen = buffer.length;
        FileInputStream fin = null;
        try
        {
            fin = new FileInputStream(f);

            int offset = 0;
            while (true)
            {
                int readlen = fin.read(buffer, offset, maxLen - offset);
                if (readlen < 0)
                {
                    break;
                }

                offset += readlen;

                if (offset >= maxLen)
                {
                    break;
                }
            }
        }
        finally
        {
            if (fin != null)
            {
                fin.close();
            }
        }
    }

    private static boolean checkFileExist(String path)
    {
        return !StringUtils.isBlank(path) && new File(path).isFile();
    }

    public static boolean checkFileLengthValid(String path)
    {
        if (StringUtils.isBlank(path))
        {
            return false;
        }
        File f = new File(path);

        // 文件大小不合法。
        return f.isFile() && f.length() > AppConfig.getInstance().getMinFileSize()
                && f.length() <= AppConfig.getInstance().getMaxFilesize();

    }

    public static boolean checkExclude(final FileInfo fi, List<String> excludeDirs)
    {
        if (excludeDirs != null && fi != null)
        {
            for (String dir : excludeDirs)
            {
                if (fi.getPath().startsWith(dir))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean checkFileChanged(final FileInfo fi) throws IOException
    {
        if (fi == null)
        {
            return true;
        }

        File f = new File(fi.getPath());
        return !(f.isFile() && fi.getcTime().getTime() == FileTools.getFileCreateTime(f)
                && fi.getSize() == f.length());
    }

    public static PicStatus checkFileDeleted(final FileInfo fi, List<String> excludeDirs)
            throws IOException
    {
        // 判空。
        if (fi == null)
        {
            return PicStatus.ERRORFILE;
        }

        switch (fi.getStatus())
        {
            case EXIST:
                if (!checkFileExist(fi.getPath()))
                {
                    return PicStatus.NOT_EXIST;
                }

                if (!checkFileLengthValid(fi.getPath()))
                {
                    return PicStatus.ERRORFILE;
                }

                if (checkExclude(fi, excludeDirs))
                {
                    return PicStatus.ERRORFILE;
                }

                if (checkFileChanged(fi))
                {
                    return PicStatus.ERRORFILE;
                }
                break;
            case ERRORFILE:
            case NOT_EXIST:
                if (checkFileExist(fi.getPath()) && checkFileLengthValid(fi.getPath())
                        && !checkExclude(fi, excludeDirs) && !checkFileChanged(fi))
                {
                    return PicStatus.EXIST;
                }
                break;
            case HIDDEN:
                if (checkFileChanged(fi))
                {
                    // TODO 已经被隐藏的文件被覆盖成其他文件或者已经删除。
                }
                break;
            default:
                return PicStatus.NOTCHANGED;
        }

        return PicStatus.NOTCHANGED;
    }

    @SuppressWarnings("unused")
    private static void rotateOneFile(FileInfo fi, int angel)
    {
        FileTools.rotatePhonePhoto(fi.getPath(), angel);
        SimpleDateFormat sf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        try
        {
            String tmpFilePath = fi.getPath() + "_tmpfile";
            new File(fi.getPath()).renameTo(new File(tmpFilePath));
            ExifCreator.addExifDate(
                    new String[]{tmpFilePath, fi.getPath(), sf.format(fi.getPhotoTime())});

        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
    }

    public static long getFileCreateTime(File f) throws IOException
    {
        // 取文件的创建时间和修改时间里面较小者。
        if (f == null)
        {
            return -1;
        }
        Path path = FileSystems.getDefault().getPath(f.getParent(), f.getName());
        BasicFileAttributes attrs =
                Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        long timetmp = attrs.lastModifiedTime().toMillis();
        logger.debug("the timetmp is: lastModify[{}], creation[{}], fileLastModify[{}]",
                     attrs.lastModifiedTime(), attrs.creationTime(), new Date(f.lastModified()));
        if (timetmp > attrs.creationTime().toMillis())
        {
            timetmp = attrs.creationTime().toMillis();
        }

        return timetmp;
        // return attrs.creationTime().toMillis();
    }

    /**
     * 旋转照片
     *
     * @return
     */
    public static void rotatePhonePhoto(String fullPath, int angel)
    {
        BufferedImage src;
        try
        {
            File oldFile = new File(fullPath);
            src = ImageIO.read(oldFile);
            int src_width = src.getWidth(null);
            int src_height = src.getHeight(null);

            int swidth = src_width;
            int sheight = src_height;

            if (angel == 90 || angel == 270)
            {
                swidth = src_height;
                sheight = src_width;
            }

            Rectangle rect_des = new Rectangle(new Dimension(swidth, sheight));

            BufferedImage res =
                    new BufferedImage(rect_des.width, rect_des.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = res.createGraphics();

            g2.translate((rect_des.width - src_width) / 2, (rect_des.height - src_height) / 2);
            g2.rotate(Math.toRadians(angel), src_width / 2, src_height / 2);

            g2.drawImage(src, null, null);

            File newTmpFile = new File(fullPath + "tmp_bak");
            ImageIO.write(res, "jpg", newTmpFile);
            oldFile.renameTo(new File(fullPath + ".old"));
            newTmpFile.renameTo(oldFile);
        }
        catch (IOException e)
        {
            logger.warn("cause by: ", e);
        }
    }

    public static boolean copyFile(String src, String dst)
    {
        BufferedInputStream fis = null;
        BufferedOutputStream fos = null;
        try
        {
            byte[] iobuff = new byte[1024 * 16];
            fis = new BufferedInputStream(new FileInputStream(src));
            fos = new BufferedOutputStream(new FileOutputStream(dst));

            int bytes;
            while ((bytes = fis.read(iobuff)) != -1)
            {
                fos.write(iobuff, 0, bytes);
            }

            return true;
        }
        catch (Exception e)
        {
            logger.warn("copy file failed!", e);
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                }
                catch (IOException e)
                {
                    logger.warn("caused by: ", e);
                }
            }
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                    logger.warn("caused by: ", e);
                }
            }
        }
        return false;
    }

    public static void submitAnThumbnailTask(final FileInfo fi)
    {
        if (!GlobalLockBaseOnString.getInstance(GlobalLockBaseOnString.PIC_THUMBNAIL_LOCK)
                .tryToDo(fi.getHash256()))
        {
            logger.warn("the task of pic id [{}] is already being done.", fi);
            return;
        }

        threadPool4Thumbnail.submit(new Runnable()
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
                    GlobalLockBaseOnString.getInstance(GlobalLockBaseOnString.PIC_THUMBNAIL_LOCK)
                            .done(fi.getHash256());
                }
            }
        });
    }

}

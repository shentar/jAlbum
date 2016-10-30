package com.backend.scan;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.backend.FileType;
import com.utils.conf.AppConfig;
import com.utils.media.ExifCreator;
import com.utils.media.ThumbnailManager;
import com.utils.sys.GloableLockBaseOnString;

public class FileTools
{
    private static final Logger logger = LoggerFactory.getLogger(FileTools.class);
    public static boolean usesqlite = "true".equals(System.getProperty("usesqlite", "true")) ? true
            : false;
    public static long lastScanTime = 0;
    public static final ExecutorService threadPool = Executors
            .newFixedThreadPool(AppConfig.getInstance().getThreadCount());

    // 对于树莓派等系统，最多只能2个线程同时计算缩略图。
    public static final ExecutorService threadPool4Thumbnail = Executors.newFixedThreadPool(2);

    public static void readShortFileContent(byte[] buffer, File f)
            throws FileNotFoundException, IOException
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

    public static boolean checkFileDeleted(final FileInfo fi, List<String> excludeDirs)
            throws IOException
    {
        if (fi != null)
        {
            File f = new File(fi.getPath());
            if (f.isFile())
            {
                if (f.length() > AppConfig.getInstance().getMinFileSize())
                {
                    if (fi.getcTime().getTime() == FileTools
                            .getFileCreateTime(new File(fi.getPath()))
                            && fi.getSize() == f.length())
                    {
                        if (excludeDirs != null)
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
                    else
                    {
                        if (fi.isDel())
                        {
                            return false;
                        }
                    }

                    /*
                     * String tmpstr = fi.getPath().toLowerCase(); if
                     * (tmpstr.endsWith("jpg") || tmpstr.endsWith("jpeg")) {
                     * final int angel = ReadEXIF.needRotateAngel(fi.getPath());
                     * if (angel != 0) { threadPool.submit(new Runnable() {
                     * 
                     * @Override public void run() { rotateOneFile(fi, angel); }
                     * }); return true; } }
                     */
                }
            }
            else
            {
                if (fi.isDel())
                {
                    return false;
                }
            }
        }
        return true;
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
                    new String[] { tmpFilePath, fi.getPath(), sf.format(fi.getPhotoTime()) });

        }
        catch (Exception e)
        {
            logger.error("caught: ", e);
        }
    }

    public static long getFileCreateTime(File f) throws IOException
    {
        if (f == null)
        {
            return -1;
        }

        Path path = FileSystems.getDefault().getPath(f.getParent(), f.getName());
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        return attrs.creationTime().toMillis();
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

            BufferedImage res = new BufferedImage(rect_des.width, rect_des.height,
                    BufferedImage.TYPE_INT_RGB);
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

            int bytes = -1;
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
        if (!GloableLockBaseOnString.getInstance().tryToDo(fi.getHash256()))
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
                    GloableLockBaseOnString.getInstance().done(fi.getHash256());
                }
            }
        });
    }

    public static FileType getFileType(String filePath)
    {
        filePath = filePath.toLowerCase();
        FileType contentType;

        if (filePath.endsWith(".mp4"))
        {
            contentType = FileType.MP4;
        }
        else if (filePath.endsWith(".png"))
        {
            contentType = FileType.PNG;
        }
        else if (filePath.endsWith(".jpeg"))
        {
            contentType = FileType.JPEG;
        }
        else if (filePath.endsWith(".jpg"))
        {
            contentType = FileType.JPG;
        }
        else
        {
            contentType = FileType.JPG;
        }

        return contentType;
    }
}

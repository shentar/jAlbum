package com.backend;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTools
{
    private static final Logger logger = LoggerFactory.getLogger(FileTools.class);
    public static final String[] filesufixs = { ".jpg", ".jpeg", ".png" };
    public static final String[] picsuffixs = { ".jpg", ".jpeg", ".png" };
    public static String inputdir = System.getProperty("inputdir", "/");
    public static boolean usesqlite = "true".equals(System.getProperty("usesqlite", "true")) ? true : false;
    public static long lastScanTime = 0;
    public static int minfilesize = 50 * 1024;
    public static int threadcount = Integer.getInteger("threadcount", 20);
    public static String hashalog = System.getProperty("hashalog", "SHA-256");
    public static final ExecutorService threadPool = Executors.newFixedThreadPool(threadcount);

    public static void readConfig()
    {
        if (FileTools.inputdir == null || FileTools.inputdir.isEmpty())
        {
            logger.error("input dir is empty.");
            return;
        }

    }

    public static void readShortFileContent(byte[] buffer, File f) throws FileNotFoundException, IOException
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

    public static boolean checkFileDeleted(final FileInfo fi) throws IOException
    {
        if (fi != null)
        {
            File f = new File(fi.getPath());
            if (f.isFile() && f.length() > FileTools.minfilesize)
            {
                if (fi.getcTime().getTime() == FileTools.getFileCreateTime(new File(fi.getPath())))
                {
                    return false;
                }

                /*
                 * String tmpstr = fi.getPath().toLowerCase(); if
                 * (tmpstr.endsWith("jpg") || tmpstr.endsWith("jpeg")) { final
                 * int angel = ReadEXIF.needRotateAngel(fi.getPath()); if (angel
                 * != 0) { threadPool.submit(new Runnable() {
                 * 
                 * @Override public void run() { rotateOneFile(fi, angel); } });
                 * return true; } }
                 */
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
            ExifCreator.addExifDate(new String[] { tmpFilePath, fi.getPath(), sf.format(fi.getPhotoTime()) });

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
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
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

            BufferedImage res = new BufferedImage(rect_des.width, rect_des.height, BufferedImage.TYPE_INT_RGB);
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
}

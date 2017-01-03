package com.utils.media;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.backend.scan.FileTools;
import com.utils.sys.SystemConstant;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegBuilder.Verbosity;

public class ThumbnailGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(ThumbnailGenerator.class);

    public static BufferedImage generateThumbnail(String fpath, int w, int h, boolean force)
            throws IOException
    {
        File imgFile = new File(fpath);
        checkPicType(imgFile);

        logger.debug("target image[{}]'s size, width:{}, height:{}.", fpath, w, h);
        Image img = ImageIO.read(imgFile);
        if (img == null)
        {
            return null;
        }
        if (!force)
        {
            // 根据原图与要求的缩略图比例，找到最合适的缩略图比例
            int width = img.getWidth(null);
            int height = img.getHeight(null);

            if (w > width || h > height)
            {
                // 照片本身比缩略图还小，则直接以照片本身当缩略图。
                return null;
            }

            if ((width * 1.0) / w > (height * 1.0) / h)
            {
                h = Integer.parseInt(
                        new java.text.DecimalFormat("0").format(height * w / (width * 1.0)));

            }
            else
            {
                w = Integer.parseInt(
                        new java.text.DecimalFormat("0").format(width * h / (height * 1.0)));
            }
        }

        logger.info("the fixed width and height are:[{}], {}, {}", fpath, w, h);

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.getGraphics();
        g.drawImage(img, 0, 0, w, h, Color.LIGHT_GRAY, null);
        g.dispose();
        return bi;
    }

    private static String checkPicType(File imgFile) throws IOException
    {
        // ImageIO 支持的图片类型 : [BMP, bmp, jpg, JPG, wbmp, jpeg, png, PNG,
        // JPEG, WBMP, GIF, gif]
        String types = Arrays.toString(ImageIO.getReaderFormatNames());
        String suffix = null;
        // 获取图片后缀
        if (imgFile.getName().indexOf(".") > -1)
        {
            suffix = imgFile.getName().substring(imgFile.getName().lastIndexOf(".") + 1);
        }

        if (suffix == null || types.toLowerCase().indexOf(suffix.toLowerCase()) < 0)
        {
            logger.error(
                    "Sorry, the image suffix is illegal. the standard image suffix is {}." + types);
            throw new IOException("the file type is not supported!");
        }

        return suffix;
    }

    public static InputStream generateThumbnailInputStream(String fpath, int w, int h,
            boolean force)
    {
        try
        {
            String suffix = checkPicType(new File(fpath));
            BufferedImage bi = generateThumbnail(fpath, w, h, force);
            ByteArrayOutputStream bf = new ByteArrayOutputStream();

            ImageIO.write(bi, suffix, bf);
            byte[] b = bf.toByteArray();
            ByteArrayInputStream bfin = new ByteArrayInputStream(b);
            return bfin;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }

        return null;
    }

    public static boolean createThumbnail(FileInfo fi, String thumbnailPath, int w, int h,
            boolean force)
    {
        String filePath = fi.getPath();
        if (MediaTool.isVideo(filePath))
        {
            return generateThumbnailForVideo(fi, thumbnailPath, w, h, force);
        }
        else
        {
            return generateThumbnailForPic(filePath, thumbnailPath, w, h, force);
        }
    }

    private static boolean generateThumbnailForVideo(FileInfo fi, String thumbnailPath, int w,
            int h, boolean force)
    {
        try
        {
            FFmpegBuilder builder = new FFmpegBuilder();
            builder.setInput(fi.getPath());
            builder.setStartOffset(0, TimeUnit.SECONDS);
            builder.setVerbosity(Verbosity.QUIET);

            if (!force)
            {
                long width = fi.getWidth();
                long height = fi.getHeight();
                if (!(fi.getRoatateDegree() % 180 == 0))
                {
                    width = fi.getHeight();
                    height = fi.getWidth();
                }

                if (w < fi.getWidth() || h < fi.getHeight())
                {
                    if ((width * 1.0) / w > (height * 1.0) / h)
                    {
                        h = Integer.parseInt(new java.text.DecimalFormat("0")
                                .format(height * w / (width * 1.0)));

                    }
                    else
                    {
                        w = Integer.parseInt(new java.text.DecimalFormat("0")
                                .format(width * h / (height * 1.0)));
                    }
                }
            }

            FFmpegBuilder target = builder.addOutput(thumbnailPath).setFormat("image2")
                    .setVideoResolution(w, h).setFrames(1).done();
            // builder.set
            FFmpeg ffmpeg = new FFmpeg();
            ffmpeg.run(target, null);
            return true;
        }
        catch (Exception e)
        {
            logger.warn("error occured.", e);
        }

        if (!FileTools.copyFile(SystemConstant.DEFAULT_VIDEO_PIC_PATH, thumbnailPath))
        {
            return true;
        }

        return false;
    }

    private static boolean generateThumbnailForPic(String fpath, String thumbnailPath, int w, int h,
            boolean force)
    {
        try
        {
            String suffix = checkPicType(new File(fpath));
            BufferedImage bi = generateThumbnail(fpath, w, h, force);
            if (bi == null)
            {
                throw new IOException();
            }

            ImageIO.write(bi, suffix, new File(thumbnailPath));
            return true;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }

        if (!FileTools.copyFile(fpath, thumbnailPath))
        {
            return true;
        }

        return false;
    }
}

package com.utils.media;

import com.backend.entity.FileInfo;
import com.backend.scan.FileTools;
import com.utils.sys.SystemConstant;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegBuilder.Verbosity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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
        if (imgFile.getName().contains("."))
        {
            suffix = imgFile.getName().substring(imgFile.getName().lastIndexOf(".") + 1);
        }

        if (suffix == null || !types.toLowerCase().contains(suffix.toLowerCase()))
        {
            logger.error(
                    "Sorry, the image suffix is illegal. the standard image suffix is {}." + types);
            throw new IOException("the file type is not supported!");
        }

        return suffix;
    }

    public static byte[] generateThumbnailBuffer(String fpath, int w, int h, boolean force)
    {
        try
        {
            String suffix = checkPicType(new File(fpath));
            BufferedImage bi = generateThumbnail(fpath, w, h, force);
            if (bi == null)
            {
                return null;
            }
            ByteArrayOutputStream bf = new ByteArrayOutputStream();

            ImageIO.write(bi, suffix, bf);
            return bf.toByteArray();
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

            FFmpegBuilder target =
                    builder.addOutput(thumbnailPath).setFormat("image2").setVideoResolution(w, h)
                            .setFrames(1).done();
            // builder.set
            FFmpeg ffmpeg = new FFmpeg();
            ffmpeg.run(target, null);
            return true;
        }
        catch (Exception e)
        {
            logger.warn("error occured.", e);
        }

        if (FileTools.copyFile(SystemConstant.DEFAULT_VIDEO_PIC_PATH, thumbnailPath))
        {
            logger.warn("the file cannot gen thumbnail copy the src file instead. {}", fi);
            return true;
        }

        logger.warn("gen the thumbnail failed, {}", fi);
        return false;
    }

    private static boolean generateThumbnailForPic(String fpath, String thumbnailPath, int w, int h,
                                                   boolean force)
    {
        try
        {
            String suffix = checkPicType(new File(fpath));
            BufferedImage bi = generateThumbnail(fpath, w, h, force);
            if (bi != null)
            {
                ImageIO.write(bi, suffix, new File(thumbnailPath));
                return true;
            }
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }

        if (FileTools.copyFile(fpath, thumbnailPath))
        {
            logger.warn("the file cannot gen thumbnail copy the src file instead. {}", fpath);
            return true;
        }

        logger.warn("gen the thumbnail failed, {}", fpath);
        return false;
    }

    public static boolean createFaceThumbnail(Object origFile, String suffix, String pos,
                                              String tmpFile)
    {
        if (StringUtils.isBlank(pos) || StringUtils.isBlank(suffix) || origFile == null
                || StringUtils.isBlank(tmpFile))
        {
            logger.warn("input arguments error!");
            return false;
        }

        ImageInputStream iis = null;
        try
        {
            if (origFile instanceof byte[])
            {
                iis = ImageIO.createImageInputStream(new ByteArrayInputStream((byte[]) origFile));
            }
            else if (origFile instanceof File || origFile instanceof InputStream)
            {
                iis = ImageIO.createImageInputStream(origFile);
            }
            else if (origFile instanceof String)
            {
                iis = ImageIO.createImageInputStream(new File((String) origFile));
            }
            else
            {
                logger.warn("unknown type of input file: {}", origFile);
                return false;
            }

            /**
             * width,height,left,top
             */
            String[] ss = pos.split(",");
            if (ss.length != 4)
            {
                logger.warn("the file info is: {}, {}", origFile, pos);
                return false;
            }

            int w = Integer.parseInt(ss[0]);
            int h = Integer.parseInt(ss[1]);
            int x = Integer.parseInt(ss[2]);
            int y = Integer.parseInt(ss[3]);

            if (x < 0)
            {
                logger.warn("the file info is: {}, {}", origFile, pos);
                w += x;
                x = 0;
            }

            if (y < 0)
            {
                logger.warn("the file info is: {}, {}", origFile, pos);
                h += y;
                y = 0;
            }

            if (w < 0 || h < 0)
            {
                logger.warn("the file info is: {}, {}", origFile, pos);
                return false;
            }

            Rectangle rect = new Rectangle(x, y, w, h);
            Rectangle newRect = getNewRectangle(rect);
            if (!genFaceThumbnail(suffix, tmpFile, iis, newRect == null ? rect : newRect))
            {
                logger.warn("generate the face thumbnail failed: {}", origFile);
                return copyTheOrigFile(origFile, tmpFile);
            }

            return true;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        finally
        {
            if (iis != null)
            {
                try
                {
                    iis.close();
                }
                catch (IOException e)
                {
                    logger.warn("caused by: ", e);
                }
            }
        }

        return false;
    }

    private static boolean copyTheOrigFile(Object origFile, String tmpFile) throws IOException
    {
        if (origFile instanceof byte[])
        {
            FileUtils.writeByteArrayToFile(new File(tmpFile), (byte[]) origFile);
        }
        else if (origFile instanceof File)
        {
            FileUtils.copyFile((File) origFile, new File(tmpFile));
        }
        else if (origFile instanceof InputStream)
        {
            FileUtils.copyInputStreamToFile((InputStream) origFile, new File(tmpFile));
        }
        else if (origFile instanceof String)
        {
            FileUtils.copyFile(new File((String) origFile), new File(tmpFile));
        }
        else
        {
            logger.warn("unknown type of input file: {}", origFile);
            return false;
        }
        return true;
    }

    private static boolean genFaceThumbnail(String suffix, String tmpFile, ImageInputStream iis,
                                            Rectangle newRect)
    {
        try
        {
            ImageReader reader = ImageIO.getImageReadersBySuffix(suffix).next();
            reader.setInput(iis, true);
            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(newRect);
            BufferedImage bi = reader.read(0, param);
            ImageIO.write(bi, suffix, new File(tmpFile));
            return true;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        return false;
    }

    private static Rectangle getNewRectangle(Rectangle rect)
    {
        int addInterval = (int) (rect.getHeight() * 0.3);
        if (rect.getX() < addInterval || rect.getY() < addInterval)
        {
            return null;
        }

        return new Rectangle((int) (rect.getX() - addInterval), (int) (rect.getY() - addInterval),
                             (int) (rect.getWidth() + 2 * addInterval),
                             (int) (rect.getHeight() + 2 * addInterval));
    }
}

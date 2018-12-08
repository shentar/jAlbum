package com.utils.media;

import com.backend.entity.FileInfo;
import com.backend.entity.PicStatus;
import com.backend.scan.FileTools;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.utils.web.HeadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ReadEXIF
{
    private static final Logger logger = LoggerFactory.getLogger(ReadEXIF.class);

    public static FileInfo genAllInfos(String filePath, boolean needExif)
    {
        if (filePath == null || filePath.isEmpty())
        {
            logger.error("input file path is empty.");
            return null;
        }
        FileInfo fi = null;
        File f = new File(filePath);
        if (!f.exists())
        {
            logger.error("the file is not exist: " + filePath);
            return null;
        }

        try
        {
            fi = new FileInfo();
            fi.setFtype(HeadUtils.getFileType(filePath));
            fi.setPath(f.getCanonicalPath());
            fi.setSize(f.length());
            fi.setStatus(PicStatus.EXIST);
            fi.setcTime(new java.sql.Date(FileTools.getFileCreateTime(new File(fi.getPath()))));
            if (needExif)
            {
                try
                {
                    Metadata metadata = ImageMetadataReader.readMetadata(f);
                    getInfo(metadata, fi);
                }
                catch (Exception e)
                {
                    logger.warn("caused by: ", e);
                    fi.setPhotoTime(new java.sql.Date(fi.getcTime().getTime()));
                    return fi;
                }
            }

            logger.debug("the file is: {}", fi);
            return fi;
        }
        catch (Throwable e)
        {
            logger.error("error file: " + f, e);
        }

        return fi;
    }

    private static void getInfo(Metadata metadata, FileInfo fi)
            throws MetadataException, IOException
    {
        Iterable<Directory> dirs = metadata.getDirectories();
        boolean getheightandweight = false;
        ExifIFD0Directory dirFD0 = null;
        ExifSubIFDDirectory dirSub = null;
        for (Directory dir : dirs)
        {
            if (dir != null)
            {
                if (dir instanceof ExifDirectoryBase)
                {
                    if (dir instanceof ExifIFD0Directory)
                    {
                        dirFD0 = (ExifIFD0Directory) dir;
                    }
                    else if (dir instanceof ExifSubIFDDirectory)
                    {
                        dirSub = (ExifSubIFDDirectory) dir;
                    }

                    if (!getheightandweight)
                    {
                        if (dir.containsTag(ExifDirectoryBase.TAG_IMAGE_HEIGHT))
                        {
                            fi.setHeight(dir.getInt(ExifDirectoryBase.TAG_IMAGE_HEIGHT));
                            getheightandweight = true;
                        }

                        if (dir.containsTag(ExifDirectoryBase.TAG_IMAGE_WIDTH))
                        {
                            fi.setWidth(dir.getInt(ExifDirectoryBase.TAG_IMAGE_WIDTH));
                            getheightandweight = true;
                        }
                    }
                }

                if (!getheightandweight)
                {
                    if (dir instanceof JpegDirectory)
                    {
                        if (dir.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT))
                        {
                            fi.setHeight(dir.getInt(JpegDirectory.TAG_IMAGE_HEIGHT));
                            getheightandweight = true;
                        }

                        if (dir.containsTag(JpegDirectory.TAG_IMAGE_WIDTH))
                        {
                            fi.setWidth(dir.getInt(JpegDirectory.TAG_IMAGE_WIDTH));
                            getheightandweight = true;
                        }
                    }

                    if (dir instanceof PngDirectory)
                    {
                        if (dir.containsTag(PngDirectory.TAG_IMAGE_HEIGHT))
                        {
                            fi.setHeight(dir.getInt(PngDirectory.TAG_IMAGE_HEIGHT));
                            getheightandweight = true;
                        }

                        if (dir.containsTag(PngDirectory.TAG_IMAGE_WIDTH))
                        {
                            fi.setWidth(dir.getInt(PngDirectory.TAG_IMAGE_WIDTH));
                            getheightandweight = true;
                        }
                    }
                }
            }
            if (dirFD0 != null && dirSub != null && getheightandweight)
            {
                break;
            }
        }

        if (!getheightandweight)
        {
            BufferedImage bfi = ImageIO.read(new File(fi.getPath()));
            fi.setHeight(bfi.getHeight());
            fi.setWidth(bfi.getWidth());
        }

        Date d = null;
        if (dirSub != null)
        {
            d = dirSub.getDate(ExifDirectoryBase.TAG_DATETIME_ORIGINAL);
            if (d == null || d.getTime() < 0)
            {
                d = dirSub.getDate(ExifDirectoryBase.TAG_DATETIME_DIGITIZED);
            }
        }

        if (d == null)
        {
            if (dirFD0 != null)
            {
                d = dirFD0.getDate(ExifDirectoryBase.TAG_DATETIME);
            }
        }

        if (d == null || d.getTime() < 0)
        {
            fi.setPhotoTime(fi.getcTime());
            logger.info("the ptime is null use the ctime: {}", fi);
        }
        else if (d.getTime() > fi.getcTime().getTime())
        {
            if (fi.getcTime().getTime() > System.currentTimeMillis())
            {
                logger.warn(
                        "the ptime is newer than ctime, "
                                + "and ctime is newer than current time, now use the ctime instead of ptime: {}",
                        fi);
                fi.setPhotoTime(new java.sql.Date(System.currentTimeMillis()));
            }
            else
            {
                logger.warn("the ptime is newer than ctime, now use the ctime instead of ptime: {}",
                        fi);
                fi.setPhotoTime(fi.getcTime());
            }
        }
        else
        {
            fi.setPhotoTime(new java.sql.Date(d.getTime()));
        }

        fi.setRoatateDegree(needRotateAngel(metadata));
    }

    public static int needRotateAngel(Metadata metadata)
    {
        try
        {
            if (metadata == null)
            {
                return 0;
            }

            Iterable<Directory> dirs = metadata.getDirectories();
            for (Directory dir : dirs)
            {
                if (dir instanceof ExifDirectoryBase
                        && dir.containsTag(ExifDirectoryBase.TAG_ORIENTATION))
                {
                    int angel = dir.getInt(ExifDirectoryBase.TAG_ORIENTATION);
                    if (angel == 3)
                    {
                        return 180;
                    }

                    if (angel == 6)
                    {
                        return 90;
                    }

                    if (angel == 8)
                    {
                        return 270;
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("caused by: ", e);
        }

        return 0;
    }

    public static int needRotateAngel(String path)
    {
        try
        {
            return needRotateAngel(ImageMetadataReader.readMetadata(new File(path)));
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }

        return 0;
    }
}

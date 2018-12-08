package com.utils.media;

import mediautil.gen.directio.SplitInputStream;
import mediautil.image.ImageResources;
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.Entry;
import mediautil.image.jpeg.Exif;
import mediautil.image.jpeg.LLJTran;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;

public class ExifCreator
{

    private static final Logger logger = LoggerFactory.getLogger(ExifCreator.class);

    public static void addExifDate(String[] args) throws Exception
    {
        if (args.length != 3)
        {
            logger.warn("Usage: java LLJTranTutorial <inputFile> <outputFile>");
            return;
        }

        // 1. Read the Image from inputFile upto READ_HEADER along with the
        // ImageReader using SplitInputStream and Generate a Thumbnail from
        // the Image.
        InputStream fip = new FileInputStream(args[0]); // No need to buffer
        SplitInputStream sip = new SplitInputStream(fip);
        // Create a substream for LLJTran to use
        InputStream subIp = sip.createSubStream();
        LLJTran llj = new LLJTran(subIp);
        // Normally it would be better to read the entire image when reading
        // shared. But LLJTran only needs to read upto header for loading
        // imageInfo and using xferInfo.
        llj.initRead(LLJTran.READ_HEADER, true, true);
        sip.attachSubReader(llj, subIp);
        // LLJTran reads the image when the below API reads from sip via
        // nextRead() calls made by sip.
        byte newThumbnail[] = getThumbnailImage(sip);
        sip.wrapup();
        fip.close();

        // Check llj for errors
        String msg = llj.getErrorMsg();
        if (msg != null)
        {
            logger.warn("Error in LLJTran While Loading Image: " + msg);
            Exception e = llj.getException();
            if (e != null)
            {
                logger.warn("Got an Exception, throwing it..");
                throw e;
            }
            System.exit(1);
        }

        // 2. If the image has a Thumbnail (Which means it has a Exif Header)
        // print a message and exit.
        @SuppressWarnings("rawtypes")
        AbstractImageInfo imageInfo = llj.getImageInfo();
        if (imageInfo.getThumbnailLength() > 0)
        {
            logger.info("Image already has a Thumbnail. Exitting.." + args[0]);
            return;
            // System.exit(1);
        }

        // 3. If the Image does not have an Exif Header create a dummy Exif
        // Header
        if (!(imageInfo instanceof Exif))
        {
            logger.info("Adding a Dummy Exif Header");
            llj.addAppx(LLJTran.dummyExifHeader, 0, LLJTran.dummyExifHeader.length, true);
            imageInfo = llj.getImageInfo(); // This would have changed

            Exif exif = (Exif) imageInfo;

            // Changed Date/Time and dimensions in Dummy Exif
            Entry entry = exif.getTagValue(Exif.DATETIME, true);
            if (entry != null)
                entry.setValue(0, args[2]);
            entry = exif.getTagValue(Exif.DATETIMEORIGINAL, true);
            if (entry != null)
                entry.setValue(0, args[2]);
            entry = exif.getTagValue(Exif.DATETIMEDIGITIZED, true);
            if (entry != null)
                entry.setValue(0, args[2]);

            int imageWidth = llj.getWidth();
            int imageHeight = llj.getHeight();
            if (imageWidth > 0 && imageHeight > 0)
            {
                entry = exif.getTagValue(Exif.EXIFIMAGEWIDTH, true);
                if (entry != null)
                    entry.setValue(0, imageWidth);
                entry = exif.getTagValue(Exif.EXIFIMAGELENGTH, true);
                if (entry != null)
                    entry.setValue(0, imageHeight);
            }
        }

        // 4. Set the new Thumbnail
        if (llj.setThumbnail(newThumbnail, 0, newThumbnail.length, ImageResources.EXT_JPG))
            logger.info("Successfully Set New Thumbnail");
        else
            logger.info("Error Setting New Thumbnail");

        // 5. Transfer the image from inputFile to outputFile replacing the new
        // Exif with the Thumbnail so that outputFile has a Thumbnail.
        fip = new BufferedInputStream(new FileInputStream(args[0]));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(args[1]));
        // Replace the new Exif Header in llj while copying the image from fip
        // to out
        llj.xferInfo(fip, out, LLJTran.REPLACE, LLJTran.RETAIN);
        fip.close();
        out.close();

        // Cleanup
        llj.freeMemory();
    }

    /**
     * Utility Method to get a Thumbnail Image in a byte array from an
     * InputStream to a full size image. The full size image is read and scaled
     * to a Thumbnail size using Java API.
     */
    private static byte[] getThumbnailImage(InputStream ip) throws IOException
    {
        ImageReader reader;
        ImageInputStream iis = ImageIO.createImageInputStream(ip);
        reader = ImageIO.getImageReaders(iis).next();
        reader.setInput(iis);
        BufferedImage image = reader.read(0);
        iis.close();

        // Scale the image to around 160x120/120x160 pixels, may not conform
        // exactly to Thumbnail requirements of 160x120.
        int t, longer, shorter;
        longer = image.getWidth();
        shorter = image.getHeight();
        if (shorter > longer)
        {
            t = longer;
            longer = shorter;
            shorter = t;
        }
        double factor = 160 / (double) longer;
        double factor1 = 120 / (double) shorter;
        if (factor1 > factor)
            factor = factor1;
        AffineTransform tx = new AffineTransform();
        tx.scale(factor, factor);
        AffineTransformOp affineOp = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        image = affineOp.filter(image, null);

        // Write Out the Scaled Image to a ByteArrayOutputStream and return the
        // bytes
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(2048);
        String format = "JPG";
        ImageIO.write(image, format, byteStream);

        return byteStream.toByteArray();
    }
}

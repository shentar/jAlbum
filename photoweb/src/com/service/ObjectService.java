package com.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.BaseSqliteStore;
import com.backend.FileInfo;
import com.backend.FileTools;
import com.backend.ThumbnailManager;
import com.backend.UniqPhotosStore;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;

public class ObjectService
{
    private static final Logger logger = LoggerFactory.getLogger(ObjectService.class);

    private String id;

    public ObjectService(String id)
    {
        this.setId(id);
    }

    @GET
    public Response getPhotoData(@Context HttpServletRequest req,
            @Context HttpServletResponse response) throws IOException
    {
        ResponseBuilder builder = Response.status(200);
        BaseSqliteStore meta = BaseSqliteStore.getInstance();
        FileInfo f = meta.getOneFileByHashID(id);
        if ("true".equalsIgnoreCase(req.getParameter("content")))
        {
            if (f != null && new File(f.getPath()).isFile())
            {
                String sizestr = req.getParameter("size");
                int size = Integer.parseInt(sizestr);
                InputStream fi = null;
                if (size <= 400)
                {
                    fi = ThumbnailManager.getThumbnail(id);
                    if (fi == null)
                    {
                        FileTools.submitAnThumbnailTask(f);
                    }
                }

                if (fi == null)
                {
                    fi = new BufferedInputStream(new FileInputStream(new File(f.getPath())));
                }

                if (fi != null)
                {
                    builder.entity(fi);
                    String contenttype = getContentType(f.getPath());
                    builder.header("Content-type", contenttype);
                    HeadUtils.setExpiredTime(builder);
                    builder.header("Content-Disposition",
                            "filename=" + new File(f.getPath()).getName());
                    builder.header("PicFileFullPath", URLEncoder.encode(f.getPath(), "UTF-8"));
                    logger.info("the file is: {}, Mime: {}", f, contenttype);
                }
            }
            else
            {
                builder.status(404);
            }
        }
        else
        {
            String bodyContent = GenerateHTML.generateSinglePhoto(f);
            builder.header("Content-type", "text/html");
            builder.entity(bodyContent);
            logger.info("the page is {}", bodyContent);
        }

        return builder.build();
    }

    @DELETE
    public Response deletePhotoData(@Context HttpServletRequest req,
            @Context HttpServletResponse response) throws IOException
    {
        logger.warn("try to delete the photo: " + id);
        ResponseBuilder builder = Response.status(204);

        BaseSqliteStore meta = BaseSqliteStore.getInstance();
        meta.setPhotoToBeHidenByID(id);

        UniqPhotosStore umeta = UniqPhotosStore.getInstance();
        // List<FileInfo> fnext = umeta.getNextNineFileByHashStr(id, 1);
        umeta.deleteRecordByID(id);

        /*
         * if (fnext!= null && !fnext.isEmpty()) { // 刷新整个页面。 String bodyContent
         * = GenerateHTML.generateSinglePhoto(fnext.get(0));
         * builder.header("Content-type", "text/html");
         * builder.entity(bodyContent); logger.info("the page is {}",
         * bodyContent); }
         */
        logger.warn("deleted the photo: " + id);
        return builder.build();
    }

    private static String getContentType(String pathToFile) throws IOException
    {
        return Files.probeContentType(Paths.get(pathToFile));
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}

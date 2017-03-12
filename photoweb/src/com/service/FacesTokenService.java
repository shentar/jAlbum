package com.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.backend.dao.FaceTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.facer.Face;
import com.utils.media.ThumbnailManager;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;

public class FacesTokenService
{
    private static final Logger logger = LoggerFactory.getLogger(FacesTokenService.class);

    private String id = null;

    public FacesTokenService(String id)
    {
        SystemProperties.add(SystemConstant.IS_FACES_KEY, true);
        this.id = id;
    }

    @GET
    public Response getFaceContent(@Context HttpServletRequest req,
            @Context HttpServletResponse response) throws IOException
    {
        ResponseBuilder builder = Response.status(200);

        if (req.getParameter("facethumbnail") != null)
        {
            File faceThumbnail = ThumbnailManager.getFaceThumbnail(id);
            InputStream is = null;
            if (faceThumbnail.exists() && faceThumbnail.isFile())
            {
                is = new BufferedInputStream(new FileInputStream(faceThumbnail));
                builder.header("Content-type",
                        HeadUtils.getContentType(faceThumbnail.getCanonicalPath()));
                logger.info("use the thumbnail: {}", faceThumbnail);
            }
            else
            {
                Face face = FaceTableDao.getInstance().getFace(id);
                if (face != null)
                {
                    FileInfo fi = UniqPhotosStore.getInstance().getOneFileByHashStr(face.getEtag());
                    is = new BufferedInputStream(new FileInputStream(fi.getPath()));
                    builder.header("Content-type", HeadUtils.getContentType(fi.getPath()));
                    logger.info("use the face file: {}", face);
                    ThumbnailManager.checkAndGenFaceThumbnail(face);
                }
            }

            if (is != null)
            {
                MDC.put(SystemConstant.FILE_NAME, id);
                HeadUtils.setExpiredTime(builder);
                builder.header("Content-Disposition", "filename=" + id);
                builder.entity(is);
            }
            else
            {
                logger.warn("the special facetoken id is not exist {}", id);
                builder.entity(GenerateHTML.generate404Notfound());
                builder.status(404);
            }
        }
        else
        {
            Face face = FaceTableDao.getInstance().getFace(id);
            FileInfo fi = null;
            if (face != null)
            {
                fi = UniqPhotosStore.getInstance().getOneFileByHashStr(face.getEtag());
                logger.info("use the face file: {}", face);
            }

            if (fi == null)
            {
                logger.warn("the special facetoken id is not exist {}", id);
                builder.entity(GenerateHTML.generate404Notfound());
                builder.status(404);
            }
            else
            {
                builder.entity(GenerateHTML.generateSinglePhoto(face));
                logger.info("generate the single face photo file successfully {}", face);
            }
        }

        return builder.build();
    }
}

package com.service;

import com.backend.dao.FaceTableDao;
import com.backend.entity.FileInfo;
import com.backend.facer.Face;
import com.utils.media.ThumbnailManager;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.*;

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
            if (faceThumbnail != null && faceThumbnail.exists() && faceThumbnail.isFile())
            {
                is = new BufferedInputStream(new FileInputStream(faceThumbnail));
                builder.header("Content-type",
                               HeadUtils.getContentType(faceThumbnail.getCanonicalPath()));
                logger.info("use the thumbnail: {}", faceThumbnail);
            }
            else
            {
                Face face = FaceTableDao.getInstance().getFace(id);
                if (face != null && face.getFi() != null)
                {
                    is = new BufferedInputStream(new FileInputStream(face.getFi().getPath()));
                    builder.header("Content-type",
                                   HeadUtils.getContentType(face.getFi().getPath()));
                    logger.info("use the face file: {}", face);
                    // performance
                    // ThumbnailManager.checkAndGenFaceThumbnail(face);
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
            if (face != null && face.getFi() != null)
            {
                fi = face.getFi();
                logger.info("use the face file: {}", face);
            }

            if (face == null || fi == null)
            {
                logger.warn("the special facetoken id is not exist {}", id);
                builder.entity(GenerateHTML.generate404Notfound());
                builder.status(404);
            }
            else
            {
                face.setFi(fi);
                builder.entity(GenerateHTML.generateSinglePhoto(face));
                logger.info("generate the single face photo file successfully {}", face);
            }
        }

        return builder.build();
    }
}

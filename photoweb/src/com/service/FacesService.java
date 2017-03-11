package com.service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.backend.dao.UniqPhotosStore;
import com.backend.facer.Face;
import com.backend.facer.FaceRecService;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;

public class FacesService
{
    private static final Logger logger = LoggerFactory.getLogger(FacesService.class);

    private long id = -1;

    public FacesService(long faceid)
    {
        this.id = faceid;
    }

    @GET
    public Response getFaces(@Context HttpServletRequest req, @Context HttpServletResponse response)
            throws IOException
    {
        SystemProperties.add(SystemConstant.IS_FACES_KEY, true);

        ResponseBuilder b = Response.status(200);

        if (id < 0)
        {
            return b.entity(GenerateHTML.generateFacesIndex(
                    FaceRecService.getInstance().checkAndGetFaceidList(), HeadUtils.isMobile() ? 3 : 5))
                    .build();
        }
        else
        {
            List<Face> flst = FaceRecService.getInstance().getSortedFaces(id);

            if (flst != null)
            {
                List<FileInfo> fflist = new LinkedList<FileInfo>();
                for (Face f : flst)
                {
                    FileInfo fi = UniqPhotosStore.getInstance().getOneFileByHashStr(f.getEtag());
                    if (fi != null)
                    {
                        fflist.add(fi);
                    }
                }

                logger.info("the face count is: {}", fflist.size());
                return b.entity(GenerateHTML.generateFacesForOneFaceID(fflist)).build();
            }
        }

        return b.entity(GenerateHTML.generate404Notfound()).status(404).build();
    }
}

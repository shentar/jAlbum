package com.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;

import com.backend.dao.FaceTableDao;
import com.backend.facer.Face;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import com.backend.facer.FaceRecService;
import com.utils.conf.AppConfig;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;

public class FacesService
{
    // private static final Logger logger =
    // LoggerFactory.getLogger(FacesService.class);

    private long id = -1;

    public FacesService(long faceid)
    {
        SystemProperties.add(SystemConstant.IS_FACES_KEY, true);
        this.id = faceid;
    }

    @GET
    public Response getFaces(@Context HttpServletRequest req, @Context HttpServletResponse response)
            throws IOException
    {
        ResponseBuilder b = Response.status(200);

        if (id < 0)
        {
            return b.entity(GenerateHTML.genFaceIndexPage(
                    FaceRecService.getInstance().checkAndGetFaceidList(), getRowCount())).build();
        }
        else
        {
            String faceToken = req.getParameter("facetoken");
            List<Face> flst = null;
            if (StringUtils.isNotBlank(faceToken))
            {
                Face f = FaceTableDao.getInstance().getFace(faceToken);
                if (f != null)
                {
                    flst = FaceTableDao.getInstance().getNextNineFileByHashStr(faceToken,
                            AppConfig.getInstance().getMaxCountOfPicInOnePage(25) - 1, true);
                    flst.add(0, f);
                }
            }
            else
            {
                flst = FaceRecService.getInstance().getSortedFaces(id,
                        AppConfig.getInstance().getMaxCountOfPicInOnePage(25), true);
            }

            return b.entity(GenerateHTML.genIndexPage(flst, getRowCount(), true)).build();
        }
    }

    private int getRowCount()
    {
        return (HeadUtils.isVideo() || HeadUtils.isMobile()) ? 3 : 5;
    }
}

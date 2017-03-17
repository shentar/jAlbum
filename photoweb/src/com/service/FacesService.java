package com.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import com.backend.facer.FaceRecService;
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
            return b.entity(GenerateHTML.genIndexPage(
                    FaceRecService.getInstance().getSortedFaces(id), getRowCount(), false)).build();
        }
    }

    private int getRowCount()
    {
        return (HeadUtils.isVideo() || HeadUtils.isMobile()) ? 3 : 5;
    }
}

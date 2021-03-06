package com.service;

import com.backend.dao.FaceTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.facer.Face;
import com.backend.facer.FaceRecServiceFactory;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.util.List;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class FacesService {
    // private static final Logger logger =
    // LoggerFactory.getLogger(FacesService.class);
    private static final Logger logger = LoggerFactory.getLogger(FacesService.class);

    private long id = -1;

    public FacesService(long faceid) {
        SystemProperties.add(SystemConstant.IS_FACES_KEY, true);
        this.id = faceid;
    }

    @GET
    public Response getFaces(@Context HttpServletRequest req, @Context HttpServletResponse response)
            throws IOException {
        ResponseBuilder b = Response.status(200);

        if (id < 0) {
            return b.entity(GenerateHTML.genFaceIndexPage(
                    FaceRecServiceFactory.getFaceRecService().checkAndGetFaceidList(), getRowCount())).build();
        } else {
            // String faceToken = req.getParameter("facetoken");
            // List<Face> flst = null;
            // if (StringUtils.isNotBlank(faceToken))
            // {
            // flst = getFaceThumbnailList(faceToken,
            // HeadUtils.judgeCountPerOnePage(req));
            // }
            // else
            // {
            // flst = FaceRecService.getInstance().getSortedFaces(id,
            // HeadUtils.judgeCountPerOnePage(req), true);
            // }

            return b.entity(
                    GenerateHTML.genIndexPage(
                            FaceRecServiceFactory.getFaceRecService().getSortedFaces(id,
                                    HeadUtils.judgeCountPerOnePage(req), true),
                            getRowCount(), true, true))
                    .build();
        }
    }

    @DELETE
    public Response deleteFaces(@Context HttpServletRequest req,
                                @Context HttpServletResponse response) {
        ResponseBuilder b = Response.status(204);

        if (!(HeadUtils.isSuperLogin() || HeadUtils.isLocalLogin())) {
            b.status(403);
            logger.warn("not be permitted to delete data for common token logon");
            return b.build();
        }
        logger.warn("try to delete the faceid: " + id);
        if (id >= 0) {
            if (FaceRecServiceFactory.getFaceRecService().deleteOneFaceId(id)) {
                FaceTableDao.getInstance().deleteFacesByID(id);
            }
        }
        logger.warn("end to delete the faceid: " + id);
        return b.build();
    }

    @SuppressWarnings("unused")
    private List<Face> getFaceThumbnailList(String faceToken, int count) {
        List<Face> flst = null;
        Face f = FaceTableDao.getInstance().getFace(faceToken);
        if (f != null) {
            flst = FaceTableDao.getInstance().getNextNineFileByHashStr(faceToken, count - 1, true);

            if (flst != null && !flst.isEmpty()) {
                for (Face fa : flst) {
                    fa.setFi(UniqPhotosStore.getInstance().getOneFileByHashStr(fa.getEtag()));
                }
                flst.add(0, f);
            }
        }
        return flst;
    }

    private int getRowCount() {
        return (HeadUtils.isVideo() || HeadUtils.isMobile()) ? 3 : 5;
    }
}

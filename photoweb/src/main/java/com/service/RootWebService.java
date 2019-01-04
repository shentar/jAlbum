package com.service;

import com.backend.dao.BackupedFilesDao;
import com.backend.dao.BaseSqliteStore;
import com.backend.dao.FaceTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.facer.Face;
import com.backend.scan.BackendScanner;
import com.backend.sync.s3.S3ServiceFactory;
import com.backend.threadpool.ThreadPoolFactory;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.*;
import java.util.List;

@Produces(value = {"text/html", "application/octet-stream"})
public class RootWebService extends HttpServlet
{
    private static final Logger logger = LoggerFactory.getLogger(RootWebService.class);

    private static final long serialVersionUID = -7748065720779404006L;

    @GET
    @Path("/statistics")
    public Response statistics(@Context HttpServletRequest req, @Context HttpServletResponse res)
    {
        ResponseBuilder builder;

        if (!HeadUtils.isSuperLogin() && !HeadUtils.isLocalLogin())
        {
            builder = Response.status(403);
            builder.entity("Only Local login or Administrator login allowed to do this!");
        }
        else
        {
            long allfilecount =
                    BaseSqliteStore.getInstance().countTables(BaseSqliteStore.tableName);
            long allphotocount =
                    UniqPhotosStore.getInstance().countTables(UniqPhotosStore.tableName);
            long allvideocount = UniqPhotosStore.getInstance().getVideoCount();

            StringBuilder sb = new StringBuilder("<table border=\"0\">");
            sb.append("<tr><td>Files count:</td><td>").append(allfilecount).append("</td></tr>");
            sb.append("<tr><td>Unique Media files count:</td><td>").append(allphotocount)
                    .append("</td></tr>");
            sb.append("<tr><td>Video Count:</td><td>").append(allvideocount).append("</td></tr>");
            BackupedFilesDao bd = S3ServiceFactory.getBackUpDao();
            if (bd != null)
            {
                long allBackedCount = bd.countTables(bd.getBackupedTableName());
                sb.append("<tr><td>Backup Count:</td><td>").append(allBackedCount)
                        .append("</td></tr>");
            }

            sb.append("</table><br/><br/>");

            sb.append(ThreadPoolFactory.runningJobStatistics());

            builder = Response.ok(sb.toString());
        }

        return builder.build();
    }

    @GET
    @Path("/flushnow")
    public Response flushNow(@Context HttpServletRequest req, @Context HttpServletResponse res)
    {
        String message;
        ResponseBuilder builder;

        if (!HeadUtils.isSuperLogin() && !HeadUtils.isLocalLogin())
        {
            message = "Only Local login or Administrator login allowed to do this!";
            builder = Response.status(403);
            builder.entity(message);
        }
        else
        {
            if (BackendScanner.getInstance().scheduleOneTask())
            {
                message = "The refresh task was submitted successfully.";
            }
            else
            {
                message = "The refresh task is in progress!";
            }
            builder = Response.ok(message);
        }

        return builder.build();
    }

    @GET
    @Path("/syncnow")
    public Response syncNow(@Context HttpServletRequest req, @Context HttpServletResponse res)
    {
        String message;
        ResponseBuilder builder;

        if (!HeadUtils.isSuperLogin() && !HeadUtils.isLocalLogin())
        {
            message = "Only Local login or Administrator login allowed to do this!";
            builder = Response.status(403);
            builder.entity(message);
        }
        else
        {
            if (BackendScanner.getInstance().scheduleOneBackupTask())
            {
                message = "The Sync task was submitted successfully.";
            }
            else
            {
                message = "The Sync task is in progress!";
            }
            builder = Response.ok(message);
        }

        return builder.build();
    }

    @GET
    @Path("/favicon.ico")
    public Response getFavicon(@Context HttpServletRequest req,
                               @Context HttpServletResponse response)
    {
        logger.debug("getFavicon in!");
        ResponseBuilder builder = Response.ok();
        try
        {
            FileInputStream fp = new FileInputStream(new File("favicon.ico"));
            builder.entity(fp);
            builder.header("Content-type", "image/x-icon");
            HeadUtils.setExpiredTime(builder);
            logger.debug("getFavicon out!");
        }
        catch (Exception e)
        {
            logger.error("catch some exception.", e);
        }

        return builder.build();
    }

    @GET
    @Path("/album.apk")
    public Response getAPK(@Context HttpServletRequest req, @Context HttpServletResponse response)
    {
        logger.debug("getAPK in!");
        ResponseBuilder builder = Response.ok();
        try
        {
            FileInputStream fp =
                    new FileInputStream(new File("client" + File.separator + "album.apk"));
            builder.entity(fp);
            builder.header("Content-type", "application/vnd.android.package-archive");
            HeadUtils.setExpiredTime(builder);
            logger.debug("getAPK out!");
        }
        catch (Exception e)
        {
            logger.error("catch some exception.", e);
        }

        return builder.build();
    }

    @GET
    @Path("/js/{file}")
    public Response getJSFile(@PathParam("file") String file, @Context HttpServletRequest req,
                              @Context HttpServletResponse response)
    {
        logger.debug("get js file in!");
        ResponseBuilder builder = Response.ok();
        try
        {
            File filepath = new File("js" + File.separator + file);
            if (filepath.isFile())
            {
                BufferedInputStream fp = new BufferedInputStream(new FileInputStream(filepath));
                builder.entity(fp);
                builder.header("Content-type", HeadUtils.judgeMIME(file));
                HeadUtils.setExpiredTime(builder);
                logger.debug("getFavicon out!");
            }
            else
            {
                builder.status(404);
            }
        }
        catch (Exception e)
        {
            logger.error("catch some exception.", e);
        }

        return builder.build();
    }

    @GET
    @Path("/")
    public Response getMsg(@Context HttpServletRequest req, @Context HttpServletResponse response)
            throws IOException
    {
        ResponseBuilder builder = Response.status(200);
        String body = GenerateHTML.genIndexPage(getFileList(req),
                                                (HeadUtils.isMobile() || HeadUtils.isVideo()) ? 3
                                                                                              : 5,
                                                true);
        if (StringUtils.isNotBlank(body))
        {
            builder.entity(body);
            builder.header("Content-type", "text/html");
        }
        else
        {
            builder.entity("404 not found!");
            builder.status(404);
        }

        HeadUtils.refreshCookie(response);

        return builder.build();
    }

    private List<?> getFileList(HttpServletRequest req)
    {
        List<?> lst;

        int count = HeadUtils.judgeCountPerOnePage(req);
        String next = req.getParameter("next");
        String prev = req.getParameter("prev");
        String id = null;
        boolean isnext = true;
        if (StringUtils.isNotBlank(next))
        {
            id = next;
        }
        else if (StringUtils.isNotBlank(prev))
        {
            id = prev;
            isnext = false;
        }

        if (HeadUtils.isFaces())
        {
            lst = FaceTableDao.getInstance().getNextNineFileByHashStr(id, count, isnext);
            if (lst != null)
            {
                for (Object f : lst)
                {
                    ((Face) f).setFi(UniqPhotosStore.getInstance()
                                             .getOneFileByHashStr(((Face) f).getEtag()));
                }
            }
        }
        else if (HeadUtils.isNoFaces())
        {
            lst = FaceTableDao.getInstance().getNextNoFacesPics(id, count, isnext);
            if (lst != null)
            {
                for (Object f : lst)
                {
                    ((Face) f).setFi(UniqPhotosStore.getInstance()
                                             .getOneFileByHashStr(((Face) f).getEtag()));
                }
            }
        }
        else
        {
            lst = UniqPhotosStore.getInstance()
                    .getNextNineFileByHashStr(id, count, isnext, HeadUtils.isVideo());
            if ((lst == null || lst.isEmpty()) && id != null)
            {
                lst = UniqPhotosStore.getInstance()
                        .getNextNineFileByHashStr(null, count, isnext, HeadUtils.isVideo());
            }
        }
        return lst;

    }

    @Path("/faces/")
    public Object getFaces()
    {
        return new FacesService(-1);
    }

    @Path("/faces/{id}")
    public Object getFaceFiles(@PathParam("id") String id)
    {
        try
        {
            long faceid = Long.parseLong(id);
            return new FacesService(faceid);
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
        }
        return new ErrorResource();
    }

    @Path("/facetoken/{id}")
    public Object getFace(@PathParam("id") String id)
    {
        if (StringUtils.isNotBlank(id))
        {
            return new FacesTokenService(id);
        }
        else
        {
            return new ErrorResource();
        }
    }

    @Path("/photos/{id}")
    public Object getPhoto(@PathParam("id") String id, @Context HttpServletRequest req,
                           @Context HttpServletResponse response, @Context HttpHeaders headers,
                           InputStream body)
    {
        if (StringUtils.isNotBlank(id))
        {
            return new ObjectService(id);
        }
        else
        {
            return new ErrorResource();
        }
    }

    @Path("/year/{year}")
    public Object getYearView(@PathParam("year") String year, @Context HttpServletRequest req,
                              @Context HttpServletResponse response, @Context HttpHeaders headers,
                              InputStream body)
    {
        if (StringUtils.isNotBlank(year))
        {
            return new YearService(year);
        }
        else
        {
            return new ErrorResource();
        }
    }

    @Path("/month/{month}")
    public Object getMonthView(@PathParam("month") String month, @Context HttpServletRequest req,
                               @Context HttpServletResponse response, @Context HttpHeaders headers,
                               InputStream body)
    {
        if (StringUtils.isNotBlank(month) && month.length() == 6)
        {
            return new MonthService(month);
        }
        else
        {
            return new ErrorResource();
        }
    }

    @Path("/day/{day}")
    public Object getDayView(@PathParam("day") String day, @Context HttpServletRequest req,
                             @Context HttpServletResponse response, @Context HttpHeaders headers,
                             InputStream body)
    {
        if (StringUtils.isNotBlank(day) && day.length() == 8)
        {
            return new DayService(day);
        }
        else
        {
            return new ErrorResource();
        }
    }
}

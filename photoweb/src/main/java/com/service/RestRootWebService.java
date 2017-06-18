package com.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dao.FaceTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.facer.Face;
import com.backend.scan.BackendScaner;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;

@Produces(value = { "text/html", "application/octet-stream" })
public class RestRootWebService extends HttpServlet
{
    private static final Logger logger = LoggerFactory.getLogger(RestRootWebService.class);

    private static final long serialVersionUID = -7748065720779404006L;

    @GET
    @Path("/flushnow")
    public Response flushNow(@Context HttpServletRequest req, @Context HttpServletResponse res)
    {
        String message = "";
        if (BackendScaner.getInstance().scheduleOneTask())
        {
            message = "The refresh task was submitted successfully.";
        }
        else
        {
            message = "The refresh task is in progress.!";
        }
        ResponseBuilder builder = Response.ok(message);
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
    @Path("/js/{file}")
    public Response getJSFile(@PathParam("file") String file, @Context HttpServletRequest req,
            @Context HttpServletResponse response)
    {
        logger.debug("get js file in!");
        ResponseBuilder builder = null;
        try
        {
            File filepath = new File("js" + File.separator + file);
            if (filepath.isFile())
            {
                builder = Response.ok();
                BufferedInputStream fp = new BufferedInputStream(new FileInputStream(filepath));
                builder.entity(fp);
                builder.header("Content-type", HeadUtils.judgeMIME(file));
                HeadUtils.setExpiredTime(builder);
                logger.debug("getFavicon out!");
            }
            else
            {
                builder = Response.ok();
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
                (HeadUtils.isMobile() || HeadUtils.isVideo()) ? 3 : 5, true);
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

        return builder.build();
    }

    /*
     * @Path("/video{var:.*}") public Object getViedoResource(@PathParam("var")
     * String var, @Context HttpServletRequest req, @Context HttpServletResponse
     * response) throws IOException { return new VideoRootResource(var); }
     */
    public List<?> getFileList(HttpServletRequest req)
    {
        List<?> lst = null;

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
        else
        {
            lst = UniqPhotosStore.getInstance().getNextNineFileByHashStr(id, count, isnext,
                    HeadUtils.isVideo());
            if ((lst == null || lst.isEmpty()) && id != null)
            {
                lst = UniqPhotosStore.getInstance().getNextNineFileByHashStr(null, count, isnext,
                        HeadUtils.isVideo());
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
            @Context HttpServletResponse response, @Context HttpHeaders headers, InputStream body)
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
            @Context HttpServletResponse response, @Context HttpHeaders headers, InputStream body)
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
            @Context HttpServletResponse response, @Context HttpHeaders headers, InputStream body)
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
            @Context HttpServletResponse response, @Context HttpHeaders headers, InputStream body)
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

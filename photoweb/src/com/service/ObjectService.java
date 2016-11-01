package com.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.FileInfo;
import com.backend.dao.BaseSqliteStore;
import com.backend.dao.UniqPhotosStore;
import com.backend.scan.FileTools;
import com.service.io.RangesFileInputStream;
import com.utils.media.MediaTool;
import com.utils.media.ThumbnailManager;
import com.utils.sys.SystemConstant;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;
import com.utils.web.Range;

public class ObjectService
{

    private static final Logger logger = LoggerFactory.getLogger(ObjectService.class);

    private static Pattern rangeStrPattern = Pattern.compile("^bytes=((\\d*-\\d*)(,\\d*-\\d*)*)$");

    private static Pattern oneRangePattern = Pattern.compile("(\\d*)-(\\d*)$");

    private String id;

    public ObjectService(String id)
    {
        this.setId(id);
    }

    @GET
    public Response getPhotoData(@Context HttpServletRequest req, @Context HttpServletResponse response)
            throws IOException
    {
        ResponseBuilder builder = Response.status(200);
        BaseSqliteStore meta = BaseSqliteStore.getInstance();
        FileInfo f = meta.getOneFileByHashID(id);
        if ("true".equalsIgnoreCase(req.getParameter("content")))
        {
            if (f != null && new File(f.getPath()).isFile())
            {
                InputStream fi = null;
                if (MediaTool.isVideo(f.getPath()))
                {
                    List<Range> rlst = parseRangeStrEx(req.getHeader(SystemConstant.RANGE_HEADER));
                    if (!rlst.isEmpty())
                    {
                        Range r = rlst.get(0);
                        @SuppressWarnings("resource")
                        RangesFileInputStream rfi = new RangesFileInputStream(new File(f.getPath()), r.getStart(),
                                r.getEnd());
                        // Content-Range:bytes 0-17190973/17190974
                        builder.header("Content-Range",
                                "bytes " + rfi.getPos() + "-" + rfi.getEnd() + "/" + f.getSize());
                        builder.header("Content-length", rfi.getEnd() - rfi.getPos() + 1);
                        fi = rfi;
                    }
                }
                else
                {
                    String sizestr = req.getParameter("size");
                    int size = Integer.parseInt(sizestr);

                    if (size <= 400)
                    {
                        fi = ThumbnailManager.getThumbnail(id);
                        if (fi == null)
                        {
                            FileTools.submitAnThumbnailTask(f);
                        }
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
                    builder.header("Content-Disposition", "filename=" + new File(f.getPath()).getName());
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

    private List<Range> parseRangeStrEx(String rstr)
    {
        List<Range> rlst = new LinkedList<Range>();
        if (StringUtils.isBlank(rstr))
        {
            return rlst;
        }

        Matcher rall = rangeStrPattern.matcher(rstr);
        boolean isMatchAll = rall.matches();
        if (isMatchAll)
        {
            String ranges = rall.group(1);
            if (StringUtils.isBlank(ranges))
            {
                return rlst;
            }

            String[] rs = ranges.split(",");
            if (rs != null)
            {
                for (String oner : rs)
                {
                    Matcher ro = oneRangePattern.matcher(oner);
                    if (ro.matches())
                    {
                        Range r = new Range();
                        if (StringUtils.isNotBlank(ro.group(1)))
                        {
                            r.setStart(Long.parseLong(ro.group(1)));
                        }
                        else
                        {
                            r.setStart(-1);
                        }

                        if (StringUtils.isNotBlank(ro.group(2)))
                        {
                            r.setEnd(Long.parseLong(ro.group(2)));
                        }
                        else
                        {
                            r.setEnd(-1);
                        }
                    }
                }
            }

        }

        return rlst;
    }

    @SuppressWarnings("unused")
    private List<Range> parseRangeStr(String rangestr)
    {
        List<Range> rlst = new LinkedList<Range>();
        // 需要考虑ranges下载，暂时只支持单range下载。
        // Content-Range:bytes 0-17190973/17190974
        // Range:bytes=0-
        // Range:bytes=0-10
        if (StringUtils.isNotBlank(rangestr))
        {
            rangestr = rangestr.trim();
            rangestr = rangestr.toLowerCase();
            if (rangestr.startsWith("bytes="))
            {
                rangestr = rangestr.substring(6);
                String[] rss = null;
                if (rangestr.contains(","))
                {
                    String[] rs = rangestr.split(",");
                    if (rs != null && rs.length > 0)
                    {
                        if (rs[0].contains("-"))
                        {
                            rss = rs[0].split("-");
                        }
                    }
                }

                if (rss == null)
                {
                    if (rangestr.contains("-"))
                    {
                        rss = rangestr.split("-");
                    }
                }

                if (rss != null)
                {
                    if (rss.length == 2)
                    {
                        Range r = new Range();
                        r.setStart(Long.parseLong(rss[0]));
                        r.setEnd(Integer.parseInt(rss[1]));
                        rlst.add(r);
                    }
                    else if (rss.length == 1)
                    {
                        Range r = new Range();
                        r.setStart(Integer.parseInt(rss[0]));
                        r.setEnd(-1);
                        rlst.add(r);
                    }
                }
            }
        }
        return rlst;
    }

    @DELETE
    public Response deletePhotoData(@Context HttpServletRequest req, @Context HttpServletResponse response)
            throws IOException
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

package com.service;

import com.backend.dao.BaseSqliteStore;
import com.backend.dao.FaceTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.entity.FileInfo;
import com.backend.scan.FileTools;
import com.service.io.RangesFileInputStream;
import com.utils.media.MediaTool;
import com.utils.media.ThumbnailManager;
import com.utils.sys.SystemConstant;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;
import com.utils.web.Range;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.*;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Produces(value = { "text/html", "application/octet-stream" })
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
    public Response getPhotoData(@Context HttpServletRequest req,
            @Context HttpServletResponse response) throws IOException
    {
        ResponseBuilder builder = Response.status(200);
        BaseSqliteStore meta = BaseSqliteStore.getInstance();
        FileInfo f = meta.getOneFileByHashID(id);

        if (f == null)
        {
            builder.status(404);
            return builder.build();
        }

        if ("true".equalsIgnoreCase(req.getParameter("content")))
        {
            File cfile = new File(f.getPath());
            if (cfile.isFile())
            {
                InputStream fi;
                File thumbnail = null;
                String sizestr = req.getParameter("size");
                if (StringUtils.isNotBlank(sizestr))
                {
                    int size = Integer.parseInt(sizestr);
                    if (size <= 400)
                    {
                        thumbnail = ThumbnailManager.getThumbnail(id);
                        if (thumbnail == null)
                        {
                            // 提交异步任务生成缩略图。
                            FileTools.submitAnThumbnailTask(f);
                            if (MediaTool.isVideo(f.getPath()))
                            {
                                thumbnail = new File(SystemConstant.DEFAULT_VIDEO_PIC_PATH);
                            }
                            else
                            {
                                thumbnail = cfile;
                            }
                        }

                    }
                }

                if (thumbnail != null)
                {
                    // 缩略图存在则使用缩略图。
                    fi = new BufferedInputStream(new FileInputStream(thumbnail));
                    builder.header("Content-length", thumbnail.length() + "");
                }
                else
                {
                    if (MediaTool.isVideo(f.getPath()))
                    {
                        String rangeStr = req.getHeader(SystemConstant.RANGE_HEADER);
                        fi = dealWitVideoRangeDownload(builder, f, cfile, rangeStr);
                    }
                    else
                    {
                        fi = new BufferedInputStream(new FileInputStream(cfile));
                        builder.header("Content-length", cfile.length() + "");
                    }
                }

                String fileName = thumbnail != null ? thumbnail.getName()
                        : new File(f.getPath()).getName();
                String contenttype = thumbnail != null
                        ? HeadUtils.getContentType(thumbnail.getCanonicalPath())
                        : HeadUtils.getContentType(f.getPath());

                MDC.put(SystemConstant.FILE_NAME, fileName);
                builder.entity(fi);

                builder.header("Content-type", contenttype);
                logger.info("content type is: {}", contenttype);
                HeadUtils.setExpiredTime(builder);
                builder.header("Content-Disposition", "filename=" + fileName);
                builder.header("MediaFileFullPath", URLEncoder.encode(f.getPath(), "UTF-8"));
                logger.info("the file is: {}, Mime: {}", f, contenttype);
            }
            else
            {
                builder.status(404);
            }
        }
        else
        {
            String bodyContent = GenerateHTML.generateSinglePhoto(f, false);
            builder.header("Content-type", "text/html");
            builder.entity(bodyContent);
            logger.info("the page is {}", bodyContent);
        }

        return builder.build();
    }

    private InputStream dealWitVideoRangeDownload(ResponseBuilder builder, FileInfo f, File cfile,
            String rangeStr) throws IOException
    {
        if (StringUtils.isNotBlank(rangeStr))
        {
            MDC.put(SystemConstant.RANGE_HEADER_KEY, rangeStr);
        }

        List<Range> rlst = parseRangeStrEx(rangeStr);
        if (!rlst.isEmpty())
        {
            Range r = rlst.get(0);
            RangesFileInputStream rfi = new RangesFileInputStream(cfile, r.getStart(), r.getEnd());
            // Content-Range:bytes 0-17190973/17190974
            String contentRange = "bytes " + rfi.getPos() + "-" + rfi.getEnd() + "/" + f.getSize();
            logger.debug("Content-Range is: " + contentRange);
            String contentLength = "" + (rfi.getEnd() - rfi.getPos() + 1);
            logger.debug("Content-Length is: " + contentLength);
            builder.header("Content-Range", contentRange);
            builder.header("Content-Length", contentLength);
            builder.status(206);
            return new BufferedInputStream(rfi);
        }
        else
        {
            builder.header("Content-Length", cfile.length());
            return new BufferedInputStream(new FileInputStream(cfile));
        }
    }

    private List<Range> parseRangeStrEx(String rstr) throws IOException
    {
        List<Range> rlst = new LinkedList<>();
        try
        {
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

                if (!ranges.contains(","))
                {
                    Range r = getOneRange(ranges);
                    if (r != null)
                    {
                        rlst.add(r);
                    }

                    return rlst;
                }

                String[] rs = ranges.split(",");
                for (String oner : rs)
                {
                    Range r = getOneRange(oner);
                    if (r != null)
                    {
                        rlst.add(r);
                    }
                }

            }

            return rlst;
        }
        catch (Exception e)
        {
            logger.warn("caused: ", e);
            throw new IOException("some error occured when parse the range header.");
        }
    }

    private Range getOneRange(String oner)
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

            return r;
        }

        return null;
    }

    @SuppressWarnings("unused")
    private List<Range> parseRangeStr(String rangestr)
    {
        List<Range> rlst = new LinkedList<>();
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
                    if (rs.length > 0)
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
    public Response deletePhotoData(@Context HttpServletRequest req,
                                    @Context HttpServletResponse response) throws IOException
    {

        logger.warn("try to delete the photo: " + id);
        ResponseBuilder builder = Response.status(204);

        if (!(HeadUtils.isSuperLogin() || HeadUtils.isLocalLogin()))
        {
            builder.status(403);
            logger.warn("not be permitted to delete data for common token logon");
        }
        else
        {
            try
            {
                // 延迟1.5s下发删除请求，前端刷新页面时需要依赖此条记录。盲等前端刷新玩页面后再行删除。
                Thread.sleep(1500);
                BaseSqliteStore.getInstance().setPhotoToBeHiden(id, false);
                UniqPhotosStore.getInstance().deleteRecordByID(id);
                FaceTableDao.getInstance().deleteOneFile(id);

                /*
                 * if (fnext!= null && !fnext.isEmpty()) { // 刷新整个页面。 String
                 * bodyContent = GenerateHTML.generateSinglePhoto(fnext.get(0));
                 * builder.header("Content-type", "text/html");
                 * builder.entity(bodyContent); logger.info("the page is {}",
                 * bodyContent); }
                 */
                logger.warn("deleted the photo: " + id);
            }
            catch (Exception e)
            {
                logger.warn("caught by, ", e);
            }
        }

        return builder.build();
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

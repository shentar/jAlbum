package com.service.video;

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

import org.apache.commons.lang.StringUtils;

import com.utils.web.GenerateVideoHtml;
import com.utils.web.HeadUtils;

public class VideoRootResource
{
    String subPath = null;

    public VideoRootResource(String subPath)
    {
        this.subPath = subPath;
    }

    @GET
    public Response getMsg(@Context HttpServletRequest req, @Context HttpServletResponse response,
            InputStream in) throws IOException
    {
        ResponseBuilder builder = Response.status(200);
        Object body = "";

        if (StringUtils.isBlank(subPath) || subPath.equals("/"))
        {
            builder.header("Content-type", "text/html");
            body = GenerateVideoHtml.genIndexHtml();
        }
        else if (subPath.startsWith("id/"))
        {
            builder.header("Content-type", "text/html");
            String videoId = subPath.substring(4);
            body = videoId;
        }
        else
        {
            String filePath = "C:\\Users\\tyanq\\Desktop\\media\\1473089246274873308.mp4";
            builder.header("Content-type", HeadUtils.judgeMIME(filePath));
            body = new BufferedInputStream(new FileInputStream(new File(filePath)));
        }

        builder.entity(body);

        return builder.build();
    }
}

package com.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.DateRecords;
import com.backend.DateTableDao;
import com.backend.FileInfo;
import com.backend.UniqPhotosStore;
import com.utils.web.GenerateHTML;

public class DayService
{
    private static final Logger logger = LoggerFactory.getLogger(MonthService.class);

    private String day;

    public DayService(String day)
    {
        this.day = day;
    }

    @GET
    public Response getDayIndex(@Context HttpServletRequest req, @Context HttpServletResponse response)
    {
        ResponseBuilder builder = Response.status(200);
        DateRecords dr = DateTableDao.getInstance().getOneRecordsByDay(day);
        if (dr == null)
        {
            logger.info("there is no photos in this year: " + day);
            builder.status(404);
            builder.entity(GenerateHTML.generate404Notfound());
            return builder.build();
        }
        else
        {
            List<FileInfo> flst = UniqPhotosStore.getInstance().getAllPhotosBy(day);
            builder.entity(GenerateHTML.generateDayPage(day, flst));
            return builder.build();
        }
    }
}

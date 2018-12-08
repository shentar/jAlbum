package com.service;

import com.backend.dao.DateRecords;
import com.backend.dao.DateTableDao;
import com.utils.web.GenerateHTML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.TreeMap;

public class YearService
{
    private static final Logger logger = LoggerFactory.getLogger(YearService.class);

    private String year;

    public YearService(String year)
    {
        this.setYear(year);
    }

    @GET
    public Response getYearIndex(@Context HttpServletRequest req,
            @Context HttpServletResponse response)
    {
        ResponseBuilder builder = Response.status(200);
        TreeMap<String, TreeMap<String, TreeMap<String, DateRecords>>> allrecords = DateTableDao
                .getInstance().getAllDateRecord();
        TreeMap<String, TreeMap<String, DateRecords>> currentyear = allrecords.get(year);
        if (currentyear == null || currentyear.isEmpty() || year.length() != 4)
        {
            logger.info("there is no photos in this year: " + year);
            builder.status(404);
            builder.entity(GenerateHTML.generate404Notfound());
            return builder.build();
        }
        else
        {
            builder.entity(GenerateHTML.generateYearPage(year, currentyear));
            return builder.build();
        }
    }

    private void setYear(String y)
    {
        this.year = y;
    }

    public String getYear()
    {
        return year;
    }
}

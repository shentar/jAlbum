package com.service;

import java.util.Map;

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
import com.utils.web.GenerateHTML;

public class YearService
{
    private static final Logger logger = LoggerFactory.getLogger(YearService.class);

    private String year;

    public YearService(String year)
    {
        this.setYear(year);
    }

    @GET
    public Response getYearIndex(@Context HttpServletRequest req, @Context HttpServletResponse response)
    {
        ResponseBuilder builder = Response.status(200);
        Map<String, Map<String, Map<String, DateRecords>>> allrecords = DateTableDao.getInstance().getAllDateRecord();
        Map<String, Map<String, DateRecords>> currentyear = allrecords.get(year);
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

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

public class MonthService
{
    private static final Logger logger = LoggerFactory.getLogger(MonthService.class);

    private String month;

    public MonthService(String month)
    {
        this.setMonth(month);
    }

    @GET
    public Response getMonthIndex(@Context HttpServletRequest req, @Context HttpServletResponse response)
    {
        ResponseBuilder builder = Response.status(200);
        Map<String, Map<String, Map<String, DateRecords>>> allrecords = DateTableDao.getInstance().getAllDateRecord();
        String year = month.substring(0, 4);
        String smonth = month.substring(4, 6);
        Map<String, Map<String, DateRecords>> currentyear = allrecords.get(year);
        if (currentyear == null || currentyear.isEmpty())
        {
            logger.info("there is no photos in this year: " + year);
            builder.status(404);
            builder.entity(GenerateHTML.generate404Notfound());
            return builder.build();
        }
        else
        {
            builder.entity(GenerateHTML.generateMonthPage(month, currentyear.get(smonth)));
            return builder.build();
        }
    }

    public String getMonth()
    {
        return month;
    }

    public void setMonth(String month)
    {
        this.month = month;
    }
}

package com.service;

import com.backend.dao.DateRecords;
import com.backend.dao.DateTableDao;
import com.utils.web.GenerateHTML;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.TreeMap;

public class MonthService
{
    private static final Logger logger = LoggerFactory.getLogger(MonthService.class);

    private String month;

    public MonthService(String month)
    {
        this.setMonth(month);
    }

    @GET
    public Response getMonthIndex(@Context HttpServletRequest req,
            @Context HttpServletResponse response)
    {
        ResponseBuilder builder = Response.status(200);
        TreeMap<String, TreeMap<String, TreeMap<String, DateRecords>>> allrecords = DateTableDao
                .getInstance().getAllDateRecord();
        String year = month.substring(0, 4);
        String smonth = month.substring(4, 6);
        TreeMap<String, TreeMap<String, DateRecords>> currentyear = allrecords.get(year);
        if (currentyear == null || currentyear.isEmpty())
        {
            logger.info("there is no photos in this year: " + year);
            builder.status(404);
            builder.entity(GenerateHTML.generate404Notfound());
            return builder.build();
        }
        else
        {
            String prevMonth = currentyear.lowerKey(smonth);
            if (StringUtils.isNotBlank(prevMonth))
            {
                prevMonth = year + prevMonth;
            }
            else
            {
                prevMonth = null;
            }

            String nextMonth = currentyear.higherKey(smonth);
            if (StringUtils.isNotBlank(nextMonth))
            {
                nextMonth = year + nextMonth;
            }
            else
            {
                nextMonth = null;
            }

            builder.entity(GenerateHTML.generateMonthPage(month, nextMonth, prevMonth,
                    currentyear.get(smonth)));
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

package com.service;

import com.backend.dao.DateRecords;
import com.backend.dao.DateTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.entity.FileInfo;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.List;
import java.util.TreeMap;

public class DayService
{
    private static final Logger logger = LoggerFactory.getLogger(MonthService.class);

    private String day;

    public DayService(String day)
    {
        this.day = day;
    }

    @GET
    public Response getDayIndex(@Context HttpServletRequest req,
            @Context HttpServletResponse response)
    {
        ResponseBuilder builder = Response.status(200);
        TreeMap<String, TreeMap<String, TreeMap<String, DateRecords>>> map = DateTableDao
                .getInstance().getAllDateRecord();
        String year = day.substring(0, 4);
        String month = day.substring(4, 6);
        String sday = day.substring(6, 8);
        DateRecords dr = null;
        String prevDay = null;
        String nextDay = null;

        TreeMap<String, DateRecords> mmap;

        TreeMap<String, TreeMap<String, DateRecords>> ymap = map.get(year);
        if (ymap != null)
        {
            mmap = ymap.get(month);
            if (mmap != null)
            {
                dr = mmap.get(sday);
                prevDay = mmap.lowerKey(sday);
                if (StringUtils.isNotBlank(prevDay))
                {
                    prevDay = year + month + prevDay;
                }

                nextDay = mmap.higherKey(sday);
                if (StringUtils.isNotBlank(nextDay))
                {
                    nextDay = year + month + nextDay;
                }
            }
        }

        if (dr == null)
        {
            logger.info("there is no photos in day: " + day);
            builder.status(404);
            builder.entity(GenerateHTML.generate404Notfound());
            return builder.build();
        }
        else
        {
            List<FileInfo> flst = UniqPhotosStore.getInstance().getAllPhotosBy(day);
            builder.entity(GenerateHTML.generateDayPage(day, prevDay, nextDay, flst,
                    HeadUtils.isMobile() ? 3 : 5));
            return builder.build();
        }
    }
}

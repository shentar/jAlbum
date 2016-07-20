package com.utils.web;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.backend.DateRecords;
import com.backend.DateTableDao;
import com.backend.FileInfo;
import com.backend.UniqPhotosStore;
import com.utils.conf.AppConfig;

public class GenerateHTML
{

    public static String genIndexPage(List<FileInfo> flst)
    {
        return genIndexPage(flst, 5);
    }

    public static String genIndexPage(List<FileInfo> flst, int rowCount)
    {
        if (flst == null || flst.isEmpty())
        {
            return generate404Notfound();
        }

        if (flst.size() == 1)
        {
            return generateSinglePhoto(flst.get(0));
        }

        FileInfo firstP = flst.get(0);
        FileInfo endP = flst.get(flst.size() - 1);
        StringBuffer sb = new StringBuffer();
        sb.append(getHtmlHead());
        String yearNavigate = genYearNavigate();
        sb.append(yearNavigate);
        String indexPageNavi = genIndexNavigate(firstP, endP);
        sb.append(indexPageNavi);
        sb.append("<br/><br/>");
        sb.append(
                "<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
        int i = 0;
        int start = 0;
        int end = 0;
        for (FileInfo f : flst)
        {
            if (i % rowCount == 0)
            {
                start++;
                sb.append("<tr>");
            }

            sb.append(
                    "<td width=\"20%\" height=\"18%\" bordercolor=\"#000000\"><br/>");
            sb.append("<a href=\"/photos/" + f.getHash256()
                    + "\" target=\"_blank\">");
            sb.append("<img " + (restrictSize(f) ? "width" : "height")
                    + "=\"340px\" src = \"/photos/" + f.getHash256()
                    + "?content=true&size=340" + "\"></img>" + "</a></td>");

            if ((i + 1) % rowCount == 0)
            {
                end++;
                sb.append("</tr>");
            }

            i++;
        }
        if (end != start)
        {
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append(indexPageNavi);
        sb.append(yearNavigate);
        sb.append(getHtmlFoot());

        return sb.toString();
    }

    private static String genIndexNavigate(FileInfo firstP, FileInfo endP)
    {
        StringBuffer indexPageNavi = new StringBuffer();
        indexPageNavi.append(
                "<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
        indexPageNavi
                .append("<tr><td width=\"33%\" bordercolor=\"#000000\"><a href=\"?prev="
                        + firstP.getHash256() + "&count="
                        + AppConfig.getInstance().getMaxCountOfPicInOnePage(25)
                        + "\">上一页</a></td>");
        indexPageNavi.append("<td width=\"33%\" bordercolor=\"#000000\">"
                + firstP.getPhotoTime() + " ~ " + endP.getPhotoTime()
                + "</td>");
        indexPageNavi
                .append("<td width=\"33%\" bordercolor=\"#000000\"><a href=\"?next="
                        + endP.getHash256() + "&count="
                        + AppConfig.getInstance().getMaxCountOfPicInOnePage(25)
                        + "\">下一页</a></td></tr></table>");
        return indexPageNavi.toString();
    }

    public static String genYearNavigate()
    {
        TreeMap<String, TreeMap<String, TreeMap<String, DateRecords>>> allrecords = DateTableDao
                .getInstance().getAllDateRecord();
        if (allrecords != null && !allrecords.isEmpty())
        {
            StringBuffer ylst = new StringBuffer(
                    "<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

            int i = 0;
            int start = 0;
            int end = 0;

            List<String> arrayList = getSortedKeyList(allrecords.keySet());

            for (String f : arrayList)
            {
                if (i % 20 == 0)
                {
                    start++;
                    ylst.append("<tr>");
                }

                if (i == 0)
                {
                    ylst.append(
                            "<td width=\"5%\" height=\"30px\" bordercolor=\"#000000\">");
                    ylst.append("<a href=\"/\">首页</a></td>");
                    i++;
                }

                ylst.append("<td width=\"5%\" bordercolor=\"#000000\">");
                ylst.append("<a href=\"/year/" + f + "\" >" + f + "</a></td>");

                if ((i + 1) % 20 == 0)
                {
                    end++;
                    ylst.append("</tr>");
                }

                i++;
            }
            if (end != start)
            {
                ylst.append("</tr>");
            }

            ylst.append("</table>");
            return ylst.toString();
        }

        return "";
    }

    public static String getHtmlHead()
    {
        String hh = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">"
                + "<head profile=\"http://gmpg.org/xfn/11\">"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/> "
                + "<script type=\"text/javascript\" src=\"/js/jquery-3.1.0.js\"></script>"
                + "<title>相册</title></head><body>";
        return hh;
    }

    private static List<String> getSortedKeyList(Set<String> set)
    {
        List<String> arrayList = new LinkedList<String>();
        arrayList.addAll(set);
        Collections.sort(arrayList, new Comparator<String>()
        {
            @Override
            public int compare(String arg0, String arg1)
            {
                if (arg0 != null)
                {
                    return 0 - arg0.compareTo(arg1);
                }
                else
                {
                    return 1;
                }
            }
        });
        return arrayList;
    }

    public static String getHtmlFoot()
    {
        return "</body></html>";
    }

    public static String generateSinglePhoto(FileInfo f)
    {

        if (f == null)
        {
            return generate404Notfound();
        }
        else
        {
            StringBuffer sb = new StringBuffer(getHtmlHead());
            String yearNavigage = genYearNavigate();
            sb.append(yearNavigage);

            sb.append(
                    "<script type=\"text/javascript\" src=\"/js/jQueryRotate.js\"></script>");
            sb.append(
                    "<script type=\"text/javascript\" src=\"/js/rotate.js\"></script>");
            sb.append("<script type=\"text/javascript\">"
                    + "function changeUrl(url){window.history.pushState({},0,'http://'+window.location.host+'/'+url);};"
                    + "window.onload=changeUrl(" + "'photos/" + f.getHash256()
                    + "');" + "function deletephoto(path){changeUrl("
                    + "'?next=" + f.getHash256() + "&count=1'"
                    + ");window.location.reload();"
                    + "$.ajax({url:path,type:'DELETE',"
                    + "success:function(result){}});}" + "</script>");

            sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                    + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
            sb.append(
                    "<tr><td width=\"33%\" bordercolor=\"#000000\"><a href=\"/?prev="
                            + f.getHash256() + "&count=1" + "\">上一张</a></td>");
            sb.append("<td width=\"33%\" bordercolor=\"#000000\">"
                    + f.getPhotoTime() + "</td>");
            sb.append(
                    "<td width=\"33%\" bordercolor=\"#000000\"><a href=\"/?next="
                            + f.getHash256() + "&count=1"
                            + "\">下一张</a></td></tr></table>");
            sb.append("<br/><br/>");
            sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                    + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
            sb.append(
                    "<tr><td width=\"100%\" height=\"100%\" bordercolor=\"#000000\"><br/>");
            // sb.append("<a href=\"/photos/" + f.getHash256() + "?content=true"
            // + "\" target=\"_blank\">");
            sb.append("<img id=\"singlephoto\""
                    + (restrictSize(f) ? "width" : "height")
                    + "=\"900px\" src = \"/photos/" + f.getHash256()
                    + "?content=true" + "\"></img>");
            // sb.append("</a>");
            sb.append("</td></tr>");

            sb.append("<tr><td><a id=\"leftrotate\" href=\"#\">左旋转</a>"
                    + "&nbsp;&nbsp;&nbsp;" + "<a href=\"javascript:deletephoto("
                    + "\'/photos/" + f.getHash256() + "'" + ");\">删除</a>"
                    + "&nbsp;&nbsp;&nbsp;"
                    + "<a id=\"rightrotate\" href=\"#\">右旋转</a></td><tr>");

            sb.append("</table>");

            sb.append(yearNavigage);

            sb.append(getHtmlFoot());
            return sb.toString();
        }
    }

    public static String generateYearPage(String year,
            TreeMap<String, TreeMap<String, DateRecords>> currentyear)
    {
        if (currentyear == null || currentyear.isEmpty())
        {
            return generate404Notfound();
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getHtmlHead());
        String yearNavigete = genYearNavigate();
        sb.append(yearNavigete);
        sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

        int i = 0;
        int start = 0;
        int end = 0;

        List<String> arrayList = getSortedKeyList(currentyear.keySet());

        for (String mo : arrayList)
        {
            int filecount = 0;
            Map<String, DateRecords> mr = currentyear.get(mo);
            String pic = null;
            for (Entry<String, DateRecords> en : mr.entrySet())
            {
                filecount += en.getValue().getPiccount();
                if (pic == null)
                {
                    pic = en.getValue().getFirstpic();
                }
            }

            if (i % 4 == 0)
            {
                start++;
                sb.append("<tr>");
            }

            FileInfo f = UniqPhotosStore.getInstance().getOneFileByHashStr(pic);
            if (f != null)
            {
                sb.append(
                        "<td width=\"25%\" height=\"31%\" bordercolor=\"#000000\">");
                sb.append("<a href=\"/month/" + year + mo + "\" >");
                sb.append("<img " + (restrictSize(f) ? "width" : "height")
                        + "=\"280px\" src = \"/photos/" + pic
                        + "?content=true&size=280" + "\"></img>"
                        + "</a><br/><b>" + year + "-" + mo + "月份 (" + filecount
                        + "张)</b></td>");
            }
            if ((i + 1) % 4 == 0)
            {
                end++;
                sb.append("</tr>");
            }

            i++;
        }

        if (end != start)
        {
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append(yearNavigete);
        sb.append(getHtmlFoot());

        return sb.toString();
    }

    public static String generateDayPage(String day, String prevDay,
            String nextDay, List<FileInfo> flst)
    {
        return generateDayPage(day, prevDay, nextDay, flst, 5);
    }

    public static String generateDayPage(String day, String prevDay,
            String nextDay, List<FileInfo> flst, int rowCount)
    {
        if (flst == null || flst.isEmpty())
        {
            return generate404Notfound();
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getHtmlHead());
        String yearNavigate = genYearNavigate();
        sb.append(yearNavigate);
        sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
        String dayNavigate = genDayNavigate(day, prevDay, nextDay);
        sb.append(dayNavigate);
        int i = 0;
        int start = 0;
        int end = 0;
        for (FileInfo f : flst)
        {
            if (i % rowCount == 0)
            {
                start++;
                sb.append("<tr>");
            }

            sb.append(
                    "<td width=\"20%\" height=\"18%\" bordercolor=\"#000000\">");
            sb.append("<a href=\"/photos/" + f.getHash256()
                    + "\" target=\"_blank\">");
            sb.append("<img " + (restrictSize(f) ? "width" : "height")
                    + "=\"340px\" src = \"/photos/" + f.getHash256()
                    + "?content=true&size=340" + "\"></img>" + "</a></td>");

            if ((i + 1) % rowCount == 0)
            {
                end++;
                sb.append("</tr>");
            }

            i++;
        }

        if (end != start)
        {
            sb.append("</tr>");
        }
        sb.append(dayNavigate);
        sb.append("</table>");
        sb.append(yearNavigate);
        sb.append(getHtmlFoot());

        return sb.toString();
    }

    private static String genDayNavigate(String day, String prevDay,
            String nextDay)
    {
        StringBuffer dayNavigate = new StringBuffer();
        dayNavigate.append("<tr><td width=\"20%\">");
        dayNavigate.append("<a href=\"/month/" + day.substring(0, 6) + "\">返回"
                + day.substring(0, 4) + "年" + day.substring(4, 6) + "月</a>");
        dayNavigate.append("</td><td width=\"20%\">");
        if (StringUtils.isNotBlank(prevDay))
        {
            dayNavigate.append("<a href=\"/day/" + prevDay + "\">" + "上一天</a>");
        }
        dayNavigate.append("</td>");
        dayNavigate.append("<td  width=\"20%\" style=\"text-align:center\">"
                + day + "</td><td width=\"20%\">");
        if (StringUtils.isNotBlank(nextDay))
        {
            dayNavigate.append("<a href=\"/day/" + nextDay + "\">" + "下一天</a>");
        }
        dayNavigate.append("</td><td width=\"20%\"></td></tr>");
        return dayNavigate.toString();
    }

    public static Object generateMonthPage(String monthstr, String nextMonth,
            String prevMonth, TreeMap<String, DateRecords> map)
    {
        if (map == null || map.isEmpty())
        {
            return generate404Notfound();
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getHtmlHead());
        String yearNavigate = genYearNavigate();
        sb.append(yearNavigate);
        String monthNavigate = genMonthNavigate(monthstr, nextMonth, prevMonth);
        sb.append(monthNavigate);
        sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

        int i = 0;
        int start = 0;
        int end = 0;

        for (String day : getSortedKeyList(map.keySet()))
        {
            DateRecords mr = map.get(day);
            String pic = mr.getFirstpic();

            if (i % 4 == 0)
            {
                start++;
                sb.append("<tr>");
            }
            FileInfo f = UniqPhotosStore.getInstance().getOneFileByHashStr(pic);
            if (f != null)
            {
                sb.append(
                        "<td width=\"25%\" height=\"31%\" bordercolor=\"#000000\">");
                sb.append("<a href=\"/day/" + monthstr + day + " \" >");
                sb.append("<img " + (restrictSize(f) ? "width" : "height")
                        + "=\"280px\" src = \"/photos/" + pic
                        + "?content=true&size=280" + "\"></img>"
                        + "</a><br/><b>" + monthstr + "-" + day + " ("
                        + mr.getPiccount() + "张)</b></td>");
            }
            if ((i + 1) % 4 == 0)
            {
                end++;
                sb.append("</tr>");
            }

            i++;
        }

        if (end != start)
        {
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append(monthNavigate);
        sb.append(yearNavigate);
        sb.append(getHtmlFoot());

        return sb.toString();
    }

    private static String genMonthNavigate(String monthstr, String nextMonth,
            String prevMonth)
    {
        StringBuffer monthNavigate = new StringBuffer();
        monthNavigate
                .append("<table style=\"text-align: center;\" width=\"100%\" "
                        + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
        monthNavigate.append("<tr>" + "<td width=\"20%\">");
        monthNavigate.append("<a href=\"/year/" + monthstr.substring(0, 4)
                + "\">返回" + monthstr.substring(0, 4) + "年</a>");
        monthNavigate.append("</td>" + "<td width=\"20%\">");
        if (StringUtils.isNotBlank(prevMonth))
        {
            monthNavigate.append(
                    "<a href=\"/month/" + prevMonth + "\">" + "上一月</a>");
        }
        monthNavigate.append("</td>");
        monthNavigate.append("<td  width=\"20%\" style=\"text-align:center\">"
                + monthstr + "月" + "</td><td width=\"20%\">");
        if (StringUtils.isNotBlank(nextMonth))
        {
            monthNavigate.append(
                    "<a href=\"/month/" + nextMonth + "\">" + "下一月</a>");
        }
        monthNavigate.append(
                "</td>" + "<td width=\"20%\"></td>" + "</tr>" + "</table>");
        return monthNavigate.toString();
    }

    public static String generate404Notfound()
    {
        return getHtmlHead() + "404 not found" + getHtmlFoot();
    }

    public static boolean restrictSize(FileInfo f)
    {
        if (f.getHeight() > 0 && f.getWidth() > 0)
        {
            double rate = (((double) f.getWidth()) / f.getHeight());
            if (rate > 1)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        return true;
    }

}

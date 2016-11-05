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

import com.backend.FileInfo;
import com.backend.dao.DateRecords;
import com.backend.dao.DateTableDao;
import com.backend.dao.UniqPhotosStore;
import com.utils.media.MediaTool;;

public class GenerateHTML
{
    private static final String seprator = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

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
        if (!HeadUtils.isMobile())
        {
            sb.append(yearNavigate);
        }
        String indexPageNavi = genIndexNavigate(firstP, endP);
        sb.append(indexPageNavi);
        sb.append("<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" "
                + "border=\"0\" bordercolor=\"#000000\">");
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

            sb.append("<td width=\"20%\" height=\"18%\" bordercolor=\"#000000\"><br/>");
            sb.append("<a href=\"/photos/" + f.getHash256() + videoTab(true) + "\">");
            sb.append(generateImgTag(f, 310));
            sb.append("</a></td>");

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

    private static String videoTab(boolean isFirst)
    {
        return HeadUtils.isVideo() ? ((isFirst ? "?" : "&") + "video=true") : "";
    }

    private static String genIndexNavigate(FileInfo firstP, FileInfo endP)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" "
                + "border=\"0\" bordercolor=\"#000000\">");

        String prevPage = getPhotoLink(firstP, false);
        String nextPage = getPhotoLink(endP, true);
        String photoTime = firstP.getPhotoTime() + " ~ " + endP.getPhotoTime();

        sb.append("<tr><td width=\"100%\" bordercolor=\"#000000\">");
        sb.append(prevPage + seprator);
        sb.append(photoTime + seprator);
        sb.append(nextPage);
        sb.append("</td></tr></table>");

        return sb.toString();
    }

    private static String getPhotoLink(FileInfo f, boolean isNext)
    {
        return getPhotoLink(f, HeadUtils.getMaxCountOfOnePage(), isNext);
    }

    private static String getPhotoLink(FileInfo f, int count, boolean isNext)
    {
        String value = isNext ? "下" : "上";
        value += "一";
        value += (count == 1 ? "张" : "页");

        String prevPage = "<a href=\"" + getPhotoUrl(f, count, isNext) + "\"><input value=\""
                + value + "\" type=\"button\"/></a>";

        return prevPage;
    }

    private static String getPhotoUrl(FileInfo f, int count, boolean isNext)
    {
        String pPara = isNext ? "next" : "prev";

        return "/?" + pPara + "=" + f.getHash256() + "&count=" + count + videoTab(false);
    }

    public static String genYearNavigate()
    {
        TreeMap<String, TreeMap<String, TreeMap<String, DateRecords>>> allrecords = DateTableDao
                .getInstance().getAllDateRecord();
        if (allrecords != null && !allrecords.isEmpty())
        {
            StringBuffer ylst = new StringBuffer(
                    "<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" "
                            + "border=\"0\" bordercolor=\"#000000\">");

            int i = 0;
            int start = 0;
            int end = 0;

            List<String> arrayList = getSortedKeyList(allrecords.keySet());

            final int rowcount = (HeadUtils.isMobile() ? 8 : 20);

            for (String f : arrayList)
            {
                if (i % rowcount == 0)
                {
                    start++;
                    ylst.append("<tr>");
                }

                if (i == 0)
                {
                    ylst.append("<td width=\"5%\" height=\"30px\" bordercolor=\"#000000\">");
                    ylst.append("<a href=\"/\"><input type=\"button\" value=\"首页\"/></a></td>");
                    i++;

                    ylst.append("<td width=\"5%\" height=\"30px\" bordercolor=\"#000000\">");
                    ylst.append(
                            "<a href=\"/?video=true\"><input type=\"button\" value=\"视频\"/></a></td>");
                    i++;

                    if (HeadUtils.isVideo())
                    {
                        break;
                    }
                }

                ylst.append("<td width=\"5%\" bordercolor=\"#000000\">");
                ylst.append("<a href=\"/year/" + f + "\" >" + "<input type=\"button\" value=\"" + f
                        + "\"/></a></td>");

                if ((i + 1) % rowcount == 0)
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
        return getHtmlHead(false);
    }

    public static String getHtmlHead(boolean isSinglePage)
    {
        String hh = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">"
                + "<head profile=\"http://gmpg.org/xfn/11\">"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/> "
                + "<script type=\"text/javascript\" src=\"/js/jquery-3.1.0.js\"></script>";

        if (HeadUtils.isMobile())
        {
            hh += "<link href=\"/js/default_mobile.css\" rel=\"stylesheet\" "
                    + "type=\"text/css\" media=\"screen\" />";

        }
        else
        {
            hh += "<link href=\"/js/default.css\" rel=\"stylesheet\" "
                    + "type=\"text/css\" media=\"screen\" />";
        }

        if (isSinglePage)
        {
            hh += "<script type=\"text/javascript\" src=\"/js/jQueryRotate.js\"></script>";
            hh += "<script type=\"text/javascript\" src=\"/js/jquery.alerts.js\"></script>";
            hh += "<script type=\"text/javascript\" src=\"/js/navigate.js\"></script>";
            if (HeadUtils.isMobile())
            {
                hh += "<script type=\"text/javascript\" src=\"/js/jquery.touchSwipe.min.js\"></script>";
            }
        }

        hh += "<title>相册</title></head><body>";
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
            StringBuffer sb = new StringBuffer(getHtmlHead(true));
            String yearNavigage = genYearNavigate();
            if (!HeadUtils.isMobile())
            {
                sb.append(yearNavigage);
            }
            // 隐藏照片
            sb.append("<script type=\"text/javascript\">"
                    + "function changeUrl(url){window.history.pushState({},0,'http://'+window.location.host+'/'+url);}"
                    + "window.onload=changeUrl(" + "'photos/" + f.getHash256() + videoTab(true)
                    + "');"
                    + "function deletephoto(path){jConfirm('该操作将永久隐藏照片，无法撤消，确认是否继续？','确认',function(r){if (r){"
                    + "changeUrl('?next=" + f.getHash256() + videoTab(false) + "&count=1');"
                    + "window.location.reload();" + "$.ajax({url:path,type:'DELETE',"
                    + "success:function(result){}});}});}");

            if (HeadUtils.isMobile())
            {
                // 滑动翻页
                sb.append("$(function() {");
                sb.append("$(\"#singlephoto\").swipe({");
                sb.append("swipe: function(event, direction, distance, duration, fingerCount) {");
                sb.append("if (distance>=30){if (direction=='right'){top.location=" + "'"
                        + getPhotoUrl(f, 1, false) + "';}else if (direction=='left') {top.location="
                        + "'" + getPhotoUrl(f, 1, true) + "';" + "}");
                sb.append("}},});});");
                /*
                 * sb.append("$(document).ready(function(){"); sb.append(
                 * "$(\"#singlephoto\").on(\"swipeleft\",function(){top.location="
                 * + "'" + getPhotoUrl(f, 1, false) + "'" + ";});" +
                 * "$(\"#singlephoto\").on(\"swiperight\",function(){top.location="
                 * + "'" + getPhotoUrl(f, 1, true) + "'" + ";});");
                 * sb.append("});");
                 */
            }
            else
            {
                // 键盘翻页。
                sb.append("$(document).ready(function(){");
                sb.append("$(\"body\").keyup(function(event){");
                sb.append("if(event.keyCode==37)top.location=" + "'" + getPhotoUrl(f, 1, false)
                        + "';");
                sb.append("if(event.keyCode==39)top.location=" + "'" + getPhotoUrl(f, 1, true)
                        + "';");
                sb.append("});});");
            }

            sb.append("</script>");

            sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                    + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

            String dayStr = HeadUtils.formatDate(f.getPhotoTime());
            String viewDayStr = String.format("%s年%s月%s日", dayStr.substring(0, 4),
                    dayStr.substring(4, 6), dayStr.substring(6, 8));
            String returnToDayPage = "<a href=\"/day/" + dayStr + videoTab(true) + "\">浏览 <b>"
                    + viewDayStr + "</b></a>";
            sb.append("<tr><td width=\"100%\" bordercolor=\"#000000\">");
            sb.append(returnToDayPage + seprator);
            sb.append(getPhotoLink(f, false) + seprator);
            sb.append(getPhotoLink(f, 1, false));
            sb.append("&nbsp;" + f.getPhotoTime() + "&nbsp;");
            sb.append(getPhotoLink(f, 1, true) + seprator);
            sb.append(getPhotoLink(f, true));
            sb.append("</td></tr>");
            sb.append("</table>");

            sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                    + "height=\"890px\" border=\"0\" bordercolor=\"#000000\">");

            sb.append("<tr><td width=\"100%\" height=\"100%\" bordercolor=\"#000000\">");
            // sb.append("<a href=\"/photos/" + f.getHash256() + "?content=true"
            // + "\" target=\"_blank\">");

            String extraInfo = "";
            if (!HeadUtils.isMobile())
            {
                extraInfo = "onmouseover=\"upNext(this" + "," + "'" + getPhotoUrl(f, 1, false) + "'"
                        + "," + "'" + getPhotoUrl(f, 1, true) + "'" + ")\"";
            }

            sb.append(generateImgTag(f, 860, extraInfo, "singlephoto"));
            // sb.append("</a>");
            sb.append("</td></tr>");

            sb.append("<tr><td width=\"100%\" height=\"100%\" bordercolor=\"#000000\">");

            String leftRotateLink = "<input id=\"leftrotate\" type=\"button\" value=\"左旋转\" />";
            String rightRotateLink = "<input id=\"rightrotate\" type=\"button\" value=\"右旋转\" />";
            String deleteLink = "<input id=\"deletephotob\" type=\"button\" value=\"隐藏\" />";

            sb.append(leftRotateLink + seprator);
            sb.append(deleteLink + seprator);
            sb.append(rightRotateLink + seprator);
            sb.append("</td><tr>");

            int r = 0;

            if (HeadUtils.needRotatePic(f))
            {
                r = f.getRoatateDegree() / 90;
            }

            // 左旋转和右旋转照片
            sb.append("<script type=\"text/javascript\">" + "var r = " + r + ";"
                    + "$(\"#rightrotate\").click(function(){r++; if (r > 3){r = 0;}"
                    + "$(\"#singlephoto\").rotate(90*r);}); "
                    + "$(\"#leftrotate\").click(function(){r--; if (r < 0){ r = 3;}"
                    + "$(\"#singlephoto\").rotate(90*r);}); "
                    + "$(\"#deletephotob\").click(function(){deletephoto('/photos/" + f.getHash256()
                    + "');});" + "</script>");

            sb.append("</table>");

            sb.append(yearNavigage);

            sb.append(getHtmlFoot());
            return sb.toString();
        }

    }

    private static String generateImgTag(FileInfo f, int size)
    {
        return generateImgTag(f, size, "", "");
    }

    private static String generateImgTag(FileInfo f, int size, String exinfo, String id)
    {
        String content = null;
        if (MediaTool.isVideo(f.getPath()))
        {
            String video = "<video ";
            if (f.getHeight() > size || f.getWidth() > size)
            {
                boolean isWidth = (f.getRoatateDegree() == 0 || f.getRoatateDegree() == 180)
                        && restrictSize(f);
                video += " " + (isWidth ? "width" : "height") + "=" + "\"" + size + "\"";
            }

            /*
             * if (HeadUtils.needRotatePic(f)) { video +=
             * " style=\"transform: rotate(" + f.getRoatateDegree() +
             * "deg); transform-origin: 50% 50% 0px;\""; }
             */

            video += " controls=\"controls\">";

            video += "<source";
            video += " src = \"/photos/" + f.getHash256() + "?content=true&size=" + size + "\""
                    + "type=\"" + HeadUtils.judgeMIME(f.getPath()) + "\"/>";
            video += "Your browser does not support the video tag.</video>";
            content = video;
        }
        else
        {
            String img = "<img";
            img += " " + exinfo;
            if (StringUtils.isNotBlank(id))
            {
                img += " id=\"" + id + "\"";
            }
            if (f.getHeight() > size || f.getWidth() > size)
            {
                img += " " + (restrictSize(f) ? "width" : "height") + "=" + "\"" + size + "px\"";
            }
            if (HeadUtils.needRotatePic(f))
            {
                img += " style=\"transform: rotate(" + f.getRoatateDegree()
                        + "deg); transform-origin: 50% 50% 0px;\"";
            }
            img += " src = \"/photos/" + f.getHash256() + "?content=true&size=" + size + "\">";
            img += "</img>";
            content = img;
        }
        return content;
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
        if (!HeadUtils.isMobile())
        {
            sb.append(yearNavigete);
        }
        sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

        int i = 0;
        int start = 0;
        int end = 0;

        final int rowcount = (HeadUtils.isMobile() ? 3 : 4);

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

            if (i % rowcount == 0)
            {
                start++;
                sb.append("<tr>");
            }

            FileInfo f = UniqPhotosStore.getInstance().getOneFileByHashStr(pic);
            if (f != null)
            {
                sb.append("<td width=\"25%\" height=\"31%\" bordercolor=\"#000000\">");
                sb.append("<a href=\"/month/" + year + mo + "\" >");
                sb.append(generateImgTag(f, 310));
                sb.append("</a><br/><b>" + year + "-" + mo + "月份 (" + filecount + "张)</b></td>");
            }
            if ((i + 1) % rowcount == 0)
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

    public static String generateDayPage(String day, String prevDay, String nextDay,
            List<FileInfo> flst)
    {
        return generateDayPage(day, prevDay, nextDay, flst, 5);
    }

    public static String generateDayPage(String day, String prevDay, String nextDay,
            List<FileInfo> flst, int rowCount)
    {
        if (flst == null || flst.isEmpty())
        {
            return generate404Notfound();
        }

        if (flst.size() == 1)
        {
            return generateSinglePhoto(flst.get(0));
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getHtmlHead());

        String yearNavigate = genYearNavigate();
        if (!HeadUtils.isMobile())
        {
            sb.append(yearNavigate);
        }
        String dayNavigate = genDayNavigate(day, prevDay, nextDay);

        sb.append(dayNavigate);

        sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
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

            sb.append("<td width=\"20%\" height=\"18%\" bordercolor=\"#000000\">");
            sb.append("<a href=\"/photos/" + f.getHash256() + videoTab(true) + "\">");
            sb.append(generateImgTag(f, 310));
            sb.append("</a></td>");

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

        sb.append(dayNavigate);

        sb.append(yearNavigate);

        sb.append(getHtmlFoot());

        return sb.toString();
    }

    private static String genDayNavigate(String day, String prevDay, String nextDay)
    {
        StringBuffer dayNavigate = new StringBuffer();
        dayNavigate.append("<table style=\"text-align: center;\" width=\"100%\" "
                + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

        dayNavigate.append("<tr><td width=\"20%\">");
        dayNavigate.append("<a href=\"/month/" + day.substring(0, 6) + "\">返回" + day.substring(0, 4)
                + "年" + day.substring(4, 6) + "月</a></td>");

        dayNavigate.append("<td  width=\"20%\" style=\"text-align:center\">");
        if (StringUtils.isNotBlank(prevDay))
        {
            dayNavigate.append("<a href=\"/day/" + prevDay + "\">"
                    + "<input value=\"上一天\" type=\"button\"/></a>");
        }
        dayNavigate.append("</td>");

        dayNavigate.append("<td  width=\"20%\" style=\"text-align:center\">" + day + "</td>");

        dayNavigate.append("<td  width=\"20%\" style=\"text-align:center\">");
        if (StringUtils.isNotBlank(nextDay))
        {
            dayNavigate.append("<a href=\"/day/" + nextDay + "\">"
                    + "<input value=\"下一天\" type=\"button\"/></a>");
        }
        dayNavigate.append("</td>");

        dayNavigate.append("<td width=\"20%\"></td>");

        dayNavigate.append("</tr>");

        dayNavigate.append("</table>");
        return dayNavigate.toString();
    }

    public static Object generateMonthPage(String monthstr, String nextMonth, String prevMonth,
            TreeMap<String, DateRecords> map)
    {
        if (map == null || map.isEmpty())
        {
            return generate404Notfound();
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getHtmlHead());
        String yearNavigate = genYearNavigate();
        if (!HeadUtils.isMobile())
        {
            sb.append(yearNavigate);
        }
        String monthNavigate = genMonthNavigate(monthstr, nextMonth, prevMonth);
        sb.append(monthNavigate);
        sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

        int i = 0;
        int start = 0;
        int end = 0;

        final int rowcount = HeadUtils.isMobile() ? 3 : 5;

        for (String day : getSortedKeyList(map.keySet()))
        {
            DateRecords mr = map.get(day);
            String pic = mr.getFirstpic();

            if (i % rowcount == 0)
            {
                start++;
                sb.append("<tr>");
            }
            FileInfo f = UniqPhotosStore.getInstance().getOneFileByHashStr(pic);
            if (f != null)
            {
                sb.append("<td width=\"25%\" height=\"31%\" bordercolor=\"#000000\">");
                sb.append("<a href=\"/day/" + monthstr + day + " \" >");
                sb.append(generateImgTag(f, 310));
                sb.append("</a><br/><b>" + monthstr + "-" + day + " (" + mr.getPiccount()
                        + "张)</b></td>");
            }
            if ((i + 1) % rowcount == 0)
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

    private static String genMonthNavigate(String monthstr, String nextMonth, String prevMonth)
    {
        StringBuffer monthNavigate = new StringBuffer();
        monthNavigate.append("<table style=\"text-align: center;\" width=\"100%\" "
                + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
        monthNavigate.append("<tr>" + "<td width=\"20%\">");
        monthNavigate.append("<a href=\"/year/" + monthstr.substring(0, 4) + "\">返回"
                + monthstr.substring(0, 4) + "年</a>");
        monthNavigate.append("</td>" + "<td width=\"20%\">");
        if (StringUtils.isNotBlank(prevMonth))
        {
            monthNavigate.append("<a href=\"/month/" + prevMonth + "\">"
                    + "<input value=\"上一月\" type=\"button\"/></a>");
        }
        monthNavigate.append("</td>");
        monthNavigate.append("<td  width=\"20%\" style=\"text-align:center\">" + monthstr + "月"
                + "</td><td width=\"20%\">");
        if (StringUtils.isNotBlank(nextMonth))
        {
            monthNavigate.append("<a href=\"/month/" + nextMonth + "\">"
                    + "<input value=\"下一月\" type=\"button\"/></a>");
        }
        monthNavigate.append("</td>" + "<td width=\"20%\"></td>" + "</tr>" + "</table>");
        return monthNavigate.toString();
    }

    public static String generate404Notfound()
    {
        return getHtmlHead() + "404 not found" + getHtmlFoot();
    }

    public static boolean restrictSize(FileInfo f)
    {
        boolean ret = true;
        if (f.getHeight() > 0 && f.getWidth() > 0)
        {
            double rate = (((double) f.getWidth()) / f.getHeight());
            if (rate > 1)
            {
                ret = true;
            }
            else
            {
                ret = false;
            }
        }

        // if (f.getRoatateDegree() == 90 || f.getRoatateDegree() == 270)
        // {
        // ret = !ret;
        // }

        return ret;
    }

}

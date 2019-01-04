package com.utils.web;

import com.backend.dao.DateRecords;
import com.backend.dao.DateTableDao;
import com.backend.dao.UniqPhotosStore;
import com.backend.entity.FileInfo;
import com.backend.facer.Face;
import com.utils.conf.AppConfig;
import com.utils.media.MediaTool;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

public class GenerateHTML
{
    private static final String separator =
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

    private static final Logger logger = LoggerFactory.getLogger(GenerateHTML.class);

    public static String genIndexPage(List<?> flst, int rowCount, boolean needNavigate)
    {
        if (flst == null || flst.isEmpty() || rowCount <= 0)
        {
            return generate404Notfound();
        }

        if (flst.size() == 1)
        {
            Object o = flst.get(0);
            if (o instanceof Face)
            {
                return generateSinglePhoto((Face) flst.get(0));
            }
            else if (o instanceof FileInfo)
            {
                return generateSinglePhoto((FileInfo) o, false);
            }
            else
            {
                return generate404Notfound();
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getHtmlHead());

        String yearNavigate = genYearNavigate();
        if (!HeadUtils.isMobile())
        {
            sb.append(yearNavigate);
        }

        String indexPageNavi = "";
        if (needNavigate)
        {
            indexPageNavi = genIndexNavigate(flst.get(0), flst.get(flst.size() - 1));
            sb.append(indexPageNavi);
        }

        sb.append("<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" "
                          + "border=\"0\" bordercolor=\"#000000\">");
        int i = 0;
        int start = 0;
        int end = 0;
        for (Object f : flst)
        {

            if (i % rowCount == 0)
            {
                start++;
                sb.append("<tr>");
            }

            sb.append("<td width=\"20%\" height=\"18%\" bordercolor=\"#000000\"><br/>");
            if (f instanceof FileInfo)
            {
                FileInfo fi = (FileInfo) f;
                sb.append("<a href=\"" + "/photos/" + fi.getHash256() + extraQueryParas(true)
                                  + "\">");
                sb.append(generateImgTag(fi, 310));
            }
            else if (f instanceof Face)
            {
                Face face = (Face) f;
                FileInfo fi = face.getFi();
                if (fi == null)
                {
                    fi = UniqPhotosStore.getInstance().getOneFileByHashStr(((Face) f).getEtag());
                }

                if (fi != null)
                {
                    face.setFi(fi);
                    if (HeadUtils.isNoFaces())
                    {
                        sb.append(
                                "<a href=\"" + "/photos/" + fi.getHash256() + extraQueryParas(true)
                                        + "\">");
                        sb.append(generateImgTag(fi, 310));
                    }
                    else
                    {
                        sb.append("<a href=\"" + "/facetoken/" + face.getFacetoken()
                                          + extraQueryParas(true) + "\">");
                        sb.append(generateImgTag(face, false));
                    }

                }
            }
            else
            {
                logger.error("unknown file info type: {}", f);
            }
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

        if (needNavigate)
        {
            sb.append(indexPageNavi);
        }

        sb.append(yearNavigate);
        sb.append(getHtmlFoot());
        logHtml(sb);
        return sb.toString();
    }

    private static String extraQueryParas(boolean isFirst)
    {
        String tag = "";
        if (HeadUtils.isVideo() || HeadUtils.isFaces() || HeadUtils.isNoFaces())
        {
            tag += (isFirst ? "?" : "&");

            if (HeadUtils.isNoFaces())
            {
                tag += "noface=true&";
            }

            if (HeadUtils.isVideo())
            {
                tag += "video=true&";
            }

            if (HeadUtils.isFaces())
            {
                tag += "face=true&";
            }
        }

        if (tag.endsWith("&"))
        {
            // "hamburger".substring(0, 8) returns "hamburge"
            tag = tag.substring(0, tag.length() - 1);
        }

        return tag;
    }

    private static String genIndexNavigate(Object firstP, Object endP)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" "
                          + "border=\"0\" bordercolor=\"#000000\">");

        String prevPage = getPhotoLink(getID(firstP), false);
        String nextPage = getPhotoLink(getID(endP), true);

        String photoTime = "";
        FileInfo f1 = null;
        FileInfo f2 = null;
        if (firstP instanceof FileInfo && endP instanceof FileInfo)
        {
            f1 = ((FileInfo) firstP);
            f2 = ((FileInfo) endP);

        }
        else if (firstP instanceof Face && endP instanceof Face)
        {
            f1 = ((Face) firstP).getFi();
            f2 = ((Face) endP).getFi();
        }

        if (f1 != null && f2 != null)
        {
            photoTime = f1.getPhotoTime() + " ~ " + f2.getPhotoTime();
        }

        sb.append("<tr><td width=\"100%\" bordercolor=\"#000000\" " + getTdHeight() + ">");
        sb.append(prevPage + separator);
        sb.append(photoTime + separator);
        sb.append(nextPage);
        sb.append("</td></tr></table>");

        return sb.toString();
    }

    private static String getTdHeight()
    {
        return HeadUtils.isMobile() ? " height=\"180px\" " : " ";
    }

    private static String getID(Object o)
    {
        String id = "";
        if (o instanceof FileInfo)
        {
            id = ((FileInfo) o).getHash256();
        }
        else if (o instanceof Face)
        {
            if (HeadUtils.isNoFaces())
            {
                FileInfo fi = ((Face) o).getFi();
                if (fi == null)
                {
                    fi = UniqPhotosStore.getInstance().getOneFileByHashStr(((Face) o).getEtag());
                }

                if (fi != null)
                {
                    id = fi.getHash256();
                }
            }
            else
            {
                id = ((Face) o).getFacetoken();
            }
        }

        return id;
    }

    private static String getPhotoLink(String id, boolean isNext)
    {
        return getPhotoLink(id, HeadUtils.getMaxCountOfOnePage(), isNext);
    }

    private static String getPhotoLink(String id, int count, boolean isNext)
    {
        String value = isNext ? "下" : "上";
        value += "一";
        value += (count == 1 ? "张" : "页");

        return "<a href=\"" + getPhotoUrl(id, count, isNext) + "\"><input value=\"" + value
                + "\" type=\"button\"/></a>";

    }

    private static String getPhotoUrl(String id, int count, boolean isNext)
    {
        String pPara = isNext ? "next" : "prev";

        return "/?" + pPara + "=" + id + "&count=" + count + extraQueryParas(false);
    }

    private static String genYearNavigate()
    {
        TreeMap<String, TreeMap<String, TreeMap<String, DateRecords>>> allrecords =
                DateTableDao.getInstance().getAllDateRecord();
        if (allrecords != null && !allrecords.isEmpty())
        {
            StringBuilder ylst = new StringBuilder(
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

                    if (AppConfig.getInstance().isFacerConfigured())
                    {

                        ylst.append("<td width=\"5%\" height=\"30px\" bordercolor=\"#000000\">");
                        ylst.append(
                                "<a href=\"/faces/\"><input type=\"button\" value=\"人物\"/></a></td>");
                        i++;
                    }

                    if (AppConfig.getInstance().isVideoConfigured())
                    {
                        ylst.append("<td width=\"5%\" height=\"30px\" bordercolor=\"#000000\">");
                        ylst.append(
                                "<a href=\"/?video=true\"><input type=\"button\" value=\"视频\"/></a></td>");
                        i++;
                    }

                    if (AppConfig.getInstance().isFacerConfigured())
                    {
                        ylst.append("<td width=\"5%\" height=\"30px\" bordercolor=\"#000000\">");
                        ylst.append(
                                "<a href=\"/?noface=true\"><input type=\"button\" value=\"风景\"/></a></td>");
                        i++;
                    }

                    if (HeadUtils.isVideo() || HeadUtils.isFaces())
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

    private static String getHtmlHead()
    {
        return getHtmlHead(false);
    }

    private static String getHtmlHead(boolean isSinglePage)
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
                hh +=
                        "<script type=\"text/javascript\" src=\"/js/jquery.touchSwipe.min.js\"></script>";
            }
        }

        hh += "<title>相册</title></head><body>";
        return hh;
    }

    private static List<String> getSortedKeyList(Set<String> set)
    {
        List<String> arrayList = new LinkedList<>();
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

    private static String getHtmlFoot()
    {
        return genDownloadAPK() + getGAStr() + "</body></html>";
    }

    public static String generateSinglePhoto(FileInfo f, boolean isBackToMonth)
    {
        if (f == null)
        {
            return generate404Notfound();
        }

        StringBuffer sb = new StringBuffer(getHtmlHead(true));
        String yearNavigage = genYearNavigate();
        if (!HeadUtils.isMobile())
        {
            sb.append(yearNavigage);
        }
        // 隐藏照片
        sb.append("<script type=\"text/javascript\">"
                          + "function changeUrl(url){window.history.pushState({},0,'//'+window.location.host+'/'+url);}"
                          + "window.onload=changeUrl(" + "'photos/" + f.getHash256()
                          + extraQueryParas(true) + "');"
                          + "function deletephoto(path){jConfirm('该操作将永久隐藏照片，无法撤消，确认是否继续？','确认',function(r){if (r){"
                          + "changeUrl('?next=" + f.getHash256() + extraQueryParas(false)
                          + "&count=1');" + "window.location.reload();"
                          + "$.ajax({url:path,type:'DELETE',"
                          + "success:function(result){}});}});}");

        if (HeadUtils.isMobile())
        {
            // 滑动翻页
            sb.append("$(function() {");
            sb.append("$(\"#singlephoto\").swipe({");
            sb.append("swipe: function(event, direction, distance, duration, fingerCount) {");
            sb.append("if (distance>=30){if (direction=='right'){top.location=" + "'" + getPhotoUrl(
                    f.getHash256(), 1, false) + "';}else if (direction=='left') {top.location="
                              + "'" + getPhotoUrl(f.getHash256(), 1, true) + "';" + "}");
            sb.append("}},});});");
            /*
             * sb.append("$(document).ready(function(){"); sb.append(
             * "$(\"#singlephoto\").on(\"swipeleft\",function(){top.location=" +
             * "'" + getPhotoUrl(f, 1, false) + "'" + ";});" +
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
            sb.append("if(event.keyCode==37)top.location=" + "'" + getPhotoUrl(f.getHash256(), 1,
                                                                               false) + "';");
            sb.append("if(event.keyCode==39)top.location=" + "'" + getPhotoUrl(f.getHash256(), 1,
                                                                               true) + "';");
            sb.append("if(event.keyCode==46)deletephoto('/photos/" + f.getHash256() + "');");
            sb.append("});});");
        }

        sb.append("</script>");

        sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                          + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

        String fullDayStr = HeadUtils.formatDate(f.getPhotoTime());
        String monStr = fullDayStr.substring(4, 6);
        String yearStr = fullDayStr.substring(0, 4);
        String dayStr = fullDayStr.substring(6, 8);
        String viewDayStr = isBackToMonth ? String.format("%s年%s月", yearStr, monStr)
                                          : String.format("%s年%s月%s日", yearStr, monStr, dayStr);
        String returnToDayPage;
        if (isBackToMonth)
        {
            returnToDayPage =
                    "<a href=\"/month/" + yearStr + monStr + extraQueryParas(true) + "\">浏览 <b>"
                            + viewDayStr + "</b></a>";
        }
        else
        {
            returnToDayPage = "<a href=\"/day/" + fullDayStr + extraQueryParas(true) + "\">浏览 <b>"
                    + viewDayStr + "</b></a>";
        }

        sb.append("<tr><td width=\"100%\" bordercolor=\"#000000\"" + getTdHeight() + ">");
        sb.append(returnToDayPage + separator);
        sb.append(getPhotoLink(f.getHash256(), false) + separator);
        sb.append(getPhotoLink(f.getHash256(), 1, false));
        sb.append("&nbsp;" + f.getPhotoTime() + "&nbsp;");
        sb.append(getPhotoLink(f.getHash256(), 1, true) + separator);
        sb.append(getPhotoLink(f.getHash256(), true));
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
            extraInfo =
                    "onmouseover=\"upNext(this" + "," + "'" + getPhotoUrl(f.getHash256(), 1, false)
                            + "'" + "," + "'" + getPhotoUrl(f.getHash256(), 1, true) + "'" + ")\"";
        }

        sb.append(generateImgTag(f, 860, extraInfo, "singlephoto", false));
        // sb.append("</a>");
        sb.append("</td></tr>");

        sb.append("<tr><td width=\"100%\" " + getTdHeight() + " bordercolor=\"#000000\""
                          + getTdHeight() + ">");

        String leftRotateLink = "<input id=\"leftrotate\" type=\"button\" value=\"左旋转\" />";
        String rightRotateLink = "<input id=\"rightrotate\" type=\"button\" value=\"右旋转\" />";


        sb.append(leftRotateLink + separator);

        if (HeadUtils.isSuperLogin() || HeadUtils.isLocalLogin())
        {
            String deleteLink = "<input id=\"deletephotob\" type=\"button\" value=\"隐藏\" />";
            sb.append(deleteLink + separator);
        }
        else
        {
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        }

        sb.append(rightRotateLink + separator);
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
                          + "$(\"#deletephotob\").click(function(){deletephoto('/photos/" + f
                .getHash256() + "');});" + "</script>");

        sb.append("</table>");

        sb.append(yearNavigage);

        sb.append(getHtmlFoot());

        logHtml(sb);
        return sb.toString();
    }

    private static String generateImgTag(FileInfo f, int size)
    {
        return generateImgTag(f, size, "", "", false);
    }

    private static String generateImgTag(FileInfo f, int size, String exinfo, String id,
                                         boolean isFace)
    {
        String content;
        if (MediaTool.isVideo(f.getPath()) && size > 400)
        {
            // 非缩略图 视频。
            String video = "<video ";
            if (f.getHeight() > size || f.getWidth() > size || f.getHeight() == 0
                    || f.getWidth() == 0)
            {
                boolean isWidth = (f.getRoatateDegree() % 180 == 0) && restrictSize(f);
                video += " " + (isWidth ? "width" : "height") + "=" + "\"" + size + "\"";
            }

            /*
             * if (HeadUtils.needRotatePic(f)) { video +=
             * " style=\"transform: rotate(" + f.getRoatateDegree() +
             * "deg); transform-origin: 50% 50% 0px;\""; }
             */

            video += " controls=\"controls\" autoplay=\"autoplay\">";

            video += "<source";
            video += " src = \"/photos/" + f.getHash256() + "?content=true&size=" + size + "\""
                    + "type=\"" + HeadUtils.judgeMIME(f.getPath()) + "\"/>";
            video += "Your browser does not support the video tag.</video>";
            content = video;
        }
        else
        {
            boolean isVideo = MediaTool.isVideo(f.getPath());
            boolean needRoatate = HeadUtils.needRotatePic(f);
            boolean needRestrict = restrictSize(f);
            String img = "";
            if (size < 400 && needRoatate)
            {
                String style = "width:" + size + "px;height: " + (isVideo ? size + 20 : size)
                        + "px;text-align: center;vertical-align: middle;display: table-cell;";
                img += "<dvi style=" + "\"" + style + "\">";
            }
            img += "<img";
            img += " " + exinfo;
            if (StringUtils.isNotBlank(id))
            {
                img += " id=\"" + id + "\"";
            }

            if (isVideo)
            {
                // 视频缩略图 不需要旋转，在视频角度不正确时才需要颠倒长和高。
                if (f.getHeight() > size || f.getWidth() > size || f.getHeight() == 0
                        || f.getWidth() == 0)
                {
                    boolean isWidth = (f.getRoatateDegree() % 180 == 0) && needRestrict;
                    img += " " + (isWidth ? "width" : "height") + "=" + "\"" + size + "px\"";
                }
            }
            else
            {
                // 普通照片
                if (f.getHeight() > size || f.getWidth() > size || f.getHeight() == 0
                        || f.getWidth() == 0)
                {
                    img += " " + (needRestrict ? "width" : "height") + "=" + "\"" + size + "px\"";
                }

                if (needRoatate)
                {
                    img += " style=\"transform: rotate(" + f.getRoatateDegree()
                            + "deg); transform-origin: 50% 50% 0px;\"";
                }
            }

            DecimalFormat digitFormat = new DecimalFormat("###,###");
            String title = String.format(" title=\"size: %s&#13;path: %s\" ",
                                         digitFormat.format(f.getSize()), f.getPath());

            img += " src = \"/photos/" + f.getHash256() + "?content=true&size=" + size + (isFace
                                                                                          ? "isface=true"
                                                                                          : "")
                    + "\"" + title + ">";
            img += "</img>";

            if (isVideo && !HeadUtils.isVideo())
            {
                // img += "</br><span style=\"text-align: center;font-family:
                // sans-serif;font-weight: bold;\">"
                // + "视频</span>";
                img +=
                        "</br><img width=\"20px\" style=\"padding: 2px 0 0px 0;\" src=\"/js/player.png\">";
            }

            if (size < 400 && needRoatate)
            {
                img += "</div>";
            }

            content = img;

        }
        return content;
    }

    public static String generateYearPage(String year,
                                          TreeMap<String, TreeMap<String, DateRecords>> currentyear)
    {
        if (StringUtils.isBlank(year) || currentyear == null || currentyear.isEmpty())
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
        logHtml(sb);
        return sb.toString();
    }

    public static String generateDayPage(String day, String prevDay, String nextDay,
                                         List<FileInfo> flst, int rowCount)
    {
        if (StringUtils.isBlank(day) || flst == null || flst.isEmpty() || rowCount <= 0)
        {
            return generate404Notfound();
        }

        if (flst.size() == 1)
        {
            return generateSinglePhoto(flst.get(0), true);
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
            sb.append("<a href=\"/photos/" + f.getHash256() + extraQueryParas(true) + "\">");
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
        logHtml(sb);
        return sb.toString();
    }

    private static String genDayNavigate(String day, String prevDay, String nextDay)
    {
        StringBuilder dayNavigate = new StringBuilder();
        dayNavigate.append("<table style=\"text-align: center;\" width=\"100%\" "
                                   + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

        dayNavigate.append("<tr><td width=\"20%\"" + getTdHeight() + ">");
        dayNavigate.append("<a href=\"/month/" + day.substring(0, 6) + "\">返回" + day.substring(0, 4)
                                   + "年" + day.substring(4, 6) + "月</a></td>");

        dayNavigate.append("<td  width=\"20%\" " + getTdHeight() + "style=\"text-align:center\">");
        if (StringUtils.isNotBlank(prevDay))
        {
            dayNavigate.append("<a href=\"/day/" + prevDay + "\">"
                                       + "<input value=\"上一天\" type=\"button\"/></a>");
        }
        dayNavigate.append("</td>");

        dayNavigate
                .append("<td  width=\"20%\" " + getTdHeight() + "style=\"text-align:center\">" + day
                                + "</td>");

        dayNavigate.append("<td  width=\"20%\" " + getTdHeight() + "style=\"text-align:center\">");
        if (StringUtils.isNotBlank(nextDay))
        {
            dayNavigate.append("<a href=\"/day/" + nextDay + "\">"
                                       + "<input value=\"下一天\" type=\"button\"/></a>");
        }
        dayNavigate.append("</td>");

        dayNavigate.append("<td width=\"20%\"" + getTdHeight() + "></td>");

        dayNavigate.append("</tr>");

        dayNavigate.append("</table>");
        return dayNavigate.toString();
    }

    public static Object generateMonthPage(String monthstr, String nextMonth, String prevMonth,
                                           TreeMap<String, DateRecords> map)
    {
        if (StringUtils.isBlank(monthstr) || map == null || map.isEmpty())
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
        logHtml(sb);
        return sb.toString();
    }

    private static String genMonthNavigate(String monthstr, String nextMonth, String prevMonth)
    {
        StringBuilder monthNavigate = new StringBuilder();
        monthNavigate.append("<table style=\"text-align: center;\" width=\"100%\" "
                                     + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");
        monthNavigate.append("<tr>" + "<td width=\"20%\" " + getTdHeight() + ">");
        monthNavigate.append("<a href=\"/year/" + monthstr.substring(0, 4) + "\">返回" + monthstr
                .substring(0, 4) + "年</a>");
        monthNavigate.append("</td>" + "<td width=\"20%\"" + getTdHeight() + ">");
        if (StringUtils.isNotBlank(prevMonth))
        {
            monthNavigate.append("<a href=\"/month/" + prevMonth + "\">"
                                         + "<input value=\"上一月\" type=\"button\"/></a>");
        }
        monthNavigate.append("</td>");
        monthNavigate.append("<td  width=\"20%\"" + getTdHeight() + " style=\"text-align:center\">"
                                     + monthstr + "月" + "</td><td width=\"20%\">");
        if (StringUtils.isNotBlank(nextMonth))
        {
            monthNavigate.append("<a href=\"/month/" + nextMonth + "\">"
                                         + "<input value=\"下一月\" type=\"button\"/></a>");
        }
        monthNavigate.append("</td>" + "<td width=\"20%\"" + getTdHeight() + "></td>" + "</tr>"
                                     + "</table>");
        return monthNavigate.toString();
    }

    public static String generate404Notfound()
    {
        return getHtmlHead() + "404 not found" + getHtmlFoot();
    }

    private static boolean restrictSize(FileInfo f)
    {
        boolean ret = true;
        if (f.getHeight() > 0 && f.getWidth() > 0)
        {
            double rate = (((double) f.getWidth()) / f.getHeight());
            ret = rate > 1;
        }

        // if (f.getRoatateDegree() == 90 || f.getRoatateDegree() == 270)
        // {
        // ret = !ret;
        // }

        return ret;
    }

    private static void logHtml(StringBuffer sb)
    {
        logHtml(sb, false);
    }

    private static void logHtml(StringBuffer sb, boolean forced)
    {
        if (forced)
        {
            logger.warn("html body: {}", sb);
        }
        else
        {
            logger.info("html body: {}", sb);
        }
    }

    private static String generateImgTag(Face f, boolean isThumbnail)
    {
        if (isThumbnail)
        {
            return "<img style=\"border-radius: 50%;\" width=\"200px\" height=\"200px\" src=\"/facetoken/"
                    + f.getFacetoken() + "?facethumbnail=true\"/>";
        }
        else
        {
            return generateImgTag(f.getFi(), 310);
        }
    }

    public static String genFaceIndexPage(List<Face> flst, int rowCount)
    {
        if (flst == null || flst.isEmpty() || rowCount <= 0)
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

        sb.append("<table style=\"text-align: center;\" width=\"100%\" height=\"100%\" "
                          + "border=\"0\" bordercolor=\"#000000\">");
        int i = 0;
        int start = 0;
        int end = 0;
        for (Face f : flst)
        {
            if (i % rowCount == 0)
            {
                start++;
                sb.append("<tr>");
            }

            sb.append("<td width=\"20%\" height=\"18%\" bordercolor=\"#000000\"><br/>");
            sb.append("<a href=\"" + "/faces/" + f.getFaceid() + "?facetoken=" + f.getFacetoken()
                              + extraQueryParas(false) + "\">");
            sb.append(generateImgTag(f, true));
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

        sb.append(yearNavigate);
        sb.append(getHtmlFoot());
        logHtml(sb);
        return sb.toString();
    }

    public static String generateSinglePhoto(Face f)
    {
        if (f == null)
        {
            return generate404Notfound();
        }

        FileInfo fi = f.getFi();
        if (fi == null)
        {
            fi = UniqPhotosStore.getInstance().getOneFileByHashStr(f.getEtag());
        }

        if (fi == null)
        {
            return generate404Notfound();
        }
        f.setFi(fi);

        if (HeadUtils.isNoFaces())
        {
            return generateSinglePhoto(fi, false);
        }

        StringBuffer sb = new StringBuffer(getHtmlHead(true));
        String yearNavigage = genYearNavigate();
        if (!HeadUtils.isMobile())
        {
            sb.append(yearNavigage);
        }
        // 隐藏照片
        sb.append("<script type=\"text/javascript\">"
                          + "function changeUrl(url){window.history.pushState({},0,'//'+window.location.host+'/'+url);}"
                          + "window.onload=changeUrl(" + "'facetoken/" + f.getFacetoken()
                          + extraQueryParas(true) + "');"
                          + "function deletephoto(path){jConfirm('该操作将永久隐藏照片，无法撤消，确认是否继续？','确认',function(r){if (r){"
                          + "changeUrl('?next=" + f.getFacetoken() + extraQueryParas(false)
                          + "&count=1');" + "window.location.reload();"
                          + "$.ajax({url:path,type:'DELETE',"
                          + "success:function(result){}});}});}");

        if (HeadUtils.isMobile())
        {
            // 滑动翻页
            sb.append("$(function() {");
            sb.append("$(\"#singlephoto\").swipe({");
            sb.append("swipe: function(event, direction, distance, duration, fingerCount) {");
            sb.append("if (distance>=30){if (direction=='right'){top.location=" + "'" + getPhotoUrl(
                    f.getFacetoken(), 1, false) + "';}else if (direction=='left') {top.location="
                              + "'" + getPhotoUrl(f.getFacetoken(), 1, true) + "';" + "}");
            sb.append("}},});});");
            /*
             * sb.append("$(document).ready(function(){"); sb.append(
             * "$(\"#singlephoto\").on(\"swipeleft\",function(){top.location=" +
             * "'" + getPhotoUrl(f, 1, false) + "'" + ";});" +
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
            sb.append("if(event.keyCode==37)top.location=" + "'" + getPhotoUrl(f.getFacetoken(), 1,
                                                                               false) + "';");
            sb.append("if(event.keyCode==39)top.location=" + "'" + getPhotoUrl(f.getFacetoken(), 1,
                                                                               true) + "';");
            sb.append(
                    "if(event.keyCode==46)deletephoto('/photos/" + f.getFi().getHash256() + "');");
            sb.append("});});");
        }

        sb.append("</script>");

        sb.append("<table style=\"text-align: center;\" width=\"100%\" "
                          + "height=\"100%\" border=\"0\" bordercolor=\"#000000\">");

        String dayStr = HeadUtils.formatDate(f.getFi().getPhotoTime());
        String viewDayStr =
                String.format("%s年%s月%s日", dayStr.substring(0, 4), dayStr.substring(4, 6),
                              dayStr.substring(6, 8));
        String returnToDayPage =
                "<a href=\"/day/" + dayStr + extraQueryParas(true) + "\">浏览 <b>" + viewDayStr
                        + "</b></a>";
        sb.append("<tr><td width=\"100%\" bordercolor=\"#000000\">");
        sb.append(returnToDayPage + separator);
        sb.append(getPhotoLink(f.getFacetoken(), false) + separator);
        sb.append(getPhotoLink(f.getFacetoken(), 1, false));
        sb.append("&nbsp;" + f.getFi().getPhotoTime() + "&nbsp;");
        sb.append(getPhotoLink(f.getFacetoken(), 1, true) + separator);
        sb.append(getPhotoLink(f.getFacetoken(), true));
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
            extraInfo = "onmouseover=\"upNext(this" + "," + "'" + getPhotoUrl(f.getFacetoken(), 1,
                                                                              false) + "'" + ","
                    + "'" + getPhotoUrl(f.getFacetoken(), 1, true) + "'" + ")\"";
        }

        sb.append(generateImgTag(f.getFi(), 860, extraInfo, "singlephoto", false));
        // sb.append("</a>");
        sb.append("</td></tr>");

        sb.append("<tr><td width=\"100%\" " + getTdHeight() + " bordercolor=\"#000000\">");

        String leftRotateLink = "<input id=\"leftrotate\" type=\"button\" value=\"左旋转\" />";
        String rightRotateLink = "<input id=\"rightrotate\" type=\"button\" value=\"右旋转\" />";

        sb.append(leftRotateLink + separator);
        if (HeadUtils.isSuperLogin() || HeadUtils.isLocalLogin())
        {
            String deleteLink = "<input id=\"deletephotob\" type=\"button\" value=\"隐藏\" />";
            sb.append(deleteLink + separator);
        }
        else
        {
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        }

        sb.append(rightRotateLink + separator);
        sb.append("</td><tr>");

        int r = 0;

        if (HeadUtils.needRotatePic(f.getFi()))
        {
            r = f.getFi().getRoatateDegree() / 90;
        }

        // 左旋转和右旋转照片
        sb.append("<script type=\"text/javascript\">" + "var r = " + r + ";"
                          + "$(\"#rightrotate\").click(function(){r++; if (r > 3){r = 0;}"
                          + "$(\"#singlephoto\").rotate(90*r);}); "
                          + "$(\"#leftrotate\").click(function(){r--; if (r < 0){ r = 3;}"
                          + "$(\"#singlephoto\").rotate(90*r);}); "
                          + "$(\"#deletephotob\").click(function(){deletephoto('/photos/" + f
                .getFi().getHash256() + "');});" + "</script>");

        sb.append("</table>");

        sb.append(yearNavigage);

        sb.append(getHtmlFoot());

        logHtml(sb);
        return sb.toString();

    }

    public static String getGAStr()
    {
        /*
         * <!-- Global site tag (gtag.js) - Google Analytics -->
         <script async src="https://www.googletagmanager.com/gtag/js?id=UA-300061-16"></script>
         <script>
         window.dataLayer = window.dataLayer || [];
         function gtag(){dataLayer.push(arguments);}
         gtag('js', new Date());

         gtag('config', 'UA-300061-16');
         </script>
         */
        String id = AppConfig.getInstance().getGoogleAnalyticsID();
        if (StringUtils.isNotBlank(id))
        {
            String gastr = "<!-- Global site tag (gtag.js) - Google Analytics -->\n"
                    + "         <script async src=\"https://www.googletagmanager.com/gtag/js?id="
                    + id + "\"></script>\n" + "         <script>\n"
                    + "         window.dataLayer = window.dataLayer || [];\n"
                    + "         function gtag(){dataLayer.push(arguments);}\n"
                    + "         gtag('js', new Date());\n" + "         gtag('config', '" + id
                    + "');\n" + "         </script>";
            return gastr;
        }
        else
        {
            return "";
        }
    }

    public static String genDownloadAPK()
    {
        if (!HeadUtils.isAPK())
        {
            return "<br/><br/><table style=\"text-align: center;\" width=\"100%\" height=\"100%\" "
                    + "border=\"0\" bordercolor=\"#000000\">"
                    + "<tr><td width=\"100%\" height=\"100%\" bordercolor=\"#000000\">"
                    + "<a href=\"/album.apk\">下载Android客户端</a>" + "</td></tr></table>";
        }

        return "";
    }

}

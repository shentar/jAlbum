package com.utils.web;

public class GenerateVideoHtml
{

    public static String genIndexHtml()
    {

        String index = GenerateHTML.getHtmlHead();
        index += "<video width=\"320\" height=\"240\" controls=\"controls\"><source src=\"movie.mp4\" "
                + "type=\"video/mp4\">Your browser does not support the video tag.</video>";

        index += GenerateHTML.getHtmlFoot();
        return index;

    }

}

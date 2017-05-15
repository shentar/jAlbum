package com.service.filter;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;

public class AuthFilter extends AbstractFilter
{
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private static final String ORIGNAL_URI_KEY = "origuri";

    @Override
    protected boolean doFilterInner(HttpServletRequest httpreq, HttpServletResponse httpres)
    {
        if (StringUtils.equals(httpreq.getRemoteAddr(), "127.0.0.1"))
        {
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.LocalLoin);
            return true;
        }

        Cookie[] cookies = httpreq.getCookies();
        String uri = httpreq.getRequestURI();

        switch (uri)
        {
        case "/logon":
            // 登录成功，则设置cookies，并返回主页。
            String token = httpreq.getParameter("token");
            String origUri = httpreq.getParameter(ORIGNAL_URI_KEY);
            String redirectLocation = "";
            if (TokenCache.getInstance().contains(token) || checkTokenExist(token))
            {
                // 登录成功跳转到主页
                Cookie c = new Cookie("sessionid", token);
                c.setMaxAge(3600 * 24 * 30);
                httpres.addCookie(c);

                SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.TokenLoin);
                redirectLocation = (origUri == null ? "/" : origUri);
            }
            else
            {
                SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.TokenError);
                // 登录失败，则继续返回登录页面。
                redirectLocation = "/login" + (StringUtils.isBlank(origUri) ? ""
                        : "?" + ORIGNAL_URI_KEY + "=" + origUri);
            }

            goToUrl(httpres, redirectLocation);
            return false;

        case "/login":
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.Unlogin);
            displayLogin(httpres, httpreq);
            return false;

        default:
            if (cookies == null || cookies.length == 0)
            {
                SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.Unlogin);
                // 跳转到登录页面。
                goToUrl(httpres, "/login" + "?" + ORIGNAL_URI_KEY + "=" + httpreq.getRequestURI());
                return false;
            }
            else
            {
                // 正确登录，则跳转主页，并刷新过期时间。
                // 登录信息过期，或者cookies不对，则删除cookies，并跳转到登录页面。
                for (Cookie c : cookies)
                {
                    if (StringUtils.equalsIgnoreCase("sessionid", c.getName())
                            && (TokenCache.getInstance().contains(c.getValue())
                                    || checkTokenExist(c.getValue())))
                    {
                        SystemProperties.add(SystemConstant.USER_LOGIN_STATUS,
                                LoginStatus.CookiesLoin);
                        // 从新设置cookie的时间为30天。
                        c.setMaxAge(3600 * 24 * 30);
                        httpres.addCookie(c);
                        return true;
                    }
                }

                for (Cookie c : cookies)
                {
                    c.setMaxAge(0);
                    httpres.addCookie(c);
                }
                SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.CookiesError);
                goToUrl(httpres, "/login" + "?" + ORIGNAL_URI_KEY + "=" + httpreq.getRequestURI());
                return false;
            }
        }

    }

    private void displayLogin(HttpServletResponse httpres, HttpServletRequest httpreq)
    {
        httpres.setStatus(200);
        try
        {
            String origUri = httpreq.getParameter(ORIGNAL_URI_KEY);
            String hh = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
                    + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
                    + "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">"
                    + "<head profile=\"http://gmpg.org/xfn/11\">"
                    + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
                    + "<title>Login jAlbum</title></head> "
                    + "<body><input id=\"txt\" size=\"64\" maxlength=\"64\"/> "
                    + "<a href=\"#\" id=\"login\"><input type=\"button\" onclick=\"chref()\" "
                    + "value=\"Login\"/></a><script>function chref(){var content = "
                    + "document.getElementById(\"txt\").value;"
                    + "window.location.replace('logon?token='+content"
                    + (StringUtils.isBlank(origUri) ? "" : "+'&origuri=" + origUri + "'")
                    + ");}</script>" + "</body></html>";
            httpres.setHeader("Content-type", "text/html;charset=UTF-8");
            httpres.getWriter().write(hh);
            httpres.getWriter().close();
        }
        catch (IOException e)
        {
            logger.warn("error occured: ", e);
        }

    }

    private void goToUrl(HttpServletResponse res, String location)
    {
        res.setHeader("Location", location);
        res.setStatus(307);
    }

}

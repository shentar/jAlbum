package com.service.filter;

import com.utils.conf.AppConfig;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.web.GenerateHTML;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthFilter extends AbstractFilter
{
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private static final String ORIGINAL_URI_KEY = "origuri";

    @Override
    public void init(FilterConfig arg0) throws ServletException
    {

    }

    @Override
    protected boolean doFilterInner(HttpServletRequest httpreq, HttpServletResponse httpres)
    {
        if (!AppConfig.getInstance().needAccessAuth())
        {
            logger.info("no need to auth access!");
            return true;
        }

        if (StringUtils.equals(httpreq.getRemoteAddr(), "127.0.0.1"))
        {
            logger.info("login from local host.");
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.LocalLoin);
            return true;
        }

        String uri = httpreq.getRequestURI();

        if (StringUtils.equals("/favicon.ico", uri) || StringUtils.equals("/album.apk", uri))
        {
            return true;
        }

        Cookie[] cookies = httpreq.getCookies();
        LoginStatus loginStatus = LoginStatus.Unlogin;
        String origUri = httpreq.getParameter(ORIGINAL_URI_KEY);
        String redirectLocation = "";
        String token = httpreq.getParameter("token");

        switch (uri)
        {
            case "/logon":
                // 登录成功，则设置cookies，并返回原入口页。
                if (StringUtils.isNotBlank(token) && TokenCache.getInstance().isSupper(token))
                {
                    loginStatus = LoginStatus.SuperLogin;
                    redirectLocation = (origUri == null ? "/" : origUri);
                    SystemProperties.add(SystemConstant.COOKIE_CONTENT, token);
                    HeadUtils.refreshCookie(httpres);
                }
                else if (StringUtils.isNotBlank(token) && TokenCache.getInstance().contains(token))
                {
                    loginStatus = LoginStatus.TokenLoin;
                    redirectLocation = (origUri == null ? "/" : origUri);
                    SystemProperties.add(SystemConstant.COOKIE_CONTENT, token);
                    HeadUtils.refreshCookie(httpres);
                }
                else
                {
                    logger.warn("token error: " + token);
                    loginStatus = LoginStatus.TokenError;
                    redirectLocation = "/login" + (StringUtils.isBlank(origUri) ? "" : "?"
                            + ORIGINAL_URI_KEY + "=" + origUri);
                }
                break;

            case "/login":
                loginStatus = LoginStatus.WaitLogin;
                break;

            default:
                if (cookies == null || cookies.length == 0)
                {
                    loginStatus = LoginStatus.Unlogin;
                    redirectLocation =
                            "/login" + "?" + ORIGINAL_URI_KEY + "=" + httpreq.getRequestURI();
                }
                else
                {
                    // 正确登录，则跳转主页，并刷新过期时间。
                    // 登录信息过期，或者cookies不对，则删除cookies，并跳转到登录页面。
                    for (Cookie c : cookies)
                    {
                        if (StringUtils.equalsIgnoreCase(HeadUtils.getCookieName(), c.getName()))
                        {
                            token = c.getValue();
                            if (TokenCache.getInstance().isSupper(token))
                            {
                                SystemProperties.add(SystemConstant.COOKIE_CONTENT, token);
                                loginStatus = LoginStatus.SuperLogin;
                                break;
                            }

                            if (TokenCache.getInstance().contains(token))
                            {
                                SystemProperties.add(SystemConstant.COOKIE_CONTENT, token);
                                loginStatus = LoginStatus.CookiesLoin;
                                break;
                            }
                        }
                    }

                    if (loginStatus.equals(LoginStatus.Unlogin))
                    {
                        logger.warn("cookies login error: " + token);
                        loginStatus = LoginStatus.CookiesError;
                        redirectLocation =
                                "/login" + "?" + ORIGINAL_URI_KEY + "=" + httpreq.getRequestURI();
                    }
                }
        }

        SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, loginStatus);
        boolean passed = false;
        switch (loginStatus)
        {
            case WaitLogin:
                displayLogin(httpres, httpreq);
                break;
            case SuperLogin:
            case TokenLoin:
            case CookiesLoin:
                passed = true;
                break;
            case CookiesError:
                for (Cookie ctmp : cookies)
                {
                    if (StringUtils.equalsIgnoreCase(ctmp.getName(), HeadUtils.getCookieName()))
                    {
                        ctmp.setMaxAge(0);
                        httpres.addCookie(ctmp);
                    }
                }
            case TokenError:
            case Unlogin:
                break;

            default:
                break;
        }

        if (StringUtils.isNotBlank(redirectLocation))
        {
            goToUrl(httpres, redirectLocation);
            passed = false;
        }

        return passed;
    }

    private void displayLogin(HttpServletResponse httpres, HttpServletRequest httpreq)
    {
        httpres.setStatus(200);
        try
        {
            String origUri = httpreq.getParameter(ORIGINAL_URI_KEY);
            String hh = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
                    + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
                    + "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"zh-CN\">"
                    + "<head profile=\"http://gmpg.org/xfn/11\">"
                    + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
                    + "<title>Login jAlbum</title></head><body style=\"text-align: center;\">"
                    + "<br/><input id=\"txt\" size=\"10\" maxlength=\"64\"/> "
                    + "<a href=\"#\" id=\"login\"><input type=\"button\" onclick=\"chref()\" "
                    + "value=\"Login\"/></a><script>function chref(){var content = "
                    + "document.getElementById(\"txt\").value;"
                    + "window.location.replace('logon?token='+content" + (
                    StringUtils.isBlank(origUri) ? "" : "+'&origuri=" + origUri + "'")
                    + ");}</script>" + GenerateHTML.getGAStr() + (!HeadUtils.isAPK()
                                                                  ? "<br/><br/><a href=\"/album.apk\">下载Android客户端</a>"
                                                                  : "") + "</body></html>";
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

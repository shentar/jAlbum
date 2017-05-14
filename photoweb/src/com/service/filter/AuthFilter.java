package com.service.filter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dao.GlobalConfDao;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.web.StringTools;

public class AuthFilter implements Filter
{
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private static final CopyOnWriteArraySet<String> tokenset = new CopyOnWriteArraySet<String>();

    @Override
    public void init(FilterConfig config) throws ServletException
    {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException
    {
        if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse))
        {
            logger.error("got an not request not http.");
            return;
        }

        boolean needNextStep = false;
        HttpServletRequest httpreq = (HttpServletRequest) req;
        HttpServletResponse httpres = (HttpServletResponse) res;
        Cookie[] cookies = httpreq.getCookies();
        String uri = httpreq.getRequestURI();
        if (StringUtils.equals(httpreq.getRemoteAddr(), "127.0.0.1")
                && StringUtils.equalsIgnoreCase(httpreq.getMethod(), "GET")
                && StringUtils.equalsIgnoreCase(uri, "/getToken"))
        {
            logger.warn("get all tokens.");
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.AdminLogin);
            httpres.setStatus(200);
            checkAndGenToken(httpres);
        }
        else if (StringUtils.equals(httpreq.getRemoteAddr(), "127.0.0.1")
                && StringUtils.equalsIgnoreCase(httpreq.getMethod(), "GET")
                && StringUtils.equalsIgnoreCase(uri, "/removeToken"))
        {
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.AdminLogin);
            String token = httpreq.getParameter("token");
            if (StringUtils.isBlank(token) || token.length() != 64)
            {
                logger.warn("error input token " + token);
                httpres.getWriter().write("error tokens.");
            }
            else
            {
                for (int i = 1; i != 5; i++)
                {
                    if (StringUtils.equals(token, getOneUserToken(i)))
                    {
                        logger.warn("the token is removed!");
                        GlobalConfDao.getInstance().delete(getOneUserKey(i));
                        tokenset.remove(token);
                        break;
                    }
                }
                logger.warn("removed token and add new token.");
                // 重新生成tokens
                checkAndGenToken(httpres);
            }
        }
        else if (StringUtils.equals(httpreq.getRemoteAddr(), "127.0.0.1")
                && StringUtils.equalsIgnoreCase(httpreq.getMethod(), "GET")
                && StringUtils.equalsIgnoreCase(uri, "/clearToken"))
        {
            logger.warn("admin: clear all tokens");
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.AdminLogin);
            for (int i = 0; i != 5; i++)
            {
                GlobalConfDao.getInstance().delete(getOneUserKey(i));
            }
            tokenset.clear();
            checkAndGenToken(httpres);
        }
        else if (!StringUtils.equals(httpreq.getRemoteAddr(), "127.0.0.1"))
        {
            // 非本地登录
            if (StringUtils.equalsIgnoreCase("/logon", uri))
            {
                // 登录成功，则设置cookies，并返回主页。
                String token = httpreq.getParameter("token");
                if (tokenset.contains(token) || checkTokenExist(token))
                {
                    // 登录成功跳转到主页
                    Cookie c = new Cookie("sessionid", token);
                    httpres.addCookie(c);
                    SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.TokenLoin);
                    goToUrl(httpres, "/");
                }
                else
                {
                    SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.TokenError);
                    // 登录失败，则继续返回登录页面。
                    goToUrl(httpres, "/login");
                }
            }
            else if (StringUtils.equalsIgnoreCase("/login", uri))
            {
                SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.Unlogin);
                displayLogin(httpres);
            }
            else
            {
                if (cookies == null || cookies.length == 0)
                {
                    SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.Unlogin);
                    // 跳转到登录页面。
                    goToUrl(httpres, "/login");
                }
                else
                {
                    // 正确登录，则跳转主页，并刷新过期时间。
                    // 登录信息过期，或者cookies不对，则删除cookies，并跳转到登录页面。
                    for (Cookie c : cookies)
                    {
                        if (StringUtils.equalsIgnoreCase("sessionid", c.getName())
                                && (tokenset.contains(c.getValue())
                                        || checkTokenExist(c.getValue())))
                        {
                            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS,
                                    LoginStatus.CookiesLoin);
                            needNextStep = true;
                            // 从新设置cookie的时间为30天。
                            c.setMaxAge(3600 * 24 * 30);
                            httpres.addCookie(c);
                            break;
                        }
                    }

                    if (!needNextStep)
                    {
                        for (Cookie c : cookies)
                        {
                            c.setMaxAge(0);
                            httpres.addCookie(c);
                        }
                        SystemProperties.add(SystemConstant.USER_LOGIN_STATUS,
                                LoginStatus.CookiesError);
                        goToUrl(httpres, "/login");
                    }
                }
            }
        }
        else
        {
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.LocalLoin);
            needNextStep = true;
        }

        if (needNextStep)
        {
            chain.doFilter(req, res);
        }

    }

    private void displayLogin(HttpServletResponse httpres)
    {
        httpres.setStatus(200);
        try
        {
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
                    + "window.location.replace('logon?token='+content);}</script>"
                    + "</body></html>";
            httpres.setHeader("Content-type", "text/html;charset=UTF-8");
            httpres.getWriter().write(hh);
            httpres.getWriter().close();
        }
        catch (IOException e)
        {
            logger.warn("error occured: ", e);
        }

    }

    private String getOneUserToken(int i)
    {
        return GlobalConfDao.getInstance().getConf(getOneUserKey(i));
    }

    private String getOneUserKey(int i)
    {
        return "usertoken" + i;
    }

    private boolean checkTokenExist(String token)
    {
        for (int i = 0; i != 5; i++)
        {
            if (StringUtils.equals(token, getOneUserToken(i)))
            {
                tokenset.add(token);
                return true;
            }
        }

        return false;
    }

    private synchronized void checkAndGenToken(HttpServletResponse httpres)
    {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i != 5; i++)
        {
            String token = getOneUserToken(i);
            if (StringUtils.isBlank(getOneUserToken(i)))
            {
                token = StringTools.getRandomString(64);
                GlobalConfDao.getInstance().setConf(getOneUserKey(i), token);
            }

            sb.append(getOneUserKey(i)).append(":").append(token).append("\r\n");
        }

        try
        {
            httpres.setStatus(200);
            httpres.getWriter().write(sb.toString());
            httpres.getWriter().close();
        }
        catch (IOException e)
        {
            logger.warn("write the token failed: ", e);
        }
    }

    private void goToUrl(HttpServletResponse res, String location)
    {
        res.setHeader("Location", location);
        res.setStatus(307);
    }

    @Override
    public void destroy()
    {

    }
}

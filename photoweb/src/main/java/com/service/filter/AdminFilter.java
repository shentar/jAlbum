package com.service.filter;

import com.backend.dao.GlobalConfDao;
import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.web.StringTools;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AdminFilter extends AbstractFilter
{
    private static final Logger logger = LoggerFactory.getLogger(AdminFilter.class);

    @Override
    protected boolean doFilterInner(HttpServletRequest httpreq, HttpServletResponse httpres)
            throws IOException
    {
        if (!(StringUtils.equals(httpreq.getRemoteAddr(), "127.0.0.1")
                && StringUtils.equalsIgnoreCase(httpreq.getMethod(), "GET")))
        {
            return true;
        }

        String uri = httpreq.getRequestURI();

        switch (uri)
        {
        case "/getToken":
            logger.warn("get all tokens.");
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.AdminLogin);
            httpres.setStatus(200);
            checkAndGenToken(httpres);
            return false;

        case "/removeToken":
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.AdminLogin);
            String token = httpreq.getParameter("token");
            if (StringUtils.isBlank(token) || token.length() != 64)
            {
                logger.warn("error input token " + token);
                httpres.getWriter().write("error tokens.");
            }
            else
            {
                if (TokenCache.getInstance().isSupper(token))
                {
                    GlobalConfDao.getInstance().delete(SystemConstant.SUPER_TOKEN_KEY);
                }
                for (int i = 1; i != 5; i++)
                {
                    if (StringUtils.equals(token, GlobalConfDao.getInstance().getOneUserToken(i)))
                    {
                        logger.warn("the token is removed!");
                        GlobalConfDao.getInstance()
                                .delete(GlobalConfDao.getInstance().getOneUserKey(i));
                        TokenCache.getInstance().removeToken(token);
                        break;
                    }
                }
                logger.warn("removed token and add new token.");
                // 重新生成tokens
                checkAndGenToken(httpres);
            }

            return false;

        case "/clearToken":
            logger.warn("admin: clear all tokens");
            SystemProperties.add(SystemConstant.USER_LOGIN_STATUS, LoginStatus.AdminLogin);
            for (int i = 0; i != 5; i++)
            {
                GlobalConfDao.getInstance().delete(GlobalConfDao.getInstance().getOneUserKey(i));
            }
            checkAndGenToken(httpres);
            return false;

        default:
            return true;
        }

    }

    private synchronized void checkAndGenToken(HttpServletResponse httpres)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i != 5; i++)
        {
            String token = GlobalConfDao.getInstance().getOneUserToken(i);
            if (StringUtils.isBlank(token))
            {
                token = StringTools.getRandomString(64);
                GlobalConfDao.getInstance().setConf(GlobalConfDao.getInstance().getOneUserKey(i),
                        token);
            }

            sb.append(GlobalConfDao.getInstance().getOneUserKey(i)).append(":").append(token)
                    .append("\r\n");
        }

        String superToken = GlobalConfDao.getInstance().getConf(SystemConstant.SUPER_TOKEN_KEY);
        if (StringUtils.isBlank(superToken))
        {
            superToken = StringTools.getRandomString(64);
            GlobalConfDao.getInstance().setConf(SystemConstant.SUPER_TOKEN_KEY, superToken);
        }

        sb.append("superToken: ").append(superToken);

        TokenCache.getInstance().refreshToken();
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

}

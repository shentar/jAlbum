package com.service.filter;

import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.sys.UUIDGenerator;
import com.utils.web.AccessLogger;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class WebFilter implements Filter
{
    private static final Logger logger = LoggerFactory.getLogger(WebFilter.class);

    @Override
    public void destroy()
    {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chains)
            throws IOException, ServletException
    {
        long startTime = System.currentTimeMillis();
        try
        {
            initMDC();
            MDC.put(SystemConstant.REQUESTIDKEY, UUIDGenerator.getUUID());

            if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse))
            {
                logger.error("got an not request not http.");
                return;
            }
            HttpServletRequest newreq = (HttpServletRequest) req;
            HttpServletResponse newres = (HttpServletResponse) res;

            newres.setHeader(SystemConstant.REQUEST_ID_HEADER,
                             (String) MDC.get(SystemConstant.REQUESTIDKEY));

            MDC.put(SystemConstant.HOST_NAME,
                    newreq.getHeader("Host") + "/" + newreq.getLocalAddr() + ":" + newreq
                            .getLocalPort());

            String useragent = newreq.getHeader(SystemConstant.USER_AGENT_HEADER);
            if (StringUtils.isNotBlank(useragent))
            {
                MDC.put(SystemConstant.USER_AGENT, useragent);
                HeadUtils.checkMobile(useragent);

                logger.info("user agent is: " + useragent);
            }

            if (newreq.getParameter("video") != null)
            {
                SystemProperties.add(SystemConstant.IS_VIDEO, true);
            }

            if (newreq.getParameter("face") != null)
            {
                SystemProperties.add(SystemConstant.IS_FACES_KEY, Boolean.TRUE);
            }

            if (newreq.getParameter("noface") != null)
            {
                SystemProperties.add(SystemConstant.IS_NO_FACES_KEY, Boolean.TRUE);
            }

            String ft = newreq.getParameter("facetoken");
            if (ft != null && ft.length() >= 32)
            {
                SystemProperties.add(SystemConstant.FACE_TOKEN_KEY, ft);
            }

            MDC.put(SystemConstant.REMOTE_ADDR,
                    newreq.getRemoteAddr() + ":" + newreq.getRemotePort());

            String url = newreq.getRequestURI();
            if (StringUtils.isNotBlank(newreq.getQueryString()))
            {
                url += ("?" + newreq.getQueryString());
            }
            MDC.put(SystemConstant.HTTP_URI, url);

            chains.doFilter(newreq, newres);

            MDC.put(SystemConstant.HTTP_STATUS, "" + newres.getStatus());
            MDC.put(SystemConstant.USER_LOGIN_STATUS,
                    "" + SystemProperties.get(SystemConstant.USER_LOGIN_STATUS));
        }
        finally
        {
            MDC.put(SystemConstant.CONSUMED_TIME, (System.currentTimeMillis() - startTime) + "");
            MDC.put(SystemConstant.IS_MOBILE_KEY, "" + HeadUtils.isMobile());
            AccessLogger.accessLog();

            SystemProperties.clear();
            MDC.clear();
        }
    }

    private void initMDC()
    {
        MDC.put(SystemConstant.CONSUMED_TIME, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.IS_MOBILE_KEY, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.HTTP_STATUS, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.REMOTE_ADDR, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.USER_AGENT, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.REQUESTIDKEY, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.FILE_NAME, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.HTTP_URI, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.RANGE_HEADER_KEY, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.USER_LOGIN_STATUS, SystemConstant.DEFAULT_LOG_VALUE);
        MDC.put(SystemConstant.HOST_NAME, SystemConstant.DEFAULT_LOG_VALUE);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException
    {

    }

}

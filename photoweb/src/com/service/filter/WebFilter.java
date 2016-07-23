package com.service.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.utils.sys.SystemConstant;
import com.utils.sys.SystemProperties;
import com.utils.sys.UUIDGenerator;
import com.utils.web.AccessLogger;
import com.utils.web.HeadUtils;

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
            MDC.put(SystemConstant.REQEUSTIDKEY, UUIDGenerator.getUUID());

            if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse))
            {
                logger.error("got an not request not http.");
                return;
            }
            HttpServletRequest newreq = (HttpServletRequest) req;
            HttpServletResponse newres = (HttpServletResponse) res;

            newres.setHeader(SystemConstant.REQUEST_ID_HEADER,
                    (String) MDC.get(SystemConstant.REQEUSTIDKEY));

            String useragent = newreq.getHeader(SystemConstant.USER_AGENT_HEADER);
            if (StringUtils.isNotBlank(useragent))
            {
                MDC.put(SystemConstant.USER_AGENT, useragent);
            }
            MDC.put(SystemConstant.REMOTE_ADDR,
                    newreq.getRemoteAddr() + ":" + newreq.getRemotePort());

            SystemProperties.add(SystemConstant.IS_MOBILE_KEY,
                    new Boolean(HeadUtils.checkMobile(((HttpServletRequest) req))));

            chains.doFilter(newreq, newres);

            MDC.put(SystemConstant.HTTP_STATUS, "" + newres.getStatus());
        }
        finally
        {
            MDC.put(SystemConstant.CONSUMED_TIME, (System.currentTimeMillis() - startTime) + "");
            MDC.put(SystemConstant.IS_MOBILE_KEY, "" + HeadUtils.isMobile());
            AccessLogger.accessLog();
            
            SystemProperties.init();
            MDC.clear();
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException
    {

    }

}

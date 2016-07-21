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

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.utils.sys.SystemProperties;
import com.utils.sys.UUIDGenerator;

public class WebFilter implements Filter
{
    private static final Logger logger = LoggerFactory.getLogger(WebFilter.class);

    private static final String REQEUSTIDKEY = "requestid";

    @Override
    public void destroy()
    {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chains)
            throws IOException, ServletException
    {
        try
        {
            MDC.put(REQEUSTIDKEY, UUIDGenerator.getUUID());

            logger.warn("get a request: " + System.nanoTime() + " the remote port is: "
                    + req.getRemotePort());

            if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse))
            {
                logger.error("got an not request not http.");
                return;
            }

            ((HttpServletResponse) res).setHeader("Request-ID", (String) MDC.get(REQEUSTIDKEY));

            chains.doFilter(req, res);

            logger.warn("done a request: " + System.nanoTime() + " the remote port is: "
                    + req.getRemotePort());
        }
        finally
        {
            SystemProperties.init();
            MDC.remove(REQEUSTIDKEY);
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException
    {

    }

}

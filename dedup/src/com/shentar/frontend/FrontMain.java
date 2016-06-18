package com.shentar.frontend;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class FrontMain
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(2148);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setWar("root.war");
        server.setHandler(context);

        server.start();
        server.join();
    }
}

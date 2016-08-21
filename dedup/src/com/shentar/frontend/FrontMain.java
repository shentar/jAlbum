package com.shentar.frontend;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class FrontMain
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(2148);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setWar("root.war");
        server.setHandler(context);

        SignalHandler sig = new SignalHandler()
        {
            @Override
            public void handle(Signal arg0)
            {
                if (arg0.getNumber() == 15)
                {
                    try
                    {
                        server.stop();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
        Signal.handle(new Signal("TERM"), sig);

        server.start();
        server.join();
    }
}

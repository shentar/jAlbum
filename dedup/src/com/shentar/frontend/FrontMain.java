package com.shentar.frontend;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class FrontMain
{
    public static void main(String[] args) throws Exception
    {
        final Server server = new Server(2148);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setWar("root.war");
        server.setHandler(context);

        SignalHandler sig = new SignalHandler()
        {
            @Override
            public void handle(Signal signum)
            {
                System.out.println("caught signal " + signum);
                if (signum.getNumber() == 15)
                {
                    System.out.println("caught KILL signal, exit now.");
                    try
                    {
                        server.stop();
                        Runtime.getRuntime().halt(0);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
        Signal.handle(new Signal("TERM"), sig);

        System.out.println("start now.");
        server.start();
        System.out.println("started.");
        server.join();
        System.out.println("exit now!");
    }
}

package com.shentar.frontend;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class FrontMain
{
    public static final int DEFAULT_PORT = 2148;

    public static void main(String[] args) throws Exception
    {
        int port = DEFAULT_PORT;
        if (args.length == 1)
        {
            String pstr = args[0];
            try
            {
                port = Integer.parseInt(pstr);
                if (port < 0 || port > 65535)
                {
                    port = DEFAULT_PORT;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        QueuedThreadPool thp = new QueuedThreadPool();
        thp.setIdleTimeout(900 * 1000);
        thp.setMinThreads(32);
        thp.setMaxThreads(200);
        final Server server = new Server(thp);

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setWar("root.war");
        ServerConnector connector = new ServerConnector(server);
        connector.setIdleTimeout(5000);
        connector.setPort(port);
        connector.setAcceptQueueSize(4);
        // connector.setSoLingerTime(5000);

        server.addConnector(connector);

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

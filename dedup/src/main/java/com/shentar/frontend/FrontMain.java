package com.shentar.frontend;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;

public class FrontMain {
    private static final int DEFAULT_PORT = 2148;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length == 1) {
            String pstr = args[0];
            try {
                port = Integer.parseInt(pstr);
                if (port < 0 || port > 65535) {
                    port = DEFAULT_PORT;
                }
            } catch (Exception e) {
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

        String epath = "lib" + File.separator + "extra";
        File[] fs = new File(epath).listFiles();
        if (fs != null && fs.length > 0) {
            StringBuilder extraPath = new StringBuilder();
            for (File f : fs) {
                String filename = f.getName();
                if (filename.toLowerCase().endsWith(".jar")) {
                    extraPath.append(epath).append(File.separator).append(filename).append(",");
                }
            }
            context.setExtraClasspath(extraPath.toString());
        } else {
            throw new IOException("there is no extra lib files.");
        }

        server.setHandler(context);

        HttpConfiguration configuration = new HttpConfiguration();
        configuration.setSendDateHeader(false);

        ServerConnector connector =
                new ServerConnector(server, new HttpConnectionFactory(configuration));
        connector.setIdleTimeout(5000);
        connector.setHost("0.0.0.0");
        connector.setPort(port);
        connector.setReuseAddress(true);
        connector.setAcceptQueueSize(4);
        // connector.setSoLingerTime(5000);
        server.addConnector(connector);

        /*
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("keystore");
        sslContextFactory.setKeyStorePassword("123456");
        sslContextFactory.setKeyManagerPassword("123456");
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

        HttpConfiguration https_config = new HttpConfiguration(configuration);
        https_config.setSecurePort(5443);
        https_config.addCustomizer(new SecureRequestCustomizer());
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(https_config);

        SslConnectionFactory scf =
                new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

        connector = new ServerConnector(server, scf, alpn, h2, new HttpConnectionFactory(https_config));
        connector.setAcceptQueueSize(4);
        connector.setIdleTimeout(5000);
        connector.setPort(5443);
        server.addConnector(connector);
        */

        /**
         * SignalHandler sig = new SignalHandler() {
         *
         * @Override public void handle(Signal signum) { System.out.println(
         *           "caught signal " + signum); if (signum.getNumber() == 15) {
         *           System.out.println("caught KILL signal, exit now."); try {
         *           server.stop(); Runtime.getRuntime().halt(0); } catch
         *           (Exception e) { e.printStackTrace(); } } } };
         *           Signal.handle(new Signal("TERM"), sig);
         */

        System.out.println("start now.");
        server.start();
        System.out.println("started.");
        server.join();
        System.out.println("exit now!");
    }
}

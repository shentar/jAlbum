package com.backend.facer;

import com.utils.conf.AppConfig;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class FaceClient {
    private static final int TIMEOUT = 6000 * 1000;

    private static final int MAX_CONNECTION = 200;

    private static CloseableHttpClient httpClient = null;

    static {
        initHttpclient();
    }

    public static CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public static void config(HttpRequestBase httpRequestBase) {
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUT)
                .setConnectTimeout(TIMEOUT).setSocketTimeout(TIMEOUT).build();
        httpRequestBase.setConfig(requestConfig);
        httpRequestBase.setHeader("User-Agent",
                "jAlbum_" + AppConfig.getInstance().getVersion("0.2.2"));
    }

    private synchronized static void initHttpclient() {
        if (httpClient == null) {
            String endPoint = AppConfig.getInstance().getFacerEndPoint();
            int port = AppConfig.getInstance().getFacerDetectPort();
            httpClient = createHttpClient(MAX_CONNECTION, MAX_CONNECTION / 2, MAX_CONNECTION / 2,
                    endPoint, port);
        }
    }

    private static CloseableHttpClient createHttpClient(int maxTotal, int maxPerRoute, int maxRoute,
                                                        String hostname, int port) {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory>create().register("http", plainsf)
                .register("https", sslsf).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(maxPerRoute);
        HttpHost httpHost = new HttpHost(hostname, port);
        cm.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);

        return HttpClients.custom().setConnectionManager(cm).build();
    }
}

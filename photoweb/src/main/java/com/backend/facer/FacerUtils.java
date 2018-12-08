package com.backend.facer;

import com.backend.entity.FileInfo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utils.conf.AppConfig;
import com.utils.media.ThumbnailGenerator;
import com.utils.media.ThumbnailManager;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FacerUtils
{
    public static final String IMG_FILE = "image_file";

    public static final String OUTER_ID = "outer_id";

    public static final String FACE_TOKEN = "face_token";

    public static final String DETACT_URL = "/facepp/v3/detect";

    public static final String COMPARE_URL = "/facepp/v3/compare";

    public static final String SEARCH_URL = "/facepp/v3/search";

    public static final String FACESET_ADD_URL = "/facepp/v3/faceset/addface";

    public static final String FACESET_DETAIL_URL = "/facepp/v3/faceset/getdetail";

    public static final String FACESET_CREATE_URL = "/facepp/v3/faceset/create";

    public static final String FACESET_TOKEN_REMOVE_URL = "/facepp/v3/faceset/removeface";

    private static final Logger logger = LoggerFactory.getLogger(FacerUtils.class);

    private static final String API_ENDPOINT = "api-cn.faceplusplus.com";

    private static final int TIMEOUT = 60 * 1000;

    private static final int PORT = 443;

    private static final int MAX_FILE_SIZE = 1000 * 2000;

    private static final int MAX_HEIGHT_WIDTH = 4096;

    private static CloseableHttpClient httpClient = null;

    private static final int MAX_CONNECTION = 4;

    private static AtomicInteger failedTimes = new AtomicInteger(0);

    static
    {
        initHttpclient();
    }

    private static void config(HttpRequestBase httpRequestBase)
    {
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(TIMEOUT)
                .setConnectTimeout(TIMEOUT).setSocketTimeout(TIMEOUT).build();
        httpRequestBase.setConfig(requestConfig);
        httpRequestBase.setHeader("User-Agent",
                "jAlbum_" + AppConfig.getInstance().getVersion("0.2.2"));
    }

    private synchronized static void initHttpclient()
    {
        if (httpClient == null)
        {
            httpClient = createHttpClient(MAX_CONNECTION, MAX_CONNECTION / 2, MAX_CONNECTION / 2,
                    API_ENDPOINT, PORT);
        }
    }

    private static CloseableHttpClient createHttpClient(int maxTotal, int maxPerRoute, int maxRoute,
            String hostname, int port)
    {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory> create().register("http", plainsf)
                .register("https", sslsf).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(maxPerRoute);
        HttpHost httpHost = new HttpHost(hostname, port);
        cm.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);

        return HttpClients.custom().setConnectionManager(cm).build();
    }

    public static Object getFileForDetectFaces(FileInfo fi)
    {
        Object file = null;
        if (fi.getSize() > MAX_FILE_SIZE || fi.getHeight() > MAX_HEIGHT_WIDTH
                || fi.getWidth() > MAX_HEIGHT_WIDTH)
        {
            byte[] buffers = ThumbnailGenerator.generateThumbnailBuffer(fi.getPath(), 1680, 1680,
                    false);
            if (buffers != null && buffers.length < MAX_FILE_SIZE)
            {
                file = buffers;
            }
            else
            {
                logger.warn("the size {} is too large to detect face.",
                        buffers == null ? 0 : buffers.length);
                File f = ThumbnailManager.getThumbnail(fi.getHash256());

                if (f != null)
                {
                    // 如果缩略图也大于最大限制大小，则返回空。
                    if (f.length() > MAX_FILE_SIZE)
                    {
                        logger.warn("the size of thumbnal is large than 2M. fileinf[{}]", fi);
                        return null;
                    }

                    try
                    {
                        file = f.getCanonicalPath();
                    }
                    catch (IOException e)
                    {
                        logger.warn("caused by: ", e);
                    }
                }
            }
        }
        else
        {
            file = fi.getPath();
        }
        return file;
    }

    private static void setPostParams(HttpPost httpost, Map<String, Object> params)
    {
        config(httpost);
        if (params == null)
        {
            return;
        }

        MultipartEntityBuilder mp = MultipartEntityBuilder.create();
        for (Map.Entry<String, Object> e : params.entrySet())
        {
            if (StringUtils.startsWithIgnoreCase(e.getKey(), IMG_FILE))
            {
                if (e.getValue() instanceof byte[])
                {
                    mp.addBinaryBody(e.getKey(), (byte[]) (e.getValue()),
                            ContentType.APPLICATION_OCTET_STREAM, "JPGFILE.JPG");
                }
                else if (e.getValue() instanceof String)
                {
                    mp.addBinaryBody(e.getKey(), new File(e.getValue() + ""));
                }
                else if (e.getValue() instanceof File)
                {
                    mp.addBinaryBody(e.getKey(), (File) (e.getValue()));
                }
                else if (e.getValue() instanceof InputStream)
                {
                    mp.addBinaryBody(e.getKey(), (InputStream) (e.getValue()),
                            ContentType.APPLICATION_OCTET_STREAM, "JPGFILE.JPG");
                }
                else
                {
                    mp.addTextBody(e.getKey(), e.getValue() + "");
                }
            }
            else
            {
                mp.addTextBody(e.getKey(), e.getValue() + "");
            }
        }
        mp.addTextBody("api_key", AppConfig.getInstance().getApiKey());
        mp.addTextBody("api_secret", AppConfig.getInstance().getSecret());
        httpost.setEntity(mp.build());
    }

    /**
     * The faceplusplus not promise concurrent for free service. This func must
     * be called synchronized.
     * 
     * @param url
     * @param params
     * @return results
     */
    public synchronized static String post(String url, Map<String, Object> params)
    {
        String fullurl = AppConfig.getInstance().facerUseHttps() ? "https" : "http";
        fullurl += "://";
        fullurl += API_ENDPOINT;
        fullurl += url;

        boolean needRetry = true;
        while (needRetry)
        {
            CloseableHttpResponse response = null;
            try
            {
                HttpPost httppost = new HttpPost(fullurl);
                httppost.addHeader("Connection", "Keep-Alive");
                setPostParams(httppost, params);

                response = httpClient.execute(httppost);
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, "utf-8");

                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 403)
                {
                    JsonParser parser = new JsonParser();
                    JsonObject jr = (JsonObject) parser.parse(result);
                    JsonElement je = jr.get("error_message");
                    if (je != null && StringUtils.equalsIgnoreCase("CONCURRENCY_LIMIT_EXCEEDED",
                            je.getAsString()))
                    {
                        // need retry.
                        EntityUtils.consume(entity);
                        continue;
                    }
                }
                else if (response.getStatusLine().getStatusCode() != 200)
                {
                    logger.error("post request failed: " + result);
                    logger.error("the file is: " + params.get(IMG_FILE));
                    result = null;
                }
                EntityUtils.consume(entity);
                needRetry = false;

                return result;
            }
            catch (Exception e)
            {
                logger.error("caused by: ", e);
                logger.error("the file is: " + params.get(IMG_FILE));
                needRetry = true;
            }
            finally
            {
                try
                {
                    if (response != null)
                    {
                        response.close();
                    }
                }
                catch (IOException e)
                {
                    logger.warn("caused: ", e);
                }

                delayBeforeRetry(needRetry);
            }
        }

        return null;
    }

    private static void delayBeforeRetry(boolean needRetry)
    {
        if (needRetry)
        {
            int ft = failedTimes.incrementAndGet();
            ft = (ft > 8 ? 8 : ft);

            try
            {
                Thread.sleep((long) (15000 * Math.pow(2, ft)));
            }
            catch (InterruptedException e)
            {
                logger.warn("caused by: ", e);
            }
        }
        else
        {
            failedTimes.set(0);
        }
    }

    public static void sortByTime(List<Face> flst)
    {
        if (flst == null || flst.isEmpty())
        {
            return;
        }

        Collections.sort(flst, new Comparator<Face>()
        {
            @Override
            public int compare(Face o1, Face o2)
            {
                if (o1 == null && o2 == null)
                {
                    return 0;
                }

                if (o1 == null)
                {
                    return 1;
                }

                if (o2 == null)
                {
                    return -1;
                }

                long res = o1.getPtime() - o2.getPtime();
                if (res == 0)
                {
                    return 0;
                }
                else if (res > 0)
                {
                    return -1;
                }
                else
                {
                    return 1;
                }
            }
        });
    }

    public static void sortByQuality(List<Face> flst)
    {
        if (flst == null || flst.isEmpty())
        {
            return;
        }

        Collections.sort(flst, new Comparator<Face>()
        {
            @Override
            public int compare(Face o1, Face o2)
            {
                String q1 = o1 == null ? null : o1.getQuality();
                String q2 = o2 == null ? null : o2.getQuality();

                if (q1 == q2)
                {
                    return 0;
                }

                if (q1 == null)
                {
                    return 1;
                }

                if (q2 == null)
                {
                    return -1;
                }

                return q2.compareTo(q1);
            }

        });
    }

}

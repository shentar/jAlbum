package com.backend.threadpool;

import com.utils.conf.AppConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolFactory
{
    public static final String BACKEND_SCANNER = "BackendScanner";

    public static final String SYNC_TOOL = "SyncTool";

    public static final String FILE_TOOL = "FileTool";

    public static final String THREAD_POOL_4THUMBNAIL = "Thumbnail";

    public static final String FACE_REC_SERVICE = "FaceRecService";

    private static final Map<String, ThreadPoolExecutor> factory = new HashMap<>();

    static
    {
        factory.put(BACKEND_SCANNER, genOnePool(BACKEND_SCANNER, 3));
        factory.put(SYNC_TOOL,
                    genOnePool(SYNC_TOOL, AppConfig.getInstance().getS3ConcurrentThreads()));
        factory.put(FILE_TOOL, genOnePool(FILE_TOOL, AppConfig.getInstance().getThreadCount()));
        // 对于树莓派等系统，最多只能2个线程同时计算缩略图。
        factory.put(THREAD_POOL_4THUMBNAIL, genOnePool(THREAD_POOL_4THUMBNAIL, 2));
        factory.put(FACE_REC_SERVICE, genOnePool(FACE_REC_SERVICE, AppConfig.getInstance()
            .getFacerConcurrentThreads()));
    }

    public static ThreadPoolExecutor getThreadPool(String type)
    {
        return factory.get(type);
    }

    public static String runningJobStatistics()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\">");
        sb.append("<tr><td>Job Status</td><td>running</td><td>waiting</td><td>completed</td></tr>");
        for (Map.Entry<String, ThreadPoolExecutor> en : factory.entrySet())
        {
            sb.append(getOnePooolInfo(en.getKey()));
        }
        sb.append("</table>");
        return sb.toString();
    }

    private static StringBuilder getOnePooolInfo(String type)
    {
        ThreadPoolExecutor tp = factory.get(type);
        long allTasks = tp.getTaskCount();
        long completedTasks = tp.getCompletedTaskCount();
        int waitedTasks = tp.getQueue().size();
        long runningTasks = allTasks - completedTasks - waitedTasks;
        if (runningTasks < 0)
        {
            runningTasks = 0;
        }

        StringBuilder sb = new StringBuilder("<tr><td>" + type + "</td>");
        sb.append("<td>").append(runningTasks).append("</td>");
        sb.append("<td>").append(waitedTasks).append("</td>");
        sb.append("<td>").append(completedTasks).append("</td>");
        sb.append("</tr>");
        return sb;
    }

    private static ThreadPoolExecutor genOnePool(final String namePrefix, int count)
    {
        return new ThreadPoolExecutor(count, count, 0L, TimeUnit.SECONDS,
                                      new LinkedBlockingDeque<Runnable>(), new ThreadFactory()
        {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r)
            {
                return new Thread(r, namePrefix + "_" + index.incrementAndGet());
            }
        });
    }
}

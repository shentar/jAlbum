package com.backend.metrics;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.utils.conf.AppConfig;

public class MetricsClient {
    private MetricsClient() {
    }

    private static class MetricsClientHolder {
        private static final MetricsClient instance = new MetricsClient();
    }

    private final StatsDClient statsd = new NonBlockingStatsDClient("home.jalbum",
            AppConfig.getInstance().getMetricsServerHost(), AppConfig.getInstance().getMetricsServerPort());

    public static MetricsClient getInstance() {
        return MetricsClientHolder.instance;
    }

    public void metricsCount(String metrics, long value) {
        if (!AppConfig.getInstance().isMetricsEnabled()) {
            return;
        }

        statsd.count(metrics, value);
    }
}

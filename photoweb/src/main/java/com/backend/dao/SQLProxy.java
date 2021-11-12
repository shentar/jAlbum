package com.backend.dao;

import com.backend.metrics.MetricsClient;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLProxy {
    public static ResultSet executeQuery(PreparedStatement ps) throws SQLException {
        long start = System.nanoTime();
        try {
            return ps.executeQuery();
        } finally {
            MetricsClient.getInstance().metricsCount("sql_exec", 1);
            MetricsClient.getInstance().metricsTimeDuration("sql_exec_duration", System.nanoTime() - start);
        }
    }

    public static boolean execute(PreparedStatement ps) throws SQLException {
        long start = System.nanoTime();
        try {
            return ps.execute();
        } finally {
            MetricsClient.getInstance().metricsCount("sql_exec", 1);
            MetricsClient.getInstance().metricsTimeDuration("sql_exec_duration", System.nanoTime() - start);
        }
    }
}

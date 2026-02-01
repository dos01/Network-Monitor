package com.networkmonitor.service;

import com.networkmonitor.model.UsageRecord;

import java.sql.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:network_stats.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS network_usage (" +
                "timestamp INTEGER," +
                "download_bytes INTEGER," +
                "upload_bytes INTEGER" +
                ");";

        // Index for faster range queries
        String indexSql = "CREATE INDEX IF NOT EXISTS idx_timestamp ON network_usage(timestamp);";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(indexSql);
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    public void insertUsage(UsageRecord record) {
        String sql = "INSERT INTO network_usage(timestamp, download_bytes, upload_bytes) VALUES(?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, record.getTimestamp());
            pstmt.setLong(2, record.getDownloadBytes());
            pstmt.setLong(3, record.getUploadBytes());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error inserting record: " + e.getMessage());
        }
    }

    public List<UsageRecord> getUsageInRange(long startMillis, long endMillis) {
        List<UsageRecord> records = new ArrayList<>();
        String sql = "SELECT timestamp, download_bytes, upload_bytes FROM network_usage WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setLong(1, startMillis);
            pstmt.setLong(2, endMillis);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new UsageRecord(
                        rs.getLong("timestamp"),
                        rs.getLong("download_bytes"),
                        rs.getLong("upload_bytes")));
            }
        } catch (SQLException e) {
            System.err.println("Error querying records: " + e.getMessage());
        }
        return records;
    }

    /**
     * Aggregates data for efficient plotting of large time ranges
     */
    public List<UsageRecord> getAggregatedUsage(long startMillis, long endMillis, long intervalMillis) {
        List<UsageRecord> records = new ArrayList<>();
        // SQLite integer division for grouping
        String sql = "SELECT (timestamp / ?) * ? as bucket, " +
                "SUM(download_bytes) as total_down, " +
                "SUM(upload_bytes) as total_up " +
                "FROM network_usage " +
                "WHERE timestamp BETWEEN ? AND ? " +
                "GROUP BY bucket " +
                "ORDER BY bucket ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setLong(1, intervalMillis);
            pstmt.setLong(2, intervalMillis);
            pstmt.setLong(3, startMillis);
            pstmt.setLong(4, endMillis);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new UsageRecord(
                        rs.getLong("bucket"), // timestamp bucket
                        rs.getLong("total_down"),
                        rs.getLong("total_up")));
            }
        } catch (SQLException e) {
            System.err.println("Error querying aggregated records: " + e.getMessage());
        }
        return records;
    }

    public UsageRecord getTotalUsage(long startMillis, long endMillis) {
        String sql = "SELECT SUM(download_bytes) as total_down, SUM(upload_bytes) as total_up FROM network_usage WHERE timestamp BETWEEN ? AND ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setLong(1, startMillis);
            pstmt.setLong(2, endMillis);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new UsageRecord(
                        endMillis,
                        rs.getLong("total_down"),
                        rs.getLong("total_up"));
            }
        } catch (SQLException e) {
            System.err.println("Error querying total usage: " + e.getMessage());
        }
        return new UsageRecord(endMillis, 0, 0);
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}

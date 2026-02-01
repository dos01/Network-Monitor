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

        String settingsSql = "CREATE TABLE IF NOT EXISTS settings (" +
                "key TEXT PRIMARY KEY," +
                "value TEXT" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(indexSql);
            stmt.execute(settingsSql);
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

    public List<UsageRecord> getDailyUsage(long startMillis, long endMillis) {
        List<UsageRecord> records = new ArrayList<>();
        // Group by day using SQLite date formatting
        String sql = "SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') as day, " +
                "SUM(download_bytes) as total_down, " +
                "SUM(upload_bytes) as total_up, " +
                "MAX(timestamp) as last_ts " +
                "FROM network_usage " +
                "WHERE timestamp BETWEEN ? AND ? " +
                "GROUP BY day " +
                "ORDER BY day ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, startMillis);
            pstmt.setLong(2, endMillis);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // We reuse UsageRecord; we'll store the day string in the 'date' field in CSV
                // later,
                // but for now we store the last_ts to keep it compatible with UsageRecord
                // constructor
                records.add(new UsageRecord(
                        rs.getLong("last_ts"),
                        rs.getLong("total_down"),
                        rs.getLong("total_up")));
            }
        } catch (SQLException e) {
            System.err.println("Error querying daily usage: " + e.getMessage());
        }
        return records;
    }

    public void saveSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings(key, value) VALUES(?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving setting: " + e.getMessage());
        }
    }

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            System.err.println("Error getting setting: " + e.getMessage());
        }
        return defaultValue;
    }

    public void performAutoCleanup() {
        // Delete records older than 1 year (365 days)
        long yearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000);
        clearDataInRange(0, yearAgo);
        System.out.println("Auto-cleanup: Removed records older than " + new java.util.Date(yearAgo));
    }

    public void clearDataInRange(long startMillis, long endMillis) {
        String sql = "DELETE FROM network_usage WHERE timestamp BETWEEN ? AND ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, startMillis);
            pstmt.setLong(2, endMillis);
            int rows = pstmt.executeUpdate();
            System.out.println("Cleanup: Deleted " + rows + " records.");
        } catch (SQLException e) {
            System.err.println("Error clearing data: " + e.getMessage());
        }
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

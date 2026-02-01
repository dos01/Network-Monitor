package com.networkmonitor.ui;

import com.networkmonitor.model.UsageRecord;
import com.networkmonitor.service.DatabaseManager;
import com.networkmonitor.service.NetworkTracker;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.PrintWriter;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardController {

    @FXML
    private Label downloadSpeedLabel;
    @FXML
    private Label uploadSpeedLabel;
    @FXML
    private Label totalDownloadLabel;
    @FXML
    private Label totalUploadLabel;
    @FXML
    private Label rangeDescriptionLabel;
    @FXML
    private HBox filterBar;
    @FXML
    private AreaChart<String, Number> usageChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    private NetworkTracker networkTracker;
    private DatabaseManager databaseManager;
    private ScheduledExecutorService executorService;
    private XYChart.Series<String, Number> downloadSeries;
    private XYChart.Series<String, Number> uploadSeries;

    private long totalDownloadBytes = 0;
    private long totalUploadBytes = 0;
    private long lastSelectionStart = 0;
    private long lastSelectionEnd = 0;

    @FXML
    public void initialize() {
        networkTracker = new NetworkTracker();
        databaseManager = DatabaseManager.getInstance();

        setupChart();

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::updateNetworkStats, 0, 2, TimeUnit.SECONDS);

        // Load initial data (e.g., last 30 mins)
        loadChartData(System.currentTimeMillis() - 1800 * 1000, System.currentTimeMillis());
    }

    private void setupChart() {
        downloadSeries = new XYChart.Series<>();
        downloadSeries.setName("Download");
        uploadSeries = new XYChart.Series<>();
        uploadSeries.setName("Upload");

        usageChart.getData().addAll(downloadSeries, uploadSeries);
        xAxis.setLabel("Time");
        yAxis.setLabel("Usage (MB)");
        usageChart.setAnimated(false); // Disable animation for real-time updates
        usageChart.setCreateSymbols(false); // Disable symbols to save memory and CPU
    }

    private void updateNetworkStats() {
        UsageRecord record = networkTracker.getNetworkUsageDelta();
        databaseManager.insertUsage(record);

        // If in live mode, calculate totals for the rolling window
        UsageRecord liveTotal = null;
        if (isLive) {
            long now = System.currentTimeMillis();
            liveTotal = databaseManager.getTotalUsage(now - currentWindowMillis, now);
        }

        final UsageRecord finalLiveTotal = liveTotal;

        Platform.runLater(() -> {
            updateLabels(record);
            if (isLive) {
                updateChart(record);
                if (finalLiveTotal != null) {
                    totalDownloadLabel.setText(formatSize(finalLiveTotal.getDownloadBytes()));
                    totalUploadLabel.setText(formatSize(finalLiveTotal.getUploadBytes()));
                }
            }
        });
    }

    private void updateLabels(UsageRecord record) {
        downloadSpeedLabel.setText(formatSpeed(record.getDownloadBytes()));
        uploadSpeedLabel.setText(formatSpeed(record.getUploadBytes()));
    }

    private void updateChart(UsageRecord record) {
        String timeLabel = new SimpleDateFormat("HH:mm:ss").format(new Date(record.getTimestamp()));

        // Add new data (converted to MB)
        downloadSeries.getData().add(new XYChart.Data<>(timeLabel, record.getDownloadBytes() / (1024.0 * 1024.0)));
        uploadSeries.getData().add(new XYChart.Data<>(timeLabel, record.getUploadBytes() / (1024.0 * 1024.0)));

        // Enforce sliding window based on time, not just count
        // removing points older than window
        // But for performance, limit count is easier.
        // Let's assume 1 point per second.
        int maxPoints = (int) (currentWindowMillis / 1000);

        if (downloadSeries.getData().size() > maxPoints) {
            downloadSeries.getData().remove(0);
            uploadSeries.getData().remove(0);
        }
    }

    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024)
            return bytesPerSecond + " B/s";
        if (bytesPerSecond < 1024 * 1024)
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private boolean isLive = true;
    private long currentWindowMillis = 60 * 60 * 1000; // Default 1 hour

    @FXML
    public void handleTimeFilter(ActionEvent event) {
        if (event.getSource() instanceof javafx.scene.control.Button) {
            javafx.scene.control.Button btn = (javafx.scene.control.Button) event.getSource();
            String text = btn.getText();
            long duration = parseDuration(text);

            // "Live" mode for 1 hour or less
            isLive = duration <= 60 * 60 * 1000;
            currentWindowMillis = duration;

            rangeDescriptionLabel.setText("Last " + text + (isLive ? " (Live)" : ""));

            // Reload chart
            long now = System.currentTimeMillis();
            reloadChart(now - duration, now);

            // Update UI state
            setActiveButton(btn);
        }
    }

    private void setActiveButton(javafx.scene.control.Button activeBtn) {
        for (javafx.scene.Node node : filterBar.getChildren()) {
            if (node instanceof javafx.scene.control.Button) {
                node.getStyleClass().remove("active");
            }
        }
        activeBtn.getStyleClass().add("active");
    }

    private long parseDuration(String text) {
        switch (text) {
            case "5 Min":
                return 5 * 60 * 1000;
            case "15 Min":
                return 15 * 60 * 1000;
            case "30 Min":
                return 30 * 60 * 1000;
            case "1 Hour":
                return 60 * 60 * 1000;
            case "3 Hours":
                return 3 * 60 * 60 * 1000;
            case "24 Hours":
                return 24 * 60 * 60 * 1000;
            case "1 Week":
                return 7 * 24 * 60 * 60 * 1000;
            case "1 Month":
                return 30L * 24 * 60 * 60 * 1000;
            case "1 Year":
                return 365L * 24 * 60 * 60 * 1000;
            default:
                return 60 * 60 * 1000;
        }
    }

    private void reloadChart(long start, long end) {
        this.lastSelectionStart = start;
        this.lastSelectionEnd = end;
        downloadSeries.getData().clear();
        uploadSeries.getData().clear();

        loadChartData(start, end);
        updateTotals(start, end);
    }

    private void updateTotals(long start, long end) {
        UsageRecord total = databaseManager.getTotalUsage(start, end);
        totalDownloadLabel.setText(formatSize(total.getDownloadBytes()));
        totalUploadLabel.setText(formatSize(total.getUploadBytes()));
    }

    @FXML
    public void handleCustomFilter(ActionEvent event) {
        javafx.scene.control.Dialog<javafx.util.Pair<java.time.LocalDate, java.time.LocalDate>> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Custom Time Range");
        dialog.setHeaderText("Select Start and End Date");

        javafx.scene.control.ButtonType loginButtonType = new javafx.scene.control.ButtonType("Apply",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        javafx.scene.control.DatePicker startDate = new javafx.scene.control.DatePicker();
        javafx.scene.control.DatePicker endDate = new javafx.scene.control.DatePicker();

        grid.add(new javafx.scene.control.Label("Start:"), 0, 0);
        grid.add(startDate, 1, 0);
        grid.add(new javafx.scene.control.Label("End:"), 0, 1);
        grid.add(endDate, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(startDate::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new javafx.util.Pair<>(startDate.getValue(), endDate.getValue());
            }
            return null;
        });

        java.util.Optional<javafx.util.Pair<java.time.LocalDate, java.time.LocalDate>> result = dialog.showAndWait();

        result.ifPresent(range -> {
            if (range.getKey() != null && range.getValue() != null) {
                // Start of start date
                long startMillis = range.getKey().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                        .toEpochMilli();
                // End of end date (approx) or start of next day
                long endMillis = range.getValue().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                        .toEpochMilli();

                isLive = false; // Disable live updates
                reloadChart(startMillis, endMillis);

                String startStr = range.getKey().toString();
                String endStr = range.getValue().toString();
                rangeDescriptionLabel.setText("Range: " + startStr + " to " + endStr);
            }
        });
    }

    private void loadChartData(long start, long end) {
        boolean useRawData = isLive && (end - start) <= 3600 * 1000;
        List<UsageRecord> records;

        if (useRawData) {
            records = databaseManager.getUsageInRange(start, end);
        } else {
            long interval = (end - start) / 60;
            if (interval < 1000)
                interval = 1000;
            records = databaseManager.getAggregatedUsage(start, end, interval);

            // Note: Since we don't have setters in UsageRecord, we'll handle the scaling
            // in the loop below by dividing the sum by (interval / 1000)
            final double secondsPerBucket = interval / 1000.0;
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            for (UsageRecord record : records) {
                String timeLabel = sdf.format(new Date(record.getTimestamp()));
                double downloadMBs = (record.getDownloadBytes() / secondsPerBucket) / (1024.0 * 1024.0);
                double uploadMBs = (record.getUploadBytes() / secondsPerBucket) / (1024.0 * 1024.0);
                downloadSeries.getData().add(new XYChart.Data<>(timeLabel, downloadMBs));
                uploadSeries.getData().add(new XYChart.Data<>(timeLabel, uploadMBs));
            }
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        for (UsageRecord record : records) {
            String timeLabel = sdf.format(new Date(record.getTimestamp()));
            downloadSeries.getData().add(new XYChart.Data<>(timeLabel, record.getDownloadBytes() / (1024.0 * 1024.0)));
            uploadSeries.getData().add(new XYChart.Data<>(timeLabel, record.getUploadBytes() / (1024.0 * 1024.0)));
        }
    }

    @FXML
    public void handleExport(ActionEvent event) {
        long start, end;
        if (isLive) {
            end = System.currentTimeMillis();
            start = end - currentWindowMillis;
        } else {
            start = lastSelectionStart;
            end = lastSelectionEnd;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Usage Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("network_usage_export.csv");

        java.io.File file = fileChooser.showSaveDialog(filterBar.getScene().getWindow());
        if (file != null) {
            exportToCSV(file, start, end);
        }
    }

    private void exportToCSV(java.io.File file, long start, long end) {
        List<UsageRecord> records = databaseManager.getDailyUsage(start, end);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("Date,Download_MB,Upload_MB,Total_MB");
            for (UsageRecord record : records) {
                double downMB = record.getDownloadBytes() / (1024.0 * 1024.0);
                double upMB = record.getUploadBytes() / (1024.0 * 1024.0);
                double totalMB = downMB + upMB;

                writer.printf("%s,%.2f,%.2f,%.2f%n",
                        sdf.format(new Date(record.getTimestamp())),
                        downMB,
                        upMB,
                        totalMB);
            }

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText(null);
            alert.setContentText("Daily report exported successfully to " + file.getName());
            alert.showAndWait();

        } catch (IOException e) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText("Failed to export report");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}

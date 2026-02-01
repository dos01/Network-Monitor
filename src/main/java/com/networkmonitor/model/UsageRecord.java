package com.networkmonitor.model;

public class UsageRecord {
    private long timestamp;
    private long downloadBytes;
    private long uploadBytes;

    public UsageRecord(long timestamp, long downloadBytes, long uploadBytes) {
        this.timestamp = timestamp;
        this.downloadBytes = downloadBytes;
        this.uploadBytes = uploadBytes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getDownloadBytes() {
        return downloadBytes;
    }

    public long getUploadBytes() {
        return uploadBytes;
    }

    @Override
    public String toString() {
        return "UsageRecord{" +
                "timestamp=" + timestamp +
                ", downloadBytes=" + downloadBytes +
                ", uploadBytes=" + uploadBytes +
                '}';
    }
}

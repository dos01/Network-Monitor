package com.networkmonitor.service;

import oshi.SystemInfo;
import oshi.hardware.NetworkIF;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.Collections;
import java.util.List;

public class NetworkTracker {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private long previousBytesRecv = 0;
    private long previousBytesSent = 0;
    private boolean isFirstRun = true;

    public NetworkTracker() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        initBaseLine();
    }

    private void initBaseLine() {
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            previousBytesRecv += net.getBytesRecv();
            previousBytesSent += net.getBytesSent();
        }
    }

    /**
     * Calculates the bytes received and sent since the last call.
     * 
     * @return UsageRecord with current timestamp and delta bytes.
     */
    public synchronized com.networkmonitor.model.UsageRecord getNetworkUsageDelta() {
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();

        long currentTotalRecv = 0;
        long currentTotalSent = 0;

        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            currentTotalRecv += net.getBytesRecv();
            currentTotalSent += net.getBytesSent();
        }

        long deltaRecv = 0;
        long deltaSent = 0;

        if (!isFirstRun) {
            deltaRecv = currentTotalRecv - previousBytesRecv;
            deltaSent = currentTotalSent - previousBytesSent;
        } else {
            isFirstRun = false;
        }

        // Handle potential counter wrap-around or reset (though OSHI usually handles
        // this, simpler to ignore negative deltas)
        if (deltaRecv < 0)
            deltaRecv = 0;
        if (deltaSent < 0)
            deltaSent = 0;

        previousBytesRecv = currentTotalRecv;
        previousBytesSent = currentTotalSent;

        return new com.networkmonitor.model.UsageRecord(
                System.currentTimeMillis(),
                deltaRecv,
                deltaSent);
    }
}

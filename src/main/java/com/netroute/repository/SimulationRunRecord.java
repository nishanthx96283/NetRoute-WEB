package com.netroute.repository;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Historical snapshot of packet simulation analytics recorded in the database.
 * <p>
 * History retention:
 * {@code topologyId} may be null if the original topology has since been permanently deleted,
 * but {@code topologyName} is denormalized and preserved forever.
 */
public final class SimulationRunRecord {

    private final int id;
    private final Integer topologyId;
    private final String topologyName;
    private final LocalDateTime runTimestamp;
    private final long simulationTicks;
    private final int totalPackets;
    private final int deliveredPackets;
    private final int failedPackets;
    private final int droppedPackets;
    private final double deliveryRatio;
    private final double avgDeliveryDelay;
    private final int linkFailures;
    private final int reroutes;
    private final String notes;

    public SimulationRunRecord(
            int id,
            Integer topologyId,
            String topologyName,
            LocalDateTime runTimestamp,
            long simulationTicks,
            int totalPackets,
            int deliveredPackets,
            int failedPackets,
            int droppedPackets,
            double deliveryRatio,
            double avgDeliveryDelay,
            int linkFailures,
            int reroutes,
            String notes) {
        this.id = id;
        this.topologyId = topologyId;
        this.topologyName = Objects.requireNonNull(topologyName, "topologyName is required");
        this.runTimestamp = runTimestamp == null ? LocalDateTime.now() : runTimestamp;
        this.simulationTicks = simulationTicks;
        this.totalPackets = totalPackets;
        this.deliveredPackets = deliveredPackets;
        this.failedPackets = failedPackets;
        this.droppedPackets = droppedPackets;
        this.deliveryRatio = deliveryRatio;
        this.avgDeliveryDelay = avgDeliveryDelay;
        this.linkFailures = linkFailures;
        this.reroutes = reroutes;
        this.notes = notes == null ? "" : notes;
    }

    public int getId() {
        return id;
    }

    public Integer getTopologyId() {
        return topologyId;
    }

    public String getTopologyName() {
        return topologyName;
    }

    public LocalDateTime getRunTimestamp() {
        return runTimestamp;
    }

    public long getSimulationTicks() {
        return simulationTicks;
    }

    public int getTotalPackets() {
        return totalPackets;
    }

    public int getDeliveredPackets() {
        return deliveredPackets;
    }

    public int getFailedPackets() {
        return failedPackets;
    }

    public int getDroppedPackets() {
        return droppedPackets;
    }

    public double getDeliveryRatio() {
        return deliveryRatio;
    }

    public double getAvgDeliveryDelay() {
        return avgDeliveryDelay;
    }

    public int getLinkFailures() {
        return linkFailures;
    }

    public int getReroutes() {
        return reroutes;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "SimulationRun[ID=" + id + ", Topology=" + topologyName + ", Delivery="
                + String.format("%.1f", deliveryRatio) + "%, Packets=" + deliveredPackets + "/" + totalPackets + "]";
    }
}

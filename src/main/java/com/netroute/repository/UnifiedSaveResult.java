package com.netroute.repository;

import java.util.Objects;

/**
 * Result record returned by a unified save operation in {@link TopologyRepository}
 * and {@link com.netroute.service.TopologyStorageService}.
 * <p>
 * Contains metadata about the saved/updated topology and whether a corresponding
 * simulation execution snapshot was recorded in the same atomic transaction.
 */
public class UnifiedSaveResult {

    private final int topologyId;
    private final String topologyName;
    private final boolean created;
    private final Integer simulationRunId;

    public UnifiedSaveResult(int topologyId, String topologyName, boolean created, Integer simulationRunId) {
        this.topologyId = topologyId;
        this.topologyName = Objects.requireNonNull(topologyName, "topologyName is required").trim();
        this.created = created;
        this.simulationRunId = simulationRunId;
    }

    public int getTopologyId() {
        return topologyId;
    }

    public String getTopologyName() {
        return topologyName;
    }

    /**
     * @return {@code true} if a new topology row was inserted into MySQL;
     *         {@code false} if an existing topology row was updated/overwritten.
     */
    public boolean isCreated() {
        return created;
    }

    /**
     * @return generated simulation run ID, or {@code null} if no simulation run was saved
     */
    public Integer getSimulationRunId() {
        return simulationRunId;
    }

    /**
     * @return {@code true} if a simulation run was persisted alongside the topology
     */
    public boolean hasSimulationRun() {
        return simulationRunId != null && simulationRunId > 0;
    }

    @Override
    public String toString() {
        return "UnifiedSaveResult{" +
                "topologyId=" + topologyId +
                ", topologyName='" + topologyName + '\'' +
                ", created=" + created +
                ", simulationRunId=" + simulationRunId +
                '}';
    }
}

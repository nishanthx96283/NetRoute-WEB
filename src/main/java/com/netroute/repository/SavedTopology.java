package com.netroute.repository;

import com.netroute.graph.Network;

import java.util.Objects;

/**
 * Full topology representation loaded from the database, containing both the
 * header summary metadata and the fully reconstructed {@link Network} graph.
 */
public final class SavedTopology {

    private final TopologySummary summary;
    private final Network network;

    public SavedTopology(TopologySummary summary, Network network) {
        this.summary = Objects.requireNonNull(summary, "summary is required");
        this.network = Objects.requireNonNull(network, "network is required");
    }

    public TopologySummary getSummary() {
        return summary;
    }

    public Network getNetwork() {
        return network;
    }

    public int getId() {
        return summary.getId();
    }

    public String getName() {
        return summary.getName();
    }

    public String getDescription() {
        return summary.getDescription();
    }

    @Override
    public String toString() {
        return "SavedTopology[" + summary.getName() + ", ID=" + summary.getId() + "]";
    }
}

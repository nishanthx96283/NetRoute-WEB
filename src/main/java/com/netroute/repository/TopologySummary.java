package com.netroute.repository;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lightweight metadata summary of a saved topology stored in the database.
 * Used for listing topologies in history and selector dropdowns without loading the full graph.
 */
public final class TopologySummary {

    private final int id;
    private final String name;
    private final String description;
    private final int nodeCount;
    private final int linkCount;
    private final boolean archived;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public TopologySummary(
            int id,
            String name,
            String description,
            int nodeCount,
            int linkCount,
            boolean archived,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name is required");
        this.description = description == null ? "" : description;
        this.nodeCount = nodeCount;
        this.linkCount = linkCount;
        this.archived = archived;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getLinkCount() {
        return linkCount;
    }

    public boolean isArchived() {
        return archived;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TopologySummary other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return name + " (" + nodeCount + " nodes, " + linkCount + " links)";
    }
}

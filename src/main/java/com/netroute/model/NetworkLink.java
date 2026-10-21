package com.netroute.model;

/**
 * Weighted edge between two nodes.
 * <p>
 * Routing cost is the Dijkstra weight. Simulated delay is time, not cost.
 * Capacity is a later simulation limit (packets per tick) and is stored here only.
 * Inactive links remain in the graph but must be ignored by routing.
 */
public final class NetworkLink {

    private final String id;
    private final String sourceId;
    private final String destinationId;
    private final boolean directed;
    private double cost;
    private double delay;
    private int capacity;
    private boolean active;

    public NetworkLink(
            String id,
            String sourceId,
            String destinationId,
            double cost,
            double delay,
            int capacity,
            boolean directed) {
        this.id = requireToken(id, "Link ID");
        this.sourceId = requireToken(sourceId, "Source node ID");
        this.destinationId = requireToken(destinationId, "Destination node ID");
        if (this.sourceId.equals(this.destinationId)) {
            throw new IllegalArgumentException("Self-loops are not allowed: " + this.sourceId);
        }
        this.directed = directed;
        this.active = true;
        setCost(cost);
        setDelay(delay);
        setCapacity(capacity);
    }

    public String getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public boolean isDirected() {
        return directed;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        if (Double.isNaN(cost) || cost < 0.0) {
            throw new IllegalArgumentException("Routing cost must be non-negative: " + cost);
        }
        this.cost = cost;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        if (Double.isNaN(delay) || delay < 0.0) {
            throw new IllegalArgumentException("Simulated delay must be non-negative: " + delay);
        }
        this.delay = delay;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1: " + capacity);
        }
        this.capacity = capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * The endpoint opposite {@code nodeId} on this link.
     */
    public String otherEndpoint(String nodeId) {
        if (sourceId.equals(nodeId)) {
            return destinationId;
        }
        if (destinationId.equals(nodeId)) {
            return sourceId;
        }
        throw new IllegalArgumentException("Node " + nodeId + " is not an endpoint of link " + id);
    }

    public boolean connects(String nodeA, String nodeB) {
        return (sourceId.equals(nodeA) && destinationId.equals(nodeB))
                || (!directed && sourceId.equals(nodeB) && destinationId.equals(nodeA));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NetworkLink other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        String arrow = directed ? " -> " : " -- ";
        return "Link(" + id + ": " + sourceId + arrow + destinationId + ", cost=" + cost + ")";
    }

    private static String requireToken(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must be a non-blank string");
        }
        return value.trim();
    }
}

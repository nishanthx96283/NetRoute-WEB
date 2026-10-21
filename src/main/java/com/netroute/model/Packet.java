package com.netroute.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A simulated datagram. Identity is the packet ID. There is no socket I/O;
 * timing fields are simulation ticks, not wall-clock time.
 */
public final class Packet {

    private final String id;
    private final String sourceId;
    private final String destinationId;
    private final int payloadSize;
    private final long createdAt;

    private PacketStatus status;
    private Route route;
    private int hopIndex;
    private String currentNodeId;
    private Long sentAt;
    private Long deliveredAt;
    private String reason;
    private int rerouteCount;
    private final List<String> visitedNodeIds = new ArrayList<>();

    public Packet(String id, String sourceId, String destinationId, int payloadSize, long createdAt) {
        this.id = requireToken(id, "Packet ID");
        this.sourceId = requireToken(sourceId, "Source node ID");
        this.destinationId = requireToken(destinationId, "Destination node ID");
        if (payloadSize < 1) {
            throw new IllegalArgumentException("Payload size must be at least 1: " + payloadSize);
        }
        if (createdAt < 0) {
            throw new IllegalArgumentException("createdAt must be non-negative: " + createdAt);
        }
        this.payloadSize = payloadSize;
        this.createdAt = createdAt;
        this.status = PacketStatus.CREATED;
        this.currentNodeId = this.sourceId;
        this.visitedNodeIds.add(this.sourceId);
        this.hopIndex = 0;
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

    public int getPayloadSize() {
        return payloadSize;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public PacketStatus getStatus() {
        return status;
    }

    public Optional<Route> getRoute() {
        return Optional.ofNullable(route);
    }

    public int getHopIndex() {
        return hopIndex;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public Optional<Long> getSentAt() {
        return Optional.ofNullable(sentAt);
    }

    public Optional<Long> getDeliveredAt() {
        return Optional.ofNullable(deliveredAt);
    }

    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    public int getRerouteCount() {
        return rerouteCount;
    }

    public List<String> getVisitedNodeIds() {
        return List.copyOf(visitedNodeIds);
    }

    public boolean isTerminal() {
        return status == PacketStatus.DELIVERED
                || status == PacketStatus.FAILED
                || status == PacketStatus.DROPPED;
    }

    public Optional<NetworkLink> nextLink() {
        if (route == null || hopIndex < 0 || hopIndex >= route.getLinks().size()) {
            return Optional.empty();
        }
        return Optional.of(route.getLinks().get(hopIndex));
    }

    public boolean remainingRouteUses(String linkId) {
        if (route == null) {
            return false;
        }
        List<NetworkLink> hops = route.getLinks();
        for (int i = hopIndex; i < hops.size(); i++) {
            if (hops.get(i).getId().equals(linkId)) {
                return true;
            }
        }
        return false;
    }

    public void assignRoute(Route route) {
        this.route = Objects.requireNonNull(route, "route is required");
        this.hopIndex = 0;
    }

    public void markQueued() {
        this.status = PacketStatus.QUEUED;
        this.reason = null;
    }

    public void markSent(long tick) {
        if (sentAt == null) {
            sentAt = tick;
        }
        this.status = PacketStatus.IN_TRANSIT;
    }

    public void arriveAt(String nodeId) {
        this.currentNodeId = nodeId;
        this.hopIndex++;
        if (visitedNodeIds.isEmpty() || !visitedNodeIds.getLast().equals(nodeId)) {
            visitedNodeIds.add(nodeId);
        }
    }

    public void markDelivered(long tick) {
        this.status = PacketStatus.DELIVERED;
        this.deliveredAt = tick;
        this.currentNodeId = destinationId;
        this.reason = null;
        if (visitedNodeIds.isEmpty() || !visitedNodeIds.getLast().equals(destinationId)) {
            visitedNodeIds.add(destinationId);
        }
    }

    public void markFailed(String reason) {
        this.status = PacketStatus.FAILED;
        this.reason = requireToken(reason, "Failure reason");
    }

    public void markDropped(String reason) {
        this.status = PacketStatus.DROPPED;
        this.reason = requireToken(reason, "Drop reason");
    }

    public void markRerouted(Route newRoute) {
        assignRoute(newRoute);
        rerouteCount++;
        markQueued();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Packet other)) {
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
        return "Packet(" + id + ": " + sourceId + " -> " + destinationId + ", " + status + ")";
    }

    private static String requireToken(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must be a non-blank string");
        }
        return value.trim();
    }
}

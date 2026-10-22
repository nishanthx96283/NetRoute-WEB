package com.netroute.statistics;

import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.Packet;
import com.netroute.model.PacketStatus;
import com.netroute.model.Route;
import com.netroute.simulation.SimulationEngine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only statistics computed from a simulation run.
 * <p>
 * All time values are <strong>simulated ticks</strong>, not wall-clock time.
 * The simulation engine advances an integer tick counter; these statistics
 * report measurements in those ticks.
 * <p>
 * This class does not modify the simulation or the network. It reads the
 * current state of the engine each time a method is called, so it always
 * reflects the latest simulation data.
 */
public final class NetworkStatistics {

    private final SimulationEngine engine;
    private final Network network;

    public NetworkStatistics(SimulationEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine is required");
        this.network = engine.getNetwork();
    }

    // ---- Packet counts ----

    /** Total number of packets submitted to the simulation. */
    public int getTotalPackets() {
        return engine.getPackets().size();
    }

    /** Packets that reached their destination. */
    public int getDeliveredCount() {
        return engine.getDeliveredCount();
    }

    /** Packets that failed at submission (e.g. unknown endpoints, no route). */
    public int getFailedCount() {
        return engine.getFailedCount();
    }

    /** Packets dropped mid-transit (e.g. link failure with no alternative). */
    public int getDroppedCount() {
        return engine.getDroppedCount();
    }

    // ---- Delivery ratio ----

    /**
     * Percentage of submitted packets that were delivered.
     * Returns 0.0 when no packets have been submitted (avoids division by zero).
     */
    public double getDeliveryRatio() {
        int total = getTotalPackets();
        if (total == 0) {
            return 0.0;
        }
        return (double) getDeliveredCount() / total * 100.0;
    }

    // ---- Timing (simulated ticks, not wall-clock time) ----

    /**
     * Average delivery delay in simulated ticks across all delivered packets.
     * Delay for each packet is {@code deliveredAt - createdAt}, measuring the
     * full simulated time from submission to delivery.
     * Returns 0.0 when no packets have been delivered (avoids division by zero).
     */
    public double getAverageDeliveryDelay() {
        List<Packet> allPackets = engine.getPackets();
        long totalDelay = 0;
        int count = 0;

        for (Packet packet : allPackets) {
            if (packet.getStatus() == PacketStatus.DELIVERED) {
                long delivered = packet.getDeliveredAt().orElse(0L);
                long created = packet.getCreatedAt();
                totalDelay += (delivered - created);
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }
        return (double) totalDelay / count;
    }

    // ---- Route cost ----

    /**
     * Total routing cost of the given packet's current route.
     * Returns empty if the packet ID is unknown or the packet has no route
     * (for example, a packet that failed before routing).
     */
    public Optional<Double> getRouteCost(String packetId) {
        Optional<Packet> packet = engine.findPacket(packetId);
        if (packet.isEmpty()) {
            return Optional.empty();
        }
        return packet.get().getRoute().map(Route::getTotalCost);
    }

    // ---- Network topology ----

    /** Total number of nodes in the network. */
    public int getTotalNodes() {
        return network.nodeCount();
    }

    /** Total number of links (active and inactive) in the network. */
    public int getTotalLinks() {
        return network.linkCount();
    }

    /** Number of links currently active (usable for routing). */
    public int getActiveLinks() {
        int count = 0;
        for (NetworkLink link : network.getLinks().values()) {
            if (link.isActive()) {
                count++;
            }
        }
        return count;
    }

    /** Number of links currently inactive (ignored by routing). */
    public int getInactiveLinks() {
        return getTotalLinks() - getActiveLinks();
    }

    // ---- Failure and reroute metrics ----

    /** Number of times {@code failLink} was called on the simulation engine. */
    public int getLinkFailureCount() {
        return engine.getLinkFailureCount();
    }

    /**
     * Total number of successful reroutes across all packets.
     * A reroute is counted as successful when Dijkstra found an alternative
     * path after a link on the packet's remaining route failed. The packet's
     * {@code rerouteCount} only increments when an alternative was found;
     * if no alternative exists, the packet is dropped instead.
     */
    public int getTotalReroutes() {
        int total = 0;
        for (Packet packet : engine.getPackets()) {
            total += packet.getRerouteCount();
        }
        return total;
    }

    // ---- Summary ----

    /**
     * Human-readable summary suitable for console or Swing display.
     * All delay values are labelled "ticks" to distinguish simulated time
     * from real execution time.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== NetRoute Simulation Statistics ===\n");
        sb.append(String.format("Packets submitted:     %d%n", getTotalPackets()));
        sb.append(String.format("Delivered:             %d%n", getDeliveredCount()));
        sb.append(String.format("Failed:                %d%n", getFailedCount()));
        sb.append(String.format("Dropped:               %d%n", getDroppedCount()));
        sb.append(String.format("Delivery ratio:        %.1f%%%n", getDeliveryRatio()));
        sb.append(String.format("Avg delivery delay:    %.2f ticks%n", getAverageDeliveryDelay()));
        sb.append(String.format("Total nodes:           %d%n", getTotalNodes()));
        sb.append(String.format("Total links:           %d%n", getTotalLinks()));
        sb.append(String.format("Active links:          %d%n", getActiveLinks()));
        sb.append(String.format("Inactive links:        %d%n", getInactiveLinks()));
        sb.append(String.format("Link failures:         %d%n", getLinkFailureCount()));
        sb.append(String.format("Successful reroutes:   %d%n", getTotalReroutes()));
        return sb.toString();
    }
}

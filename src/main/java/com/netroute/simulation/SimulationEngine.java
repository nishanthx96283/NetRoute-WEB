package com.netroute.simulation;

import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.Packet;
import com.netroute.model.PacketStatus;
import com.netroute.model.Route;
import com.netroute.routing.DijkstraRouter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Discrete-event packet simulator. No sockets and no wall-clock waits: time is
 * an integer tick counter.
 * <p>
 * Transmission model:
 * <ul>
 *   <li>Each link has a FIFO {@link PacketQueue}.</li>
 *   <li>{@link NetworkLink#getCapacity()} is the maximum number of packets that
 *       may <em>start</em> traversing that link in one tick.</li>
 *   <li>Hop duration is {@code max(1, ceil(delay))} ticks. Payload size is
 *       recorded on the packet but does not change duration in this engine.</li>
 *   <li>Links are served in lexicographic ID order so a run is reproducible.</li>
 * </ul>
 * Packets are routed with {@link DijkstraRouter}. Inactive links are ignored.
 * If a link on a live packet's remaining route fails, the engine reroutes from
 * the packet's current node; if Dijkstra finds nothing, the packet is dropped.
 */
public final class SimulationEngine {

    static final long MAX_TICKS = 1_000_000L;

    private final Network network;
    private final DijkstraRouter router;
    private final Map<String, PacketQueue> queues = new LinkedHashMap<>();
    private final List<Transmission> inFlight = new ArrayList<>();
    private final List<Packet> packets = new ArrayList<>();

    private long time;
    private int nextPacketNumber = 1;
    private int sentCount;
    private int deliveredCount;
    private int failedCount;
    private int droppedCount;
    private int linkFailureCount;

    public SimulationEngine(Network network) {
        this(network, new DijkstraRouter());
    }

    public SimulationEngine(Network network, DijkstraRouter router) {
        this.network = Objects.requireNonNull(network, "network is required");
        this.router = Objects.requireNonNull(router, "router is required");
        for (String linkId : network.getLinks().keySet()) {
            queues.put(linkId, new PacketQueue(linkId));
        }
    }

    public Network getNetwork() {
        return network;
    }

    public long getCurrentTime() {
        return time;
    }

    public List<Packet> getPackets() {
        return List.copyOf(packets);
    }

    public Optional<Packet> findPacket(String packetId) {
        for (Packet packet : packets) {
            if (packet.getId().equals(packetId)) {
                return Optional.of(packet);
            }
        }
        return Optional.empty();
    }

    public PacketQueue queueFor(String linkId) {
        return queues.computeIfAbsent(linkId, PacketQueue::new);
    }

    public int getSentCount() {
        return sentCount;
    }

    public int getDeliveredCount() {
        return deliveredCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getDroppedCount() {
        return droppedCount;
    }

    public int getLinkFailureCount() {
        return linkFailureCount;
    }

    /**
     * Accepts a packet at the current tick. Unknown endpoints or an empty graph
     * fail the packet without throwing so other submissions can continue.
     */
    public Packet submitPacket(String sourceId, String destinationId, int payloadSize) {
        String source = requireId(sourceId, "Source node ID");
        String destination = requireId(destinationId, "Destination node ID");
        Packet packet = new Packet(nextPacketId(), source, destination, payloadSize, time);
        packets.add(packet);

        if (network.nodeCount() == 0 || !network.containsNode(source) || !network.containsNode(destination)) {
            fail(packet, "Unknown source or destination");
            return packet;
        }

        Optional<Route> route = router.findRoute(network, source, destination);
        if (route.isEmpty()) {
            fail(packet, "No available route");
            return packet;
        }
        packet.assignRoute(route.get());
        sentCount++;
        if (source.equals(destination)) {
            deliver(packet);
            return packet;
        }
        enqueueNextHop(packet);
        return packet;
    }

    /**
     * Deactivates a link and immediately reroutes (or drops) every live packet
     * whose remaining route uses it.
     */
    public void failLink(String linkId) {
        network.setLinkActive(linkId, false);
        linkFailureCount++;
        List<Packet> affected = new ArrayList<>();
        for (Packet packet : packets) {
            if (!packet.isTerminal() && packet.remainingRouteUses(linkId)) {
                affected.add(packet);
            }
        }
        affected.sort(Comparator.comparing(Packet::getId));
        for (Packet packet : affected) {
            cancelCurrentHop(packet);
            rerouteOrDrop(packet, "Link " + linkId + " failed");
        }
    }

    public void restoreLink(String linkId) {
        network.setLinkActive(linkId, true);
    }

    /**
     * Advance until every packet is delivered, failed or dropped.
     */
    public void run() {
        long guard = 0;
        while (!isIdle()) {
            processTick();
            if (!isIdle()) {
                time++;
            }
            if (++guard > MAX_TICKS) {
                throw new IllegalStateException("Simulation exceeded " + MAX_TICKS + " ticks");
            }
        }
    }

    /**
     * One simulation step: complete arrivals due now, then start new hops.
     * Increments time when work remains after the step.
     */
    public void step() {
        processTick();
        if (!isIdle()) {
            time++;
        }
    }

    public boolean isIdle() {
        if (!inFlight.isEmpty()) {
            return false;
        }
        for (PacketQueue queue : queues.values()) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void processTick() {
        completeArrivals();
        startTransmissions();
    }

    private void completeArrivals() {
        inFlight.sort(Comparator
                .comparingLong((Transmission tx) -> tx.arrivalTick)
                .thenComparing(tx -> tx.packet.getId()));
        List<Transmission> due = new ArrayList<>();
        List<Transmission> remaining = new ArrayList<>();
        for (Transmission tx : inFlight) {
            if (tx.arrivalTick <= time) {
                due.add(tx);
            } else {
                remaining.add(tx);
            }
        }
        inFlight.clear();
        inFlight.addAll(remaining);
        for (Transmission tx : due) {
            Packet packet = tx.packet;
            if (packet.isTerminal()) {
                continue;
            }
            NetworkLink link = network.getLink(tx.linkId);
            String nextNode = link.otherEndpoint(packet.getCurrentNodeId());
            packet.arriveAt(nextNode);
            if (nextNode.equals(packet.getDestinationId())) {
                deliver(packet);
            } else {
                enqueueNextHop(packet);
            }
        }
    }

    private void startTransmissions() {
        List<String> linkIds = new ArrayList<>(queues.keySet());
        linkIds.sort(String::compareTo);
        for (String linkId : linkIds) {
            if (!network.containsLink(linkId) || !network.getLink(linkId).isActive()) {
                continue;
            }
            PacketQueue queue = queueFor(linkId);
            int inUse = countInFlight(linkId);
            int capacity = network.getLink(linkId).getCapacity();
            while (inUse < capacity && !queue.isEmpty()) {
                Packet packet = queue.poll();
                if (packet == null || packet.isTerminal()) {
                    continue;
                }
                Optional<NetworkLink> next = packet.nextLink();
                if (next.isEmpty() || !next.get().getId().equals(linkId) || !next.get().isActive()) {
                    rerouteOrDrop(packet, "Next hop unavailable");
                    continue;
                }
                long duration = hopTicks(next.get());
                packet.markSent(time);
                inFlight.add(new Transmission(packet, linkId, time + duration));
                inUse++;
            }
        }
    }

    private void enqueueNextHop(Packet packet) {
        Optional<NetworkLink> next = packet.nextLink();
        if (next.isEmpty()) {
            if (packet.getCurrentNodeId().equals(packet.getDestinationId())) {
                deliver(packet);
            } else {
                rerouteOrDrop(packet, "Route exhausted before destination");
            }
            return;
        }
        NetworkLink link = next.get();
        if (!link.isActive()) {
            rerouteOrDrop(packet, "Next hop " + link.getId() + " is inactive");
            return;
        }
        packet.markQueued();
        queueFor(link.getId()).enqueue(packet);
    }

    private void rerouteOrDrop(Packet packet, String cause) {
        if (packet.isTerminal()) {
            return;
        }
        if (packet.getCurrentNodeId().equals(packet.getDestinationId())) {
            deliver(packet);
            return;
        }
        Optional<Route> alternative = router.findRoute(
                network, packet.getCurrentNodeId(), packet.getDestinationId());
        if (alternative.isEmpty()) {
            drop(packet, cause + "; no alternative route");
            return;
        }
        packet.markRerouted(alternative.get());
        if (packet.getCurrentNodeId().equals(packet.getDestinationId())) {
            deliver(packet);
            return;
        }
        enqueueNextHop(packet);
    }

    private void cancelCurrentHop(Packet packet) {
        packet.nextLink().ifPresent(link -> queueFor(link.getId()).remove(packet));
        inFlight.removeIf(tx -> tx.packet.equals(packet));
    }

    private void deliver(Packet packet) {
        if (packet.getStatus() == PacketStatus.DELIVERED) {
            return;
        }
        packet.markDelivered(time);
        deliveredCount++;
    }

    private void fail(Packet packet, String reason) {
        packet.markFailed(reason);
        failedCount++;
    }

    private void drop(Packet packet, String reason) {
        packet.markDropped(reason);
        droppedCount++;
    }

    private int countInFlight(String linkId) {
        int count = 0;
        for (Transmission tx : inFlight) {
            if (tx.linkId.equals(linkId) && tx.arrivalTick > time) {
                count++;
            }
        }
        return count;
    }

    private String nextPacketId() {
        return "P" + nextPacketNumber++;
    }

    /**
     * Delay is simulated time, not routing cost. Zero delay still occupies one
     * tick so the event loop always makes progress.
     */
    static long hopTicks(NetworkLink link) {
        return Math.max(1L, (long) Math.ceil(link.getDelay()));
    }

    private static String requireId(String id, String label) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(label + " must be a non-blank string");
        }
        return id.trim();
    }

    private record Transmission(Packet packet, String linkId, long arrivalTick) {
    }
}

package com.netroute.routing;

import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Dijkstra shortest-path routing over {@link Network}.
 * <p>
 * Expansion uses {@link Network#getActiveOutgoingLinks(String)} so inactive links
 * are ignored. Edge weight is {@link NetworkLink#getCost()}, never delay.
 * <p>
 * The algorithm uses {@link PriorityQueue} as a binary min-heap of tentative
 * distances. {@code poll()} always yields the unsettled entry with the smallest
 * distance (then the lexicographically smallest node ID). Decrease-key is not
 * available on {@code PriorityQueue}, so a later, better distance re-inserts a
 * new entry. Stale (worse) entries are skipped when they are polled.
 * <p>
 * Time complexity: {@code O((V + E) log V)} typical; {@code O(E log E)} worst
 * case because each relaxing edge may enqueue an extra heap entry.
 * Space complexity: {@code O(V + E)}.
 */
public final class DijkstraRouter {

    public Optional<Route> findRoute(Network network, String sourceId, String destinationId) {
        Objects.requireNonNull(network, "network is required");
        String source = requireNodeId(sourceId, "Source node ID");
        String destination = requireNodeId(destinationId, "Destination node ID");
        NetworkNode sourceNode = network.getNode(source);
        NetworkNode destinationNode = network.getNode(destination);

        if (source.equals(destination)) {
            return Optional.of(Route.trivial(sourceNode));
        }

        Map<String, Double> distance = new HashMap<>();
        Map<String, String> previousNode = new HashMap<>();
        Map<String, NetworkLink> previousLink = new HashMap<>();
        Set<String> settled = new HashSet<>();

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>();
        distance.put(source, 0.0);
        queue.add(new QueueEntry(source, 0.0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            Double bestKnown = distance.get(current.nodeId);
            if (bestKnown == null || current.distance > bestKnown) {
                continue;
            }
            if (!settled.add(current.nodeId)) {
                continue;
            }
            if (current.nodeId.equals(destination)) {
                break;
            }

            for (NetworkLink link : network.getActiveOutgoingLinks(current.nodeId)) {
                requireNonNegativeCost(link);
                String neighbour = link.otherEndpoint(current.nodeId);
                if (settled.contains(neighbour)) {
                    continue;
                }
                double candidate = current.distance + link.getCost();
                Double existing = distance.get(neighbour);
                if (existing == null || candidate < existing || (candidate == existing
                        && shouldReplaceEqualCost(previousNode.get(neighbour), current.nodeId,
                        previousLink.get(neighbour), link))) {
                    distance.put(neighbour, candidate);
                    previousNode.put(neighbour, current.nodeId);
                    previousLink.put(neighbour, link);
                    queue.add(new QueueEntry(neighbour, candidate));
                }
            }
        }

        if (!distance.containsKey(destination)) {
            return Optional.empty();
        }
        return Optional.of(rebuild(network, sourceNode, destinationNode, previousNode, previousLink));
    }

    private static Route rebuild(
            Network network,
            NetworkNode source,
            NetworkNode destination,
            Map<String, String> previousNode,
            Map<String, NetworkLink> previousLink) {
        List<NetworkNode> nodes = new ArrayList<>();
        List<NetworkLink> links = new ArrayList<>();
        String cursor = destination.getId();
        while (!cursor.equals(source.getId())) {
            NetworkLink hop = previousLink.get(cursor);
            String pred = previousNode.get(cursor);
            if (hop == null || pred == null) {
                throw new IllegalStateException("Incomplete predecessor chain at node " + cursor);
            }
            nodes.add(network.getNode(cursor));
            links.add(hop);
            cursor = pred;
        }
        nodes.add(source);
        Collections.reverse(nodes);
        Collections.reverse(links);
        double total = 0.0;
        for (NetworkLink link : links) {
            total += link.getCost();
        }
        return new Route(nodes, links, total);
    }

    /**
     * When two paths have the same cost, prefer the predecessor with the smaller
     * node ID, then the smaller link ID, so the chosen route is stable.
     */
    private static boolean shouldReplaceEqualCost(
            String existingPred,
            String candidatePred,
            NetworkLink existingLink,
            NetworkLink candidateLink) {
        int predCmp = candidatePred.compareTo(existingPred);
        if (predCmp != 0) {
            return predCmp < 0;
        }
        return candidateLink.getId().compareTo(existingLink.getId()) < 0;
    }

    private static void requireNonNegativeCost(NetworkLink link) {
        double cost = link.getCost();
        if (Double.isNaN(cost) || cost < 0.0) {
            throw new IllegalArgumentException(
                    "Routing cost must be non-negative on link " + link.getId() + ": " + cost);
        }
    }

    private static String requireNodeId(String id, String label) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(label + " must be a non-blank string");
        }
        return id.trim();
    }

    /**
     * Min-heap entry. {@link #compareTo} orders by tentative distance, then node
     * ID, so {@link PriorityQueue#poll()} selects the next closest node
     * deterministically.
     */
    private static final class QueueEntry implements Comparable<QueueEntry> {

        private final String nodeId;
        private final double distance;

        private QueueEntry(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }

        @Override
        public int compareTo(QueueEntry other) {
            int byDistance = Double.compare(this.distance, other.distance);
            if (byDistance != 0) {
                return byDistance;
            }
            return this.nodeId.compareTo(other.nodeId);
        }
    }
}

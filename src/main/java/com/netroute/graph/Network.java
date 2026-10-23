package com.netroute.graph;

import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Weighted graph stored as an adjacency list.
 * This class is independent of Swing and JDBC.
 */
public final class Network {

    private final Map<String, NetworkNode> nodes = new LinkedHashMap<>();
    private final Map<String, NetworkLink> links = new LinkedHashMap<>();
    private final Map<String, List<NetworkLink>> adjacency = new LinkedHashMap<>();

    public NetworkNode addNode(NetworkNode node) {
        Objects.requireNonNull(node, "node is required");
        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Duplicate node ID: " + node.getId());
        }
        nodes.put(node.getId(), node);
        adjacency.put(node.getId(), new ArrayList<>());
        return node;
    }

    public NetworkLink addLink(NetworkLink link) {
        Objects.requireNonNull(link, "link is required");
        if (links.containsKey(link.getId())) {
            throw new IllegalArgumentException("Duplicate link ID: " + link.getId());
        }
        requireNode(link.getSourceId());
        requireNode(link.getDestinationId());
        if (hasOverlappingEdge(link)) {
            throw new IllegalArgumentException(
                    "A link already exists between " + link.getSourceId()
                            + " and " + link.getDestinationId());
        }
        links.put(link.getId(), link);
        adjacency.get(link.getSourceId()).add(link);
        if (!link.isDirected()) {
            adjacency.get(link.getDestinationId()).add(link);
        }
        return link;
    }

    public void removeNode(String nodeId) {
        requireNode(nodeId);
        List<String> incident = new ArrayList<>();
        for (NetworkLink link : links.values()) {
            if (link.getSourceId().equals(nodeId) || link.getDestinationId().equals(nodeId)) {
                incident.add(link.getId());
            }
        }
        for (String linkId : incident) {
            removeLink(linkId);
        }
        nodes.remove(nodeId);
        adjacency.remove(nodeId);
    }

    public void removeLink(String linkId) {
        NetworkLink link = links.remove(linkId);
        if (link == null) {
            throw new IllegalArgumentException("Unknown link ID: " + linkId);
        }
        List<NetworkLink> fromAdj = adjacency.get(link.getSourceId());
        if (fromAdj != null) {
            fromAdj.remove(link);
        }
        List<NetworkLink> toAdj = adjacency.get(link.getDestinationId());
        if (toAdj != null) {
            toAdj.remove(link);
        }
    }

    public void setLinkActive(String linkId, boolean active) {
        requireLink(linkId).setActive(active);
    }

    public Optional<NetworkNode> findNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public NetworkNode getNode(String nodeId) {
        return requireNode(nodeId);
    }

    public Optional<NetworkLink> findLink(String linkId) {
        return Optional.ofNullable(links.get(linkId));
    }

    public NetworkLink getLink(String linkId) {
        return requireLink(linkId);
    }

    public Map<String, NetworkNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public Map<String, NetworkLink> getLinks() {
        return Collections.unmodifiableMap(links);
    }

    /**
     * Outgoing adjacency for {@code nodeId}, including inactive links.
     */
    public List<NetworkLink> getOutgoingLinks(String nodeId) {
        return List.copyOf(adjacency.get(requireNode(nodeId).getId()));
    }

    /**
     * Outgoing adjacency limited to currently active links.
     */
    public List<NetworkLink> getActiveOutgoingLinks(String nodeId) {
        List<NetworkLink> active = new ArrayList<>();
        for (NetworkLink link : getOutgoingLinks(nodeId)) {
            if (link.isActive()) {
                active.add(link);
            }
        }
        return List.copyOf(active);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int linkCount() {
        return links.size();
    }

    /**
     * Returns the number of links currently in the inactive/failed state.
     */
    public int countInactiveLinks() {
        int count = 0;
        for (NetworkLink link : links.values()) {
            if (!link.isActive()) {
                count++;
            }
        }
        return count;
    }

    public boolean containsNode(String nodeId) {
        return nodes.containsKey(nodeId);
    }

    public boolean containsLink(String linkId) {
        return links.containsKey(linkId);
    }

    private NetworkNode requireNode(String nodeId) {
        NetworkNode node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node ID: " + nodeId);
        }
        return node;
    }

    private NetworkLink requireLink(String linkId) {
        NetworkLink link = links.get(linkId);
        if (link == null) {
            throw new IllegalArgumentException("Unknown link ID: " + linkId);
        }
        return link;
    }

    private boolean hasOverlappingEdge(NetworkLink candidate) {
        for (NetworkLink existing : links.values()) {
            if (edgesOverlap(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Undirected links occupy both directions. A directed link overlaps another
     * directed link only in the same direction, or any undirected link on the pair.
     */
    private static boolean edgesOverlap(NetworkLink a, NetworkLink b) {
        boolean samePair = sameUndirectedPair(a, b);
        if (!samePair) {
            return false;
        }
        if (!a.isDirected() || !b.isDirected()) {
            return true;
        }
        return a.getSourceId().equals(b.getSourceId())
                && a.getDestinationId().equals(b.getDestinationId());
    }

    private static boolean sameUndirectedPair(NetworkLink a, NetworkLink b) {
        return (a.getSourceId().equals(b.getSourceId()) && a.getDestinationId().equals(b.getDestinationId()))
                || (a.getSourceId().equals(b.getDestinationId()) && a.getDestinationId().equals(b.getSourceId()));
    }
}

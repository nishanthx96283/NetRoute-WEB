package com.netroute.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An immutable path through the network: ordered nodes, the links between them,
 * and the sum of those links' routing costs.
 */
public final class Route {

    private final List<NetworkNode> nodes;
    private final List<NetworkLink> links;
    private final double totalCost;

    public Route(List<NetworkNode> nodes, List<NetworkLink> links, double totalCost) {
        Objects.requireNonNull(nodes, "nodes is required");
        Objects.requireNonNull(links, "links is required");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("A route must contain at least one node");
        }
        if (nodes.size() != links.size() + 1) {
            throw new IllegalArgumentException(
                    "Route must have one more node than links: nodes="
                            + nodes.size() + ", links=" + links.size());
        }
        if (Double.isNaN(totalCost) || totalCost < 0.0) {
            throw new IllegalArgumentException("Total routing cost must be non-negative: " + totalCost);
        }
        this.nodes = List.copyOf(nodes);
        this.links = List.copyOf(links);
        this.totalCost = totalCost;
    }

    /**
     * Zero-cost route that stays at a single node.
     */
    public static Route trivial(NetworkNode node) {
        Objects.requireNonNull(node, "node is required");
        return new Route(List.of(node), List.of(), 0.0);
    }

    public List<NetworkNode> getNodes() {
        return nodes;
    }

    public List<NetworkLink> getLinks() {
        return links;
    }

    public List<String> getNodeIds() {
        List<String> ids = new ArrayList<>(nodes.size());
        for (NetworkNode node : nodes) {
            ids.add(node.getId());
        }
        return List.copyOf(ids);
    }

    public List<String> getLinkIds() {
        List<String> ids = new ArrayList<>(links.size());
        for (NetworkLink link : links) {
            ids.add(link.getId());
        }
        return List.copyOf(ids);
    }

    public double getTotalCost() {
        return totalCost;
    }

    public NetworkNode getSource() {
        return nodes.getFirst();
    }

    public NetworkNode getDestination() {
        return nodes.getLast();
    }

    @Override
    public String toString() {
        return "Route(" + String.join(" -> ", getNodeIds()) + ", cost=" + totalCost + ")";
    }
}

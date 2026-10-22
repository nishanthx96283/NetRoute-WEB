package com.netroute.graph;

import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.model.Router;
import com.netroute.model.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Mutation and factory API for {@link Network}.
 * Callers that need the raw graph (routing, simulation) should use {@link #getNetwork()}.
 */
public final class GraphManager {

    private final Network network;

    public GraphManager() {
        this(new Network());
    }

    public GraphManager(Network network) {
        this.network = network == null ? new Network() : network;
    }

    public Network getNetwork() {
        return network;
    }

    public Computer addComputer(String id, String name, double x, double y) {
        Computer node = new Computer(id, name, x, y);
        network.addNode(node);
        return node;
    }

    public Router addRouter(String id, String name, double x, double y) {
        Router node = new Router(id, name, x, y);
        network.addNode(node);
        return node;
    }

    public Server addServer(String id, String name, double x, double y) {
        Server node = new Server(id, name, x, y);
        network.addNode(node);
        return node;
    }

    public NetworkNode addNode(NetworkNode node) {
        return network.addNode(node);
    }

    /**
     * Adds an undirected (bidirectional) link, the default for this simulator.
     */
    public NetworkLink addBidirectionalLink(
            String id,
            String sourceId,
            String destinationId,
            double cost,
            double delay,
            int capacity) {
        return network.addLink(new NetworkLink(id, sourceId, destinationId, cost, delay, capacity, false));
    }

    /**
     * Adds a one-way link. Supported by the model; sample networks use bidirectional links.
     */
    public NetworkLink addDirectedLink(
            String id,
            String sourceId,
            String destinationId,
            double cost,
            double delay,
            int capacity) {
        return network.addLink(new NetworkLink(id, sourceId, destinationId, cost, delay, capacity, true));
    }

    public void removeNode(String nodeId) {
        network.removeNode(nodeId);
    }

    public void removeLink(String linkId) {
        network.removeLink(linkId);
    }

    /**
     * Returns the IDs of all links directly connected to the specified node.
     */
    public List<String> getIncidentLinkIds(String nodeId) {
        if (nodeId == null || !network.containsNode(nodeId)) {
            return List.of();
        }
        List<String> incident = new ArrayList<>();
        for (NetworkLink link : network.getLinks().values()) {
            if (link.getSourceId().equals(nodeId) || link.getDestinationId().equals(nodeId)) {
                incident.add(link.getId());
            }
        }
        return incident;
    }

    public void activateLink(String linkId) {
        network.setLinkActive(linkId, true);
    }

    public void deactivateLink(String linkId) {
        network.setLinkActive(linkId, false);
    }

    // ==========================================
    // FEATURE 1: RANDOMIZE LINK COSTS
    // ==========================================

    /**
     * Randomizes costs for all existing links to integer values from 1 to 10.
     * Preserves link active/inactive status, simulated delay, and capacity.
     */
    public void randomizeLinkCosts() {
        randomizeLinkCosts(1, 10, new Random());
    }

    /**
     * Randomizes costs for all existing links within [minCost, maxCost] inclusive.
     */
    public void randomizeLinkCosts(int minCost, int maxCost) {
        randomizeLinkCosts(minCost, maxCost, new Random());
    }

    /**
     * Randomizes costs for all existing links within [minCost, maxCost] inclusive
     * using the provided random generator. Useful for deterministic testing.
     */
    public void randomizeLinkCosts(int minCost, int maxCost, Random random) {
        if (minCost < 1) {
            throw new IllegalArgumentException("minCost must be at least 1: " + minCost);
        }
        if (maxCost < minCost) {
            throw new IllegalArgumentException("maxCost (" + maxCost + ") cannot be less than minCost (" + minCost + ")");
        }
        Objects.requireNonNull(random, "random is required");

        for (NetworkLink link : network.getLinks().values()) {
            int newCost = minCost + random.nextInt(maxCost - minCost + 1);
            link.setCost(newCost);
        }
    }

    // ==========================================
    // FEATURE 2: DYNAMIC NETWORK MANAGEMENT
    // ==========================================

    /**
     * Generates a sequential unique node ID based on existing nodes in the network
     * (e.g. PC4, R3, S2). Never overwrites existing node IDs.
     */
    public String generateNextNodeId(NodeType type) {
        Objects.requireNonNull(type, "type is required");
        String prefix = switch (type) {
            case COMPUTER -> "PC";
            case ROUTER -> "R";
            case SERVER -> "S";
        };
        int index = 1;
        while (network.containsNode(prefix + index)) {
            index++;
        }
        return prefix + index;
    }

    /**
     * Generates a sequential unique link ID based on existing links in the network (e.g. L7).
     */
    public String generateNextLinkId() {
        int index = 1;
        while (network.containsLink("L" + index)) {
            index++;
        }
        return "L" + index;
    }

    /**
     * Dynamically creates a new unconnected node at an automatically calculated
     * non-overlapping position within the topology canvas.
     */
    public NetworkNode addDynamicNode(NodeType type) {
        double[] pos = findAvailableNodePosition();
        return addDynamicNode(type, null, pos[0], pos[1]);
    }

    /**
     * Dynamically creates a new unconnected node of the given type with a specified position.
     */
    public NetworkNode addDynamicNode(NodeType type, String name, double x, double y) {
        String id = generateNextNodeId(type);
        String finalName = (name != null && !name.isBlank()) ? name.trim() : defaultNodeName(type, id);
        return switch (type) {
            case COMPUTER -> addComputer(id, finalName, x, y);
            case ROUTER -> addRouter(id, finalName, x, y);
            case SERVER -> addServer(id, finalName, x, y);
        };
    }

    /**
     * Checks if a link already exists between two nodes in either direction.
     */
    public boolean hasLinkBetween(String nodeA, String nodeB) {
        for (NetworkLink link : network.getLinks().values()) {
            if (link.connects(nodeA, nodeB)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Connects two existing nodes with a bidirectional link using default delay (2.0) and capacity (4).
     */
    public NetworkLink connectNodes(String sourceId, String destinationId, double cost) {
        return connectNodes(sourceId, destinationId, cost, 2.0, 4);
    }

    /**
     * Connects two existing nodes with a bidirectional link.
     * Validates that endpoints exist, are distinct (no self-loops), no duplicate link exists
     * in either direction, and that the cost is a finite positive number.
     */
    public NetworkLink connectNodes(
            String sourceId,
            String destinationId,
            double cost,
            double delay,
            int capacity) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("Source node ID must be a non-blank string");
        }
        if (destinationId == null || destinationId.isBlank()) {
            throw new IllegalArgumentException("Destination node ID must be a non-blank string");
        }

        String src = sourceId.trim();
        String dst = destinationId.trim();

        if (src.equals(dst)) {
            throw new IllegalArgumentException("Self-loops are not allowed: " + src);
        }
        if (!network.containsNode(src)) {
            throw new IllegalArgumentException("Source node does not exist: " + src);
        }
        if (!network.containsNode(dst)) {
            throw new IllegalArgumentException("Destination node does not exist: " + dst);
        }
        if (hasLinkBetween(src, dst)) {
            throw new IllegalArgumentException("A link already exists between " + src + " and " + dst);
        }
        if (Double.isNaN(cost) || Double.isInfinite(cost) || cost <= 0.0) {
            throw new IllegalArgumentException("Link cost must be a finite positive number: " + cost);
        }

        String linkId = generateNextLinkId();
        return addBidirectionalLink(linkId, src, dst, cost, delay, capacity);
    }

    /**
     * Finds a candidate position within the visible topology canvas that does not
     * overlap existing nodes.
     */
    public double[] findAvailableNodePosition() {
        for (int y = 90; y <= 400; y += 70) {
            for (int x = 180; x <= 650; x += 100) {
                boolean overlaps = false;
                for (NetworkNode existing : network.getNodes().values()) {
                    if (Math.hypot(x - existing.getX(), y - existing.getY()) < 85.0) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps) {
                    return new double[]{x, y};
                }
            }
        }
        int count = network.nodeCount();
        return new double[]{120.0 + (count * 45) % 520, 100.0 + (count * 35) % 280};
    }

    private static String defaultNodeName(NodeType type, String id) {
        return switch (type) {
            case COMPUTER -> "Lab " + id;
            case ROUTER -> "Router " + id;
            case SERVER -> "Server " + id;
        };
    }

    /**
     * Builds a small connected campus-style topology: three computers, two routers, one server.
     */
    public static GraphManager createSampleNetwork() {
        GraphManager graph = new GraphManager();
        graph.addComputer("PC1", "Lab PC 1", 80, 80);
        graph.addComputer("PC2", "Lab PC 2", 80, 220);
        graph.addComputer("PC3", "Office PC", 80, 360);
        graph.addRouter("R1", "Core Router", 280, 150);
        graph.addRouter("R2", "Edge Router", 480, 220);
        graph.addServer("S1", "File Server", 680, 220);

        graph.addBidirectionalLink("L1", "PC1", "R1", 2.0, 5.0, 4);
        graph.addBidirectionalLink("L2", "PC2", "R1", 2.0, 5.0, 4);
        graph.addBidirectionalLink("L3", "PC3", "R2", 3.0, 8.0, 3);
        graph.addBidirectionalLink("L4", "R1", "R2", 1.0, 2.0, 8);
        graph.addBidirectionalLink("L5", "R2", "S1", 2.0, 4.0, 6);
        graph.addBidirectionalLink("L6", "R1", "S1", 5.0, 10.0, 4);
        return graph;
    }
}

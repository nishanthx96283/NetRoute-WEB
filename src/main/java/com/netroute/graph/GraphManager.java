package com.netroute.graph;

import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.Router;
import com.netroute.model.Server;

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

    public void activateLink(String linkId) {
        network.setLinkActive(linkId, true);
    }

    public void deactivateLink(String linkId) {
        network.setLinkActive(linkId, false);
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

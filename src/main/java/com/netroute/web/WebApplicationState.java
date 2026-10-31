package com.netroute.web;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.routing.DijkstraRouter;
import com.netroute.service.TopologyStorageService;
import com.netroute.simulation.SimulationEngine;
import com.netroute.statistics.NetworkStatistics;
import com.netroute.web.dto.LinkDto;
import com.netroute.web.dto.NetworkDto;
import com.netroute.web.dto.NodeDto;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe holder and coordinator of the active NetRoute simulation state.
 * Bridges web request handlers with the underlying Java networking engine.
 */
public class WebApplicationState {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_EVENT_LOGS = 100;

    private final DijkstraRouter router = new DijkstraRouter();
    private final TopologyStorageService storageService;

    private GraphManager graphManager;
    private Network network;
    private SimulationEngine engine;
    private NetworkStatistics statistics;

    private Integer activeTopologyId = null;
    private String activeTopologyName = "Sample Campus Network";
    private String activeTopologyDescription = "";
    private boolean topologyModified = false;
    private boolean currentRunSaved = false;
    private Integer lastSavedRunId = null;

    private final Deque<String> eventLogs = new ArrayDeque<>();

    public WebApplicationState() {
        this(new TopologyStorageService());
    }

    public WebApplicationState(TopologyStorageService storageService) {
        this.storageService = Objects.requireNonNull(storageService, "storageService is required");
        resetToSampleNetwork();
    }

    public synchronized void resetToSampleNetwork() {
        this.graphManager = GraphManager.createSampleNetwork();
        this.network = graphManager.getNetwork();
        this.engine = new SimulationEngine(this.network, this.router);
        this.statistics = new NetworkStatistics(this.engine);
        this.activeTopologyId = null;
        this.activeTopologyName = "Sample Campus Network";
        this.activeTopologyDescription = "Connected campus topology with 3 computers, 2 routers, and 1 server.";
        this.topologyModified = false;
        this.currentRunSaved = false;
        this.lastSavedRunId = null;
        logEvent("Loaded default Sample Campus Network (6 nodes, 6 links).");
    }

    public synchronized void newProject() {
        this.graphManager = new GraphManager();
        this.network = graphManager.getNetwork();
        this.engine = new SimulationEngine(this.network, this.router);
        this.statistics = new NetworkStatistics(this.engine);
        this.activeTopologyId = null;
        this.activeTopologyName = "Untitled Project";
        this.activeTopologyDescription = "";
        this.topologyModified = false;
        this.currentRunSaved = false;
        this.lastSavedRunId = null;
        logEvent("Created blank network project.");
    }

    public synchronized void loadTopology(int id, String name, String description, Network loadedNetwork) {
        Objects.requireNonNull(loadedNetwork, "loadedNetwork is required");
        this.network = loadedNetwork;
        this.graphManager = new GraphManager(this.network);
        this.engine = new SimulationEngine(this.network, this.router);
        this.statistics = new NetworkStatistics(this.engine);
        this.activeTopologyId = id;
        this.activeTopologyName = name != null ? name : "Topology #" + id;
        this.activeTopologyDescription = description != null ? description : "";
        this.topologyModified = false;
        this.currentRunSaved = false;
        this.lastSavedRunId = null;
        logEvent("Loaded topology '" + this.activeTopologyName + "' from database (ID: " + id + ").");
    }

    public synchronized void resetSimulationEngine() {
        this.engine = new SimulationEngine(this.network, this.router);
        this.statistics = new NetworkStatistics(this.engine);
        this.currentRunSaved = false;
        logEvent("Simulation engine reset for current network.");
    }

    public synchronized NetworkDto getNetworkSnapshot() {
        List<NodeDto> nodes = network.getNodes().values().stream().map(NodeDto::from).toList();
        List<LinkDto> links = network.getLinks().values().stream().map(LinkDto::from).toList();
        return new NetworkDto(
                nodes,
                links,
                activeTopologyId,
                activeTopologyName,
                activeTopologyDescription,
                topologyModified,
                currentRunSaved,
                lastSavedRunId
        );
    }

    public synchronized NetworkNode addNode(NodeType type, String name, Double x, Double y) {
        double posX = (x != null) ? x : 100.0;
        double posY = (y != null) ? y : 100.0;
        if (x == null || y == null) {
            double[] candidate = graphManager.findAvailableNodePosition();
            posX = candidate[0];
            posY = candidate[1];
        }
        NetworkNode node = graphManager.addDynamicNode(type, name, posX, posY);
        this.topologyModified = true;
        this.currentRunSaved = false;
        logEvent("Added " + type + " node: " + node.getId() + " (" + node.getName() + ").");
        return node;
    }

    public synchronized void deleteNode(String nodeId) {
        if (nodeId == null || !network.containsNode(nodeId)) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
        NetworkNode node = network.getNode(nodeId);
        engine.removeNode(nodeId);
        graphManager.removeNode(nodeId);
        this.topologyModified = true;
        this.currentRunSaved = false;
        logEvent("Deleted node: " + nodeId + " (" + node.getName() + ") and connected links.");
    }

    public synchronized NetworkLink connectNodes(
            String sourceId,
            String destinationId,
            double cost,
            Double delay,
            Integer capacity,
            Boolean directed) {
        double d = (delay != null && delay >= 0.0) ? delay : 2.0;
        int cap = (capacity != null && capacity >= 1) ? capacity : 4;
        boolean dir = (directed != null) && directed;

        String src = sourceId != null ? sourceId.trim() : "";
        String dst = destinationId != null ? destinationId.trim() : "";

        if (src.isEmpty() || dst.isEmpty()) {
            throw new IllegalArgumentException("Source and destination must be non-empty");
        }
        if (src.equals(dst)) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
        if (!network.containsNode(src) || !network.containsNode(dst)) {
            throw new IllegalArgumentException("Endpoints do not exist: " + src + " -> " + dst);
        }
        if (graphManager.hasLinkBetween(src, dst)) {
            throw new IllegalArgumentException("A link already exists between " + src + " and " + dst);
        }

        String linkId = graphManager.generateNextLinkId();
        NetworkLink link = dir
                ? graphManager.addDirectedLink(linkId, src, dst, cost, d, cap)
                : graphManager.addBidirectionalLink(linkId, src, dst, cost, d, cap);

        this.topologyModified = true;
        this.currentRunSaved = false;
        logEvent("Connected " + src + " <-> " + dst + " via link " + link.getId() + " (cost=" + cost + ").");
        return link;
    }

    public synchronized void deleteLink(String linkId) {
        if (linkId == null || !network.containsLink(linkId)) {
            throw new IllegalArgumentException("Link not found: " + linkId);
        }
        engine.removeLink(linkId);
        graphManager.removeLink(linkId);
        this.topologyModified = true;
        this.currentRunSaved = false;
        logEvent("Deleted link: " + linkId);
    }

    public synchronized void updateNodePosition(String nodeId, double x, double y) {
        if (!network.containsNode(nodeId)) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
        network.getNode(nodeId).setPosition(x, y);
        this.topologyModified = true;
    }

    public synchronized void randomizeLinkCosts() {
        graphManager.randomizeLinkCosts();
        this.topologyModified = true;
        this.currentRunSaved = false;
        logEvent("Randomized link costs (1-10) across all active links.");
    }

    public synchronized void failLink(String linkId) {
        if (!network.containsLink(linkId)) {
            throw new IllegalArgumentException("Link not found: " + linkId);
        }
        NetworkLink link = network.getLink(linkId);
        if (!link.isActive()) {
            return;
        }
        engine.failLink(linkId);
        logEvent("Link " + linkId + " (" + link.getSourceId() + " <-> " + link.getDestinationId() + ") marked FAILED.");
    }

    public synchronized void restoreLink(String linkId) {
        if (!network.containsLink(linkId)) {
            throw new IllegalArgumentException("Link not found: " + linkId);
        }
        NetworkLink link = network.getLink(linkId);
        if (link.isActive()) {
            return;
        }
        engine.restoreLink(linkId);
        logEvent("Link " + linkId + " RESTORED to active operational status.");
    }

    public synchronized void logEvent(String message) {
        String entry = "[" + LocalTime.now().format(TIME_FORMAT) + "] " + message;
        if (eventLogs.size() >= MAX_EVENT_LOGS) {
            eventLogs.pollFirst();
        }
        eventLogs.addLast(entry);
    }

    public synchronized List<String> getRecentEvents() {
        return new ArrayList<>(eventLogs);
    }

    public synchronized GraphManager getGraphManager() {
        return graphManager;
    }

    public synchronized Network getNetwork() {
        return network;
    }

    public synchronized DijkstraRouter getRouter() {
        return router;
    }

    public synchronized SimulationEngine getEngine() {
        return engine;
    }

    public synchronized TopologyStorageService getStorageService() {
        return storageService;
    }

    public synchronized NetworkStatistics getStatistics() {
        return statistics;
    }

    public synchronized Integer getActiveTopologyId() {
        return activeTopologyId;
    }

    public synchronized void setActiveTopologyId(Integer activeTopologyId) {
        this.activeTopologyId = activeTopologyId;
    }

    public synchronized String getActiveTopologyName() {
        return activeTopologyName;
    }

    public synchronized void setActiveTopologyName(String activeTopologyName) {
        this.activeTopologyName = activeTopologyName;
    }

    public synchronized String getActiveTopologyDescription() {
        return activeTopologyDescription;
    }

    public synchronized void setActiveTopologyDescription(String activeTopologyDescription) {
        this.activeTopologyDescription = activeTopologyDescription;
    }

    public synchronized boolean isTopologyModified() {
        return topologyModified;
    }

    public synchronized void setTopologyModified(boolean topologyModified) {
        this.topologyModified = topologyModified;
    }

    public synchronized boolean isCurrentRunSaved() {
        return currentRunSaved;
    }

    public synchronized void setCurrentRunSaved(boolean currentRunSaved) {
        this.currentRunSaved = currentRunSaved;
    }

    public synchronized Integer getLastSavedRunId() {
        return lastSavedRunId;
    }

    public synchronized void setLastSavedRunId(Integer lastSavedRunId) {
        this.lastSavedRunId = lastSavedRunId;
    }
}

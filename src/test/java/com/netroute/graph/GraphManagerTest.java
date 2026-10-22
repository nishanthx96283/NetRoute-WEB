package com.netroute.graph;

import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.model.Packet;
import com.netroute.model.PacketStatus;
import com.netroute.model.Route;
import com.netroute.model.Router;
import com.netroute.model.Server;
import com.netroute.routing.DijkstraRouter;
import com.netroute.simulation.SimulationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphManagerTest {

    private GraphManager graph;
    private Network network;

    @BeforeEach
    void setUp() {
        graph = new GraphManager();
        network = graph.getNetwork();
    }

    @Test
    void createsNodesWithUniqueIdsNamesTypesAndPositions() {
        Computer pc = graph.addComputer("PC1", "Lab PC", 10.0, 20.0);
        Router router = graph.addRouter("R1", "Core", 30.0, 40.0);
        Server server = graph.addServer("S1", "Files", 50.0, 60.0);

        assertEquals("PC1", pc.getId());
        assertEquals("Lab PC", pc.getName());
        assertEquals(NodeType.COMPUTER, pc.getType());
        assertEquals(10.0, pc.getX());
        assertEquals(20.0, pc.getY());
        assertEquals(NodeType.ROUTER, router.getType());
        assertEquals(NodeType.SERVER, server.getType());
        assertEquals(3, network.nodeCount());
        assertTrue(pc instanceof NetworkNode);
    }

    @Test
    void rejectsDuplicateNodeIds() {
        graph.addComputer("PC1", "First", 0, 0);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> graph.addRouter("PC1", "Same ID", 1, 1));
        assertTrue(ex.getMessage().contains("Duplicate node ID"));
        assertEquals(1, network.nodeCount());
    }

    @Test
    void createsBidirectionalLinkAndAdjacency() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);

        NetworkLink link = graph.addBidirectionalLink("AB", "A", "B", 4.5, 12.0, 2);

        assertEquals("AB", link.getId());
        assertEquals("A", link.getSourceId());
        assertEquals("B", link.getDestinationId());
        assertEquals(4.5, link.getCost());
        assertEquals(12.0, link.getDelay());
        assertEquals(2, link.getCapacity());
        assertTrue(link.isActive());
        assertFalse(link.isDirected());

        List<NetworkLink> fromA = network.getOutgoingLinks("A");
        List<NetworkLink> fromB = network.getOutgoingLinks("B");
        assertEquals(1, fromA.size());
        assertEquals(1, fromB.size());
        assertEquals("B", fromA.getFirst().otherEndpoint("A"));
        assertEquals("A", fromB.getFirst().otherEndpoint("B"));
    }

    @Test
    void directedLinkAppearsOnlyInSourceAdjacency() {
        graph.addRouter("R1", "R1", 0, 0);
        graph.addRouter("R2", "R2", 1, 0);

        graph.addDirectedLink("R1R2", "R1", "R2", 1.0, 1.0, 1);

        assertEquals(1, network.getOutgoingLinks("R1").size());
        assertEquals(0, network.getOutgoingLinks("R2").size());
        assertEquals("R2", network.getOutgoingLinks("R1").getFirst().otherEndpoint("R1"));
    }

    @Test
    void rejectsUnknownLinkEndpoints() {
        graph.addComputer("A", "A", 0, 0);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> graph.addBidirectionalLink("AX", "A", "MISSING", 1, 1, 1));
        assertTrue(ex.getMessage().contains("Unknown node ID"));
        assertEquals(0, network.linkCount());
    }

    @Test
    void rejectsSelfLoopAndNegativeCost() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addBidirectionalLink("AA", "A", "A", 1, 1, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addBidirectionalLink("AB", "A", "B", -0.1, 1, 1));
        assertEquals(0, network.linkCount());
    }

    @Test
    void rejectsDuplicateLinkIdsAndOverlappingUndirectedEdges() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1, 1, 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addBidirectionalLink("AB", "A", "B", 2, 1, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addBidirectionalLink("BA", "B", "A", 2, 1, 1));
        assertEquals(1, network.linkCount());
    }

    @Test
    void deactivateHidesLinkFromActiveAdjacencyButKeepsItInGraph() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1, 1, 1);

        graph.deactivateLink("AB");
        assertFalse(network.getLink("AB").isActive());
        assertEquals(0, network.getActiveOutgoingLinks("A").size());
        assertEquals(1, network.getOutgoingLinks("A").size());

        graph.activateLink("AB");
        assertEquals(1, network.getActiveOutgoingLinks("A").size());
    }

    @Test
    void removingNodeRemovesIncidentLinksFromAdjacency() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1, 1, 1);
        graph.addBidirectionalLink("BC", "B", "C", 1, 1, 1);

        graph.removeNode("B");

        assertFalse(network.containsNode("B"));
        assertFalse(network.containsLink("AB"));
        assertFalse(network.containsLink("BC"));
        assertTrue(network.getOutgoingLinks("A").isEmpty());
        assertTrue(network.getOutgoingLinks("C").isEmpty());
        assertEquals(2, network.nodeCount());
    }

    @Test
    void sampleNetworkIsConnectedWithMixedNodeTypes() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();

        assertEquals(6, net.nodeCount());
        assertEquals(6, net.linkCount());
        assertEquals(NodeType.COMPUTER, net.getNode("PC1").getType());
        assertEquals(NodeType.ROUTER, net.getNode("R1").getType());
        assertEquals(NodeType.SERVER, net.getNode("S1").getType());

        assertEquals(4, net.getOutgoingLinks("R1").size());
        assertTrue(reachesAll(net, "PC1"));
        assertTrue(reachesAll(net, "S1"));
    }

    // ==========================================
    // FEATURE 1: RANDOMIZE LINK COSTS TESTS
    // ==========================================

    @Test
    void randomizeLinkCostsAssignsIntegerCostsBetweenOneAndTen() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();

        sample.randomizeLinkCosts();

        for (NetworkLink link : net.getLinks().values()) {
            double cost = link.getCost();
            assertTrue(cost >= 1.0 && cost <= 10.0, "Cost should be within [1, 10]: " + cost);
            assertEquals(cost, Math.floor(cost), "Cost must be an integer: " + cost);
        }
    }

    @Test
    void randomizeLinkCostsPreservesActiveStatusDelayAndCapacity() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();

        // Deactivate one link before randomization
        sample.deactivateLink("L4");
        assertFalse(net.getLink("L4").isActive());

        double delayBefore = net.getLink("L1").getDelay();
        int capBefore = net.getLink("L1").getCapacity();

        sample.randomizeLinkCosts();

        // L4 must remain inactive, others active
        assertFalse(net.getLink("L4").isActive(), "Deactivated link must remain inactive");
        assertTrue(net.getLink("L1").isActive(), "Active link must remain active");

        // Delay and capacity must not change
        assertEquals(delayBefore, net.getLink("L1").getDelay());
        assertEquals(capBefore, net.getLink("L1").getCapacity());
    }

    @Test
    void randomizeLinkCostsRejectsInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> graph.randomizeLinkCosts(0, 10));
        assertThrows(IllegalArgumentException.class, () -> graph.randomizeLinkCosts(-2, 5));
        assertThrows(IllegalArgumentException.class, () -> graph.randomizeLinkCosts(10, 5));
    }

    @Test
    void dijkstraRecalculatesRouteAfterLinkCostRandomization() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();
        DijkstraRouter router = new DijkstraRouter();

        // Initial shortest path PC1 -> S1: PC1 -> R1 -> R2 -> S1 (Cost: 2+1+2 = 5)
        Route initialRoute = router.findRoute(net, "PC1", "S1").orElseThrow();
        assertEquals(List.of("PC1", "R1", "R2", "S1"), initialRoute.getNodeIds());

        // Make L6 (direct R1 -> S1) cheaper than (R1 -> R2 -> S1)
        net.getLink("L6").setCost(1.0);
        net.getLink("L4").setCost(10.0);
        net.getLink("L5").setCost(10.0);

        Route newRoute = router.findRoute(net, "PC1", "S1").orElseThrow();
        assertEquals(List.of("PC1", "R1", "S1"), newRoute.getNodeIds(),
                "Dijkstra should find the new cheaper path via L6");
        assertEquals(3.0, newRoute.getTotalCost());
    }

    // ==========================================
    // FEATURE 2: DYNAMIC NETWORK MANAGEMENT TESTS
    // ==========================================

    @Test
    void generateNextNodeIdProducesUniqueSequentialIds() {
        GraphManager sample = GraphManager.createSampleNetwork();

        assertEquals("PC4", sample.generateNextNodeId(NodeType.COMPUTER));
        assertEquals("R3", sample.generateNextNodeId(NodeType.ROUTER));
        assertEquals("S2", sample.generateNextNodeId(NodeType.SERVER));

        // Add PC4 and check that next is PC5
        sample.addDynamicNode(NodeType.COMPUTER);
        assertEquals("PC5", sample.generateNextNodeId(NodeType.COMPUTER));
    }

    @Test
    void generateNextLinkIdProducesUniqueSequentialIds() {
        GraphManager sample = GraphManager.createSampleNetwork();
        assertEquals("L7", sample.generateNextLinkId());

        sample.addBidirectionalLink("L7", "PC1", "PC2", 3.0, 1.0, 1);
        assertEquals("L8", sample.generateNextLinkId());
    }

    @Test
    void addDynamicNodeCreatesUnconnectedNode() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();

        NetworkNode newNode = sample.addDynamicNode(NodeType.SERVER);

        assertEquals("S2", newNode.getId());
        assertEquals(NodeType.SERVER, newNode.getType());
        assertTrue(net.containsNode("S2"));
        // Must be completely unconnected initially
        assertTrue(net.getOutgoingLinks("S2").isEmpty(), "New node must initially have no links");
        assertEquals(7, net.nodeCount());
    }

    @Test
    void connectNodesCreatesBidirectionalLink() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();

        NetworkNode newPc = sample.addDynamicNode(NodeType.COMPUTER); // PC4
        NetworkLink newLink = sample.connectNodes("PC4", "R1", 3.5);

        assertNotNull(newLink);
        assertEquals("L7", newLink.getId());
        assertEquals(3.5, newLink.getCost());
        assertTrue(newLink.isActive());
        assertFalse(newLink.isDirected());

        assertTrue(sample.hasLinkBetween("PC4", "R1"));
        assertTrue(sample.hasLinkBetween("R1", "PC4"));
        assertEquals(7, net.linkCount());
    }

    @Test
    void connectNodesRejectsSelfLoopAndDuplicateLinkInEitherDirection() {
        GraphManager sample = GraphManager.createSampleNetwork();

        // 1. Self-loop
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("PC1", "PC1", 2.0));

        // 2. Duplicate link in existing direction (PC1 -> R1 exists as L1)
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("PC1", "R1", 2.0));

        // 3. Duplicate link in reverse direction (R1 -> PC1)
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("R1", "PC1", 2.0));

        // 4. Non-existent endpoints
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("UNKNOWN", "PC1", 2.0));
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("PC1", "UNKNOWN", 2.0));
    }

    @Test
    void connectNodesRejectsInvalidCosts() {
        GraphManager sample = GraphManager.createSampleNetwork();
        sample.addDynamicNode(NodeType.COMPUTER); // PC4

        // Zero cost
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("PC4", "R1", 0.0));
        // Negative cost
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("PC4", "R1", -1.5));
        // NaN cost
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("PC4", "R1", Double.NaN));
        // Infinite cost
        assertThrows(IllegalArgumentException.class, () -> sample.connectNodes("PC4", "R1", Double.POSITIVE_INFINITY));
    }

    @Test
    void dijkstraRoutesThroughNewlyAddedNodesAndLinks() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();
        DijkstraRouter router = new DijkstraRouter();

        // Add S2 and connect R2 <-> S2 with cost 1.0
        sample.addDynamicNode(NodeType.SERVER); // S2
        sample.connectNodes("R2", "S2", 1.0);

        // Before connecting, PC1 could not reach S2; now it can
        Route route = router.findRoute(net, "PC1", "S2").orElseThrow();
        assertEquals(List.of("PC1", "R1", "R2", "S2"), route.getNodeIds());
        // Cost: PC1-R1 (2.0) + R1-R2 (1.0) + R2-S2 (1.0) = 4.0
        assertEquals(4.0, route.getTotalCost());
    }

    @Test
    void simulationDeliversPacketAcrossDynamicallyAddedLink() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();
        SimulationEngine engine = new SimulationEngine(net);

        // Add PC4 and connect to R1
        sample.addDynamicNode(NodeType.COMPUTER); // PC4
        NetworkLink link = sample.connectNodes("PC4", "R1", 1.0);
        engine.queueFor(link.getId());

        // Send packet from PC4 to S1
        Packet packet = engine.submitPacket("PC4", "S1", 1);
        engine.run();

        assertEquals(PacketStatus.DELIVERED, packet.getStatus());
        assertEquals("PC4", packet.getSourceId());
        assertEquals("S1", packet.getDestinationId());
        assertTrue(packet.getVisitedNodeIds().contains("PC4"));
        assertTrue(packet.getVisitedNodeIds().contains("S1"));
    }

    @Test
    void linkFailureAndRestoreWorksOnDynamicallyAddedLink() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();
        SimulationEngine engine = new SimulationEngine(net);

        sample.addDynamicNode(NodeType.COMPUTER); // PC4
        NetworkLink link = sample.connectNodes("PC4", "R1", 1.0);
        engine.queueFor(link.getId());

        assertTrue(link.isActive());
        engine.failLink(link.getId());
        assertFalse(link.isActive(), "Dynamically added link should be deactivated by failLink");

        engine.restoreLink(link.getId());
        assertTrue(link.isActive(), "Dynamically added link should be reactivated by restoreLink");
    }

    @Test
    void removeLinkRemovesLinkFromNetworkAndAdjacencies() {
        graph.addComputer("A", "Node A", 0, 0);
        graph.addComputer("B", "Node B", 10, 10);
        NetworkLink link = graph.addBidirectionalLink("AB", "A", "B", 3.0, 1.0, 1);

        assertTrue(network.containsLink("AB"));
        assertEquals(1, network.getActiveOutgoingLinks("A").size());
        assertEquals(1, network.getActiveOutgoingLinks("B").size());

        graph.removeLink("AB");

        assertFalse(network.containsLink("AB"));
        assertTrue(network.getActiveOutgoingLinks("A").isEmpty());
        assertTrue(network.getActiveOutgoingLinks("B").isEmpty());

        assertThrows(IllegalArgumentException.class, () -> graph.removeLink("AB"));
    }

    @Test
    void removeNodeRemovesNodeAndAllIncidentLinks() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();

        assertTrue(net.containsNode("PC1"));
        assertTrue(net.containsLink("L1"));

        sample.removeNode("PC1");

        assertFalse(net.containsNode("PC1"));
        assertFalse(net.containsLink("L1"));
        assertTrue(net.getActiveOutgoingLinks("R1").stream().noneMatch(l -> l.getId().equals("L1")));

        assertThrows(IllegalArgumentException.class, () -> sample.removeNode("PC1"));
    }

    @Test
    void getIncidentLinkIdsReturnsAllConnectedLinks() {
        GraphManager sample = GraphManager.createSampleNetwork();
        List<String> r1Links = sample.getIncidentLinkIds("R1");

        assertEquals(4, r1Links.size());
        assertTrue(r1Links.containsAll(List.of("L1", "L2", "L4", "L6")));

        List<String> r2Links = sample.getIncidentLinkIds("R2");
        assertEquals(3, r2Links.size());
        assertTrue(r2Links.containsAll(List.of("L3", "L4", "L5")));

        assertTrue(sample.getIncidentLinkIds("NONEXISTENT").isEmpty());
        assertTrue(sample.getIncidentLinkIds(null).isEmpty());
    }

    @Test
    void dijkstraRecalculatesRouteWhenLinkOrNodeIsDeleted() {
        GraphManager sample = GraphManager.createSampleNetwork();
        Network net = sample.getNetwork();
        DijkstraRouter router = new DijkstraRouter();

        // Initially PC1 -> R1 -> R2 -> S1 via L1, L4, L5 (Cost: 2 + 1 + 2 = 5.0)
        Route directRoute = router.findRoute(net, "PC1", "S1").orElseThrow();
        assertEquals(List.of("PC1", "R1", "R2", "S1"), directRoute.getNodeIds());
        assertEquals(5.0, directRoute.getTotalCost());

        // Delete link L5 (R2 <-> S1)
        sample.removeLink("L5");

        // Route should now reroute via L6 (PC1 -> R1 -> S1, Cost: 2 + 5 = 7.0)
        Route rerouted = router.findRoute(net, "PC1", "S1").orElseThrow();
        assertEquals(List.of("PC1", "R1", "S1"), rerouted.getNodeIds());
        assertEquals(7.0, rerouted.getTotalCost());
        assertFalse(rerouted.getLinkIds().contains("L5"));

        // Delete remaining link L6 (R1 <-> S1) so S1 is disconnected
        sample.removeLink("L6");
        Optional<Route> noRoute = router.findRoute(net, "PC1", "S1");
        assertTrue(noRoute.isEmpty());

        // Delete destination node S1
        sample.removeNode("S1");
        assertThrows(IllegalArgumentException.class, () -> router.findRoute(net, "PC1", "S1"));
    }

    private static boolean reachesAll(Network net, String startId) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        queue.add(startId);
        seen.add(startId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (NetworkLink link : net.getActiveOutgoingLinks(current)) {
                String next = link.otherEndpoint(current);
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen.size() == net.nodeCount();
    }
}

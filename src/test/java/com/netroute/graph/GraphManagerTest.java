package com.netroute.graph;

import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.model.Router;
import com.netroute.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

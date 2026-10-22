package com.netroute.repository;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.model.Route;
import com.netroute.model.Router;
import com.netroute.model.Server;
import com.netroute.routing.DijkstraRouter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologyReconstructionTest {

    @Test
    void topologyDataFidelityIsPreservedThroughReconstruction() {
        // Build original source network
        GraphManager gm = GraphManager.createSampleNetwork();
        Network original = gm.getNetwork();

        // Add an inactive link and an extra server to verify edge cases
        gm.addServer("S2", "Backup Server", 680, 360);
        NetworkLink l7 = gm.addBidirectionalLink("L7", "R2", "S2", 4.5, 6.0, 5);
        l7.setActive(false);

        // Reconstruct network simulating TopologyRepository load logic
        Network reconstructed = new Network();

        // Step 1: Reconstruct nodes
        for (NetworkNode origNode : original.getNodes().values()) {
            NetworkNode node = switch (origNode.getType()) {
                case COMPUTER -> new Computer(origNode.getId(), origNode.getName(), origNode.getX(), origNode.getY());
                case ROUTER -> new Router(origNode.getId(), origNode.getName(), origNode.getX(), origNode.getY());
                case SERVER -> new Server(origNode.getId(), origNode.getName(), origNode.getX(), origNode.getY());
            };
            reconstructed.addNode(node);
        }

        // Step 2: Reconstruct links
        for (NetworkLink origLink : original.getLinks().values()) {
            NetworkLink link = new NetworkLink(
                    origLink.getId(),
                    origLink.getSourceId(),
                    origLink.getDestinationId(),
                    origLink.getCost(),
                    origLink.getDelay(),
                    origLink.getCapacity(),
                    origLink.isDirected()
            );
            link.setActive(origLink.isActive());
            reconstructed.addLink(link);
        }

        // Validate node parity
        assertEquals(original.nodeCount(), reconstructed.nodeCount());
        for (NetworkNode origNode : original.getNodes().values()) {
            Optional<NetworkNode> recNode = reconstructed.findNode(origNode.getId());
            assertTrue(recNode.isPresent());
            assertEquals(origNode.getName(), recNode.get().getName());
            assertEquals(origNode.getType(), recNode.get().getType());
            assertEquals(origNode.getX(), recNode.get().getX(), 0.001);
            assertEquals(origNode.getY(), recNode.get().getY(), 0.001);
        }

        // Validate link parity
        assertEquals(original.linkCount(), reconstructed.linkCount());
        for (NetworkLink origLink : original.getLinks().values()) {
            Optional<NetworkLink> recLink = reconstructed.findLink(origLink.getId());
            assertTrue(recLink.isPresent());
            assertEquals(origLink.getSourceId(), recLink.get().getSourceId());
            assertEquals(origLink.getDestinationId(), recLink.get().getDestinationId());
            assertEquals(origLink.getCost(), recLink.get().getCost(), 0.001);
            assertEquals(origLink.getDelay(), recLink.get().getDelay(), 0.001);
            assertEquals(origLink.getCapacity(), recLink.get().getCapacity());
            assertEquals(origLink.isActive(), recLink.get().isActive());
            assertEquals(origLink.isDirected(), recLink.get().isDirected());
        }

        // Validate routing parity on both networks
        DijkstraRouter router = new DijkstraRouter();
        Optional<Route> origRoute = router.findRoute(original, "PC1", "S1");
        Optional<Route> recRoute = router.findRoute(reconstructed, "PC1", "S1");

        assertTrue(origRoute.isPresent());
        assertTrue(recRoute.isPresent());
        assertEquals(origRoute.get().getTotalCost(), recRoute.get().getTotalCost(), 0.001);
        assertEquals(origRoute.get().getLinks().size(), recRoute.get().getLinks().size());
        assertEquals(origRoute.get().getNodeIds(), recRoute.get().getNodeIds());

        // Inactive link should not be usable for routing
        Optional<Route> inactiveRoute = router.findRoute(reconstructed, "PC1", "S2");
        assertFalse(inactiveRoute.isPresent(), "Inactive link L7 must not be routed across");
    }
}

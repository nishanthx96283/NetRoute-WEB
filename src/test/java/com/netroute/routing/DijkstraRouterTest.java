package com.netroute.routing;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DijkstraRouterTest {

    private final DijkstraRouter router = new DijkstraRouter();
    private GraphManager graph;
    private Network network;

    @BeforeEach
    void setUp() {
        graph = new GraphManager();
        network = graph.getNetwork();
    }

    @Test
    void threeNodeNetworkHasKnownShortestPath() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addBidirectionalLink("AB", "A", "B", 2.0, 1, 1);
        graph.addBidirectionalLink("BC", "B", "C", 3.0, 1, 1);
        graph.addBidirectionalLink("AC", "A", "C", 10.0, 1, 1);

        Route route = requireRoute("A", "C");

        assertEquals(List.of("A", "B", "C"), route.getNodeIds());
        assertEquals(List.of("AB", "BC"), route.getLinkIds());
        assertEquals(5.0, route.getTotalCost());
    }

    @Test
    void cheaperMultiLinkRouteBeatsExpensiveDirectLink() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addBidirectionalLink("AC", "A", "C", 10.0, 1, 1);
        graph.addBidirectionalLink("AB", "A", "B", 1.0, 1, 1);
        graph.addBidirectionalLink("BC", "B", "C", 1.0, 1, 1);

        Route route = requireRoute("A", "C");

        assertEquals(List.of("A", "B", "C"), route.getNodeIds());
        assertEquals(2.0, route.getTotalCost());
    }

    @Test
    void selectsCheaperOfTwoAlternativeRoutes() {
        graph.addComputer("A", "A", 0, 0);
        graph.addRouter("R1", "R1", 1, 0);
        graph.addRouter("R2", "R2", 1, 1);
        graph.addServer("S", "S", 2, 0);
        graph.addBidirectionalLink("AR1", "A", "R1", 1.0, 1, 1);
        graph.addBidirectionalLink("R1S", "R1", "S", 5.0, 1, 1);
        graph.addBidirectionalLink("AR2", "A", "R2", 2.0, 1, 1);
        graph.addBidirectionalLink("R2S", "R2", "S", 2.0, 1, 1);

        Route route = requireRoute("A", "S");

        assertEquals(List.of("A", "R2", "S"), route.getNodeIds());
        assertEquals(4.0, route.getTotalCost());
    }

    @Test
    void equalCostRoutesPickDeterministicPredecessor() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 1, 1);
        graph.addComputer("D", "D", 2, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1.0, 1, 1);
        graph.addBidirectionalLink("AC", "A", "C", 1.0, 1, 1);
        graph.addBidirectionalLink("BD", "B", "D", 1.0, 1, 1);
        graph.addBidirectionalLink("CD", "C", "D", 1.0, 1, 1);

        Route route = requireRoute("A", "D");

        assertEquals(List.of("A", "B", "D"), route.getNodeIds());
        assertEquals(2.0, route.getTotalCost());
        assertEquals(requireRoute("A", "D").getNodeIds(), route.getNodeIds());
    }

    @Test
    void inactiveLinkOnShortestRouteForcesDetour() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1.0, 1, 1);
        graph.addBidirectionalLink("BC", "B", "C", 1.0, 1, 1);
        graph.addBidirectionalLink("AC", "A", "C", 10.0, 1, 1);

        assertEquals(List.of("A", "B", "C"), requireRoute("A", "C").getNodeIds());

        graph.deactivateLink("AB");
        Route detour = requireRoute("A", "C");

        assertEquals(List.of("A", "C"), detour.getNodeIds());
        assertEquals(List.of("AC"), detour.getLinkIds());
        assertEquals(10.0, detour.getTotalCost());
        assertTrue(network.containsLink("AB"));
        assertTrue(network.getActiveOutgoingLinks("A").stream().noneMatch(link -> link.getId().equals("AB")));
    }

    @Test
    void unreachableDestinationReturnsEmpty() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addDirectedLink("AB", "A", "B", 1.0, 1, 1);

        assertTrue(router.findRoute(network, "B", "A").isEmpty());
        assertEquals(List.of("A", "B"), requireRoute("A", "B").getNodeIds());
    }

    @Test
    void disconnectedNetworkIsUnreachableAcrossComponents() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addComputer("D", "D", 3, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1.0, 1, 1);
        graph.addBidirectionalLink("CD", "C", "D", 1.0, 1, 1);

        assertTrue(router.findRoute(network, "A", "D").isEmpty());
        assertEquals(List.of("C", "D"), requireRoute("C", "D").getNodeIds());
    }

    @Test
    void sourceEqualToDestinationIsZeroCostRoute() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addBidirectionalLink("AB", "A", "B", 4.0, 1, 1);

        Route stay = requireRoute("A", "A");

        assertEquals(List.of("A"), stay.getNodeIds());
        assertTrue(stay.getLinkIds().isEmpty());
        assertEquals(0.0, stay.getTotalCost());
        assertEquals("A", stay.getSource().getId());
        assertEquals("A", stay.getDestination().getId());
    }

    @Test
    void rejectsInvalidInputAndNegativeLinkCost() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);

        assertThrows(NullPointerException.class, () -> router.findRoute(null, "A", "B"));
        assertThrows(IllegalArgumentException.class, () -> router.findRoute(network, " ", "B"));
        assertThrows(IllegalArgumentException.class, () -> router.findRoute(network, "A", "MISSING"));
        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addBidirectionalLink("AB", "A", "B", -1.0, 1, 1));

        graph.addBidirectionalLink("AB", "A", "B", 1.0, 1, 1);
        setCostUnvalidated(network.getLink("AB"), -2.0);
        assertThrows(IllegalArgumentException.class, () -> router.findRoute(network, "A", "B"));
    }

    @Test
    void emptyGraphRejectsUnknownEndpoints() {
        Network empty = new GraphManager().getNetwork();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> router.findRoute(empty, "A", "B"));
        assertTrue(ex.getMessage().contains("Unknown node ID"));
    }

    @Test
    void sampleNetworkRoutesLabPcToServerViaCheapestPath() {
        Network sample = GraphManager.createSampleNetwork().getNetwork();
        Route route = router.findRoute(sample, "PC1", "S1").orElseThrow();

        assertEquals(List.of("PC1", "R1", "R2", "S1"), route.getNodeIds());
        assertEquals(5.0, route.getTotalCost());
    }

    private Route requireRoute(String source, String destination) {
        Optional<Route> route = router.findRoute(network, source, destination);
        assertTrue(route.isPresent(), "expected a route from " + source + " to " + destination);
        return route.get();
    }

    private static void setCostUnvalidated(NetworkLink link, double cost) {
        try {
            Field field = NetworkLink.class.getDeclaredField("cost");
            field.setAccessible(true);
            field.setDouble(link, cost);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

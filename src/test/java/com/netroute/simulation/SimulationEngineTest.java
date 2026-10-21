package com.netroute.simulation;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.Packet;
import com.netroute.model.PacketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    private GraphManager graph;
    private Network network;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        graph = new GraphManager();
        network = graph.getNetwork();
    }

    @Test
    void deliversPacketAlongShortestRoute() {
        triangleWithDetour();
        engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "C", 4);
        engine.run();

        assertEquals(PacketStatus.DELIVERED, packet.getStatus());
        assertEquals(List.of("A", "B", "C"), packet.getVisitedNodeIds());
        assertEquals(List.of("AB", "BC"), packet.getRoute().orElseThrow().getLinkIds());
        assertEquals(1, engine.getSentCount());
        assertEquals(1, engine.getDeliveredCount());
        assertEquals(0, engine.getFailedCount());
        assertEquals(0, engine.getDroppedCount());
        assertEquals(4L, packet.getDeliveredAt().orElseThrow());
        assertTrue(engine.isIdle());
    }

    @Test
    void failsWhenNoRouteExists() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "B", 1);
        engine.run();

        assertEquals(PacketStatus.FAILED, packet.getStatus());
        assertEquals("No available route", packet.getReason().orElseThrow());
        assertEquals(0, engine.getSentCount());
        assertEquals(1, engine.getFailedCount());
        assertEquals(0, engine.getDeliveredCount());
    }

    @Test
    void reroutesAfterLinkFailureOnRemainingPath() {
        triangleWithDetour();
        engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "C", 1);
        engine.step();
        assertEquals(PacketStatus.IN_TRANSIT, packet.getStatus());
        assertEquals("AB", packet.nextLink().orElseThrow().getId());

        engine.failLink("BC");
        engine.run();

        assertEquals(PacketStatus.DELIVERED, packet.getStatus());
        assertEquals(1, packet.getRerouteCount());
        assertEquals(List.of("AC"), packet.getRoute().orElseThrow().getLinkIds());
        assertTrue(packet.getVisitedNodeIds().contains("C"));
        assertFalse(network.getLink("BC").isActive());
        assertEquals(1, engine.getDeliveredCount());
        assertEquals(0, engine.getDroppedCount());
    }

    @Test
    void dropsPacketWhenFailedLinkHasNoAlternative() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1.0, 2.0, 1);
        graph.addBidirectionalLink("BC", "B", "C", 1.0, 2.0, 1);
        engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "C", 1);
        engine.step();
        engine.failLink("BC");
        engine.run();

        assertEquals(PacketStatus.DROPPED, packet.getStatus());
        assertTrue(packet.getReason().orElseThrow().contains("no alternative route"));
        assertEquals(1, engine.getDroppedCount());
        assertEquals(0, engine.getDeliveredCount());
        assertEquals(1, engine.getSentCount());
    }

    @Test
    void deliversMultiplePacketsInDeterministicOrder() {
        triangleWithDetour();
        engine = new SimulationEngine(network);

        Packet first = engine.submitPacket("A", "C", 1);
        Packet second = engine.submitPacket("A", "C", 1);
        engine.run();

        assertEquals("P1", first.getId());
        assertEquals("P2", second.getId());
        assertEquals(PacketStatus.DELIVERED, first.getStatus());
        assertEquals(PacketStatus.DELIVERED, second.getStatus());
        assertEquals(2, engine.getSentCount());
        assertEquals(2, engine.getDeliveredCount());
        assertTrue(second.getDeliveredAt().orElseThrow() >= first.getDeliveredAt().orElseThrow());
    }

    @Test
    void handlesEmptyNetworkAndUnknownEndpointsWithoutThrowing() {
        engine = new SimulationEngine(new GraphManager().getNetwork());
        Packet missing = engine.submitPacket("A", "B", 1);

        assertEquals(PacketStatus.FAILED, missing.getStatus());
        assertEquals("Unknown source or destination", missing.getReason().orElseThrow());
        assertEquals(1, engine.getFailedCount());

        triangleWithDetour();
        engine = new SimulationEngine(network);
        Packet unknownDest = engine.submitPacket("A", "Z", 1);
        assertEquals(PacketStatus.FAILED, unknownDest.getStatus());
        assertThrows(IllegalArgumentException.class, () -> engine.submitPacket(" ", "C", 1));
    }

    @Test
    void sameSourceAndDestinationDeliversImmediately() {
        triangleWithDetour();
        engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "A", 1);

        assertEquals(PacketStatus.DELIVERED, packet.getStatus());
        assertEquals(0L, packet.getDeliveredAt().orElseThrow());
        assertTrue(engine.isIdle());
    }

    private void triangleWithDetour() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1.0, 2.0, 1);
        graph.addBidirectionalLink("BC", "B", "C", 1.0, 2.0, 1);
        graph.addBidirectionalLink("AC", "A", "C", 10.0, 2.0, 1);
    }
}

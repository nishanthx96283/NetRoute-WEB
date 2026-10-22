package com.netroute.statistics;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.Packet;
import com.netroute.model.PacketStatus;
import com.netroute.simulation.SimulationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkStatisticsTest {

    private GraphManager graph;
    private Network network;

    @BeforeEach
    void setUp() {
        graph = new GraphManager();
        network = graph.getNetwork();
    }

    @Test
    void emptySimulationReturnsZerosWithoutErrors() {
        graph.addComputer("A", "A", 0, 0);
        SimulationEngine engine = new SimulationEngine(network);
        NetworkStatistics stats = new NetworkStatistics(engine);

        assertEquals(0, stats.getTotalPackets());
        assertEquals(0, stats.getDeliveredCount());
        assertEquals(0, stats.getFailedCount());
        assertEquals(0, stats.getDroppedCount());
        assertEquals(0.0, stats.getDeliveryRatio());
        assertEquals(0.0, stats.getAverageDeliveryDelay());
        assertEquals(1, stats.getTotalNodes());
        assertEquals(0, stats.getTotalLinks());
        assertEquals(0, stats.getActiveLinks());
        assertEquals(0, stats.getInactiveLinks());
        assertEquals(0, stats.getLinkFailureCount());
        assertEquals(0, stats.getTotalReroutes());
    }

    @Test
    void allPacketsDeliveredShowsFullDeliveryRatio() {
        triangleNetwork();
        SimulationEngine engine = new SimulationEngine(network);

        engine.submitPacket("A", "C", 1);
        engine.submitPacket("A", "B", 1);
        engine.run();

        NetworkStatistics stats = new NetworkStatistics(engine);

        assertEquals(2, stats.getTotalPackets());
        assertEquals(2, stats.getDeliveredCount());
        assertEquals(0, stats.getFailedCount());
        assertEquals(0, stats.getDroppedCount());
        assertEquals(100.0, stats.getDeliveryRatio());
        assertTrue(stats.getAverageDeliveryDelay() > 0.0,
                "Delivered packets should have a positive simulated delay");
        assertEquals(0, stats.getLinkFailureCount());
        assertEquals(0, stats.getTotalReroutes());
    }

    @Test
    void failedPacketGivesZeroDeliveryRatioAndZeroDelay() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        // No link — route will fail
        SimulationEngine engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "B", 1);
        engine.run();

        NetworkStatistics stats = new NetworkStatistics(engine);

        assertEquals(1, stats.getTotalPackets());
        assertEquals(0, stats.getDeliveredCount());
        assertEquals(1, stats.getFailedCount());
        assertEquals(0.0, stats.getDeliveryRatio());
        assertEquals(0.0, stats.getAverageDeliveryDelay());
        assertEquals(PacketStatus.FAILED, packet.getStatus());
    }

    @Test
    void rerouteAfterLinkFailureIsTracked() {
        triangleNetwork();
        SimulationEngine engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "C", 1);
        engine.step(); // P1 starts traversing link AB
        engine.failLink("BC"); // BC goes down, P1 must reroute via AC
        engine.run();

        NetworkStatistics stats = new NetworkStatistics(engine);

        assertEquals(1, stats.getTotalPackets());
        assertEquals(1, stats.getDeliveredCount());
        assertEquals(PacketStatus.DELIVERED, packet.getStatus());
        assertEquals(100.0, stats.getDeliveryRatio());
        assertEquals(1, stats.getLinkFailureCount());
        assertEquals(1, stats.getTotalReroutes());
        // BC is now inactive
        assertEquals(1, stats.getInactiveLinks());
        assertEquals(2, stats.getActiveLinks());
    }

    @Test
    void droppedPacketWhenNoAlternativeAfterLinkFailure() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1.0, 2.0, 1);
        graph.addBidirectionalLink("BC", "B", "C", 1.0, 2.0, 1);
        SimulationEngine engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "C", 1);
        engine.step();
        engine.failLink("BC"); // No alternative path exists
        engine.run();

        NetworkStatistics stats = new NetworkStatistics(engine);

        assertEquals(1, stats.getTotalPackets());
        assertEquals(0, stats.getDeliveredCount());
        assertEquals(1, stats.getDroppedCount());
        assertEquals(PacketStatus.DROPPED, packet.getStatus());
        assertEquals(0.0, stats.getDeliveryRatio());
        assertEquals(0.0, stats.getAverageDeliveryDelay());
        assertEquals(1, stats.getLinkFailureCount());
        assertEquals(0, stats.getTotalReroutes());
    }

    @Test
    void routeCostIsAvailableForRoutedPacket() {
        triangleNetwork();
        SimulationEngine engine = new SimulationEngine(network);

        Packet packet = engine.submitPacket("A", "C", 1);
        engine.run();

        NetworkStatistics stats = new NetworkStatistics(engine);

        // A->B->C costs 1.0 + 1.0 = 2.0
        assertTrue(stats.getRouteCost(packet.getId()).isPresent());
        assertEquals(2.0, stats.getRouteCost(packet.getId()).orElseThrow());
    }

    @Test
    void routeCostIsEmptyForUnknownPacketId() {
        triangleNetwork();
        SimulationEngine engine = new SimulationEngine(network);
        NetworkStatistics stats = new NetworkStatistics(engine);

        assertTrue(stats.getRouteCost("UNKNOWN").isEmpty());
    }

    @Test
    void routeCostIsEmptyForFailedPacketWithNoRoute() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        SimulationEngine engine = new SimulationEngine(network);

        // Fails because no link exists
        Packet packet = engine.submitPacket("A", "B", 1);
        NetworkStatistics stats = new NetworkStatistics(engine);

        assertFalse(stats.getRouteCost(packet.getId()).isPresent());
    }

    @Test
    void sameSourceAndDestinationHasZeroDelay() {
        triangleNetwork();
        SimulationEngine engine = new SimulationEngine(network);

        engine.submitPacket("A", "A", 1);
        engine.run();

        NetworkStatistics stats = new NetworkStatistics(engine);

        assertEquals(1, stats.getDeliveredCount());
        assertEquals(0.0, stats.getAverageDeliveryDelay());
        assertEquals(100.0, stats.getDeliveryRatio());
    }

    @Test
    void mixOfDeliveredAndFailedPackets() {
        triangleNetwork();
        SimulationEngine engine = new SimulationEngine(network);

        // This one will be delivered
        engine.submitPacket("A", "C", 1);
        // This one fails (unknown destination)
        engine.submitPacket("A", "MISSING", 1);
        engine.run();

        NetworkStatistics stats = new NetworkStatistics(engine);

        assertEquals(2, stats.getTotalPackets());
        assertEquals(1, stats.getDeliveredCount());
        assertEquals(1, stats.getFailedCount());
        assertEquals(50.0, stats.getDeliveryRatio());
        assertTrue(stats.getAverageDeliveryDelay() > 0.0,
                "Average delay should only count the one delivered packet");
    }

    @Test
    void toStringContainsAllMetricLabels() {
        triangleNetwork();
        SimulationEngine engine = new SimulationEngine(network);
        engine.submitPacket("A", "C", 1);
        engine.run();

        NetworkStatistics stats = new NetworkStatistics(engine);
        String summary = stats.toString();

        assertTrue(summary.contains("Packets submitted"));
        assertTrue(summary.contains("Delivered"));
        assertTrue(summary.contains("Failed"));
        assertTrue(summary.contains("Dropped"));
        assertTrue(summary.contains("Delivery ratio"));
        assertTrue(summary.contains("Avg delivery delay"));
        assertTrue(summary.contains("ticks"));
        assertTrue(summary.contains("Total nodes"));
        assertTrue(summary.contains("Total links"));
        assertTrue(summary.contains("Active links"));
        assertTrue(summary.contains("Inactive links"));
        assertTrue(summary.contains("Link failures"));
        assertTrue(summary.contains("Successful reroutes"));
    }

    @Test
    void networkTopologyMetricsMatchGraph() {
        triangleNetwork();
        SimulationEngine engine = new SimulationEngine(network);
        NetworkStatistics stats = new NetworkStatistics(engine);

        assertEquals(3, stats.getTotalNodes());
        assertEquals(3, stats.getTotalLinks());
        assertEquals(3, stats.getActiveLinks());
        assertEquals(0, stats.getInactiveLinks());
    }

    /**
     * Builds a three-node triangle: A -- B -- C with a direct A -- C detour.
     * AB and BC cost 1.0 each (shortest path A->B->C = 2.0).
     * AC costs 10.0 (the expensive backup route).
     * All links have delay 2.0 ticks and capacity 1.
     */
    private void triangleNetwork() {
        graph.addComputer("A", "A", 0, 0);
        graph.addComputer("B", "B", 1, 0);
        graph.addComputer("C", "C", 2, 0);
        graph.addBidirectionalLink("AB", "A", "B", 1.0, 2.0, 1);
        graph.addBidirectionalLink("BC", "B", "C", 1.0, 2.0, 1);
        graph.addBidirectionalLink("AC", "A", "C", 10.0, 2.0, 1);
    }
}

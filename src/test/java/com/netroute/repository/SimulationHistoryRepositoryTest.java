package com.netroute.repository;

import com.netroute.graph.Network;
import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.Router;
import com.netroute.model.Server;
import com.netroute.routing.DijkstraRouter;
import com.netroute.simulation.SimulationEngine;
import com.netroute.statistics.NetworkStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationHistoryRepositoryTest {

    private DatabaseConnection databaseConnection;
    private SimulationHistoryRepository repository;
    private TopologyRepository topologyRepository;

    @BeforeEach
    void setUp() {
        databaseConnection = new DatabaseConnection();
        repository = new SimulationHistoryRepository(databaseConnection);
        topologyRepository = new TopologyRepository(databaseConnection, repository);
    }

    private boolean isDatabaseAvailable() {
        return databaseConnection.isAvailable();
    }

    @Test
    void testListAllRunsReturnsNewestFirstWithStableIdSort() throws Exception {
        if (!isDatabaseAvailable()) {
            return;
        }

        long timestampSuffix = System.currentTimeMillis();
        String topName = "OrderTest_Top_" + timestampSuffix;

        // Create a test topology
        Network net = new Network();
        net.addNode(new Computer("PC1", "PC 1", 100, 100));
        net.addNode(new Server("S1", "Server 1", 300, 100));
        net.addLink(new NetworkLink("L1", "PC1", "S1", 1.0, 1.0, 10, false));
        int topId = topologyRepository.saveTopology(topName, "Ordering test topology", net, false);

        SimulationEngine engine = new SimulationEngine(net, new DijkstraRouter());
        NetworkStatistics stats = new NetworkStatistics(engine);

        // Insert Run A
        int runA = repository.recordSimulationRun(topId, topName, stats, 10, "Run A notes");
        // Insert Run B
        int runB = repository.recordSimulationRun(topId, topName, stats, 20, "Run B notes");

        // Force both to have the exact same run_timestamp to test stable secondary ordering by id DESC
        Timestamp tieTimestamp = Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE simulation_runs SET run_timestamp = ? WHERE id IN (?, ?)")) {
            ps.setTimestamp(1, tieTimestamp);
            ps.setInt(2, runA);
            ps.setInt(3, runB);
            ps.executeUpdate();
        }

        // Insert Run C with a later timestamp (2026-01-15 11:00:00)
        int runC = repository.recordSimulationRun(topId, topName, stats, 30, "Run C notes");
        Timestamp laterTimestamp = Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 11, 0, 0));
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE simulation_runs SET run_timestamp = ? WHERE id = ?")) {
            ps.setTimestamp(1, laterTimestamp);
            ps.setInt(2, runC);
            ps.executeUpdate();
        }

        // Query all runs
        List<SimulationRunRecord> allRuns = repository.listAllRuns();
        assertNotNull(allRuns);
        assertFalse(allRuns.isEmpty());

        // Find the relative positions of runC, runB, runA
        int idxC = -1, idxB = -1, idxA = -1;
        for (int i = 0; i < allRuns.size(); i++) {
            if (allRuns.get(i).getId() == runC) idxC = i;
            if (allRuns.get(i).getId() == runB) idxB = i;
            if (allRuns.get(i).getId() == runA) idxA = i;
        }

        assertTrue(idxC >= 0, "runC must be in list");
        assertTrue(idxB >= 0, "runB must be in list");
        assertTrue(idxA >= 0, "runA must be in list");

        // Run C has newer timestamp -> must come before Run B and Run A
        assertTrue(idxC < idxB, "Newer timestamp (runC) must appear before runB");
        assertTrue(idxC < idxA, "Newer timestamp (runC) must appear before runA");

        // Run B and Run A have identical timestamps, but Run B has higher ID -> Run B must appear before Run A
        assertTrue(runB > runA, "runB should have higher id than runA");
        assertTrue(idxB < idxA, "Equal timestamp must tie-break with id DESC: runB before runA");

        // Clean up
        repository.deleteRun(runA);
        repository.deleteRun(runB);
        repository.deleteRun(runC);
        topologyRepository.deleteTopologyPermanently(topId);
    }

    @Test
    void testRecordAndMapSimulationMetricsCompletely() throws Exception {
        if (!isDatabaseAvailable()) {
            return;
        }

        long suffix = System.currentTimeMillis();
        String topName = "MetricMap_Top_" + suffix;

        Network net = new Network();
        net.addNode(new Computer("C1", "Client", 50, 50));
        net.addNode(new Router("R1", "Router", 150, 50));
        net.addNode(new Server("S1", "Server", 250, 50));
        net.addLink(new NetworkLink("L1", "C1", "R1", 2.0, 3.0, 5, false));
        net.addLink(new NetworkLink("L2", "R1", "S1", 1.5, 2.5, 5, false));
        int topId = topologyRepository.saveTopology(topName, "Metric map test", net, false);

        SimulationEngine engine = new SimulationEngine(net, new DijkstraRouter());
        engine.submitPacket("C1", "S1", 100);
        engine.failLink("L1");
        engine.step();
        engine.restoreLink("L1");

        NetworkStatistics stats = new NetworkStatistics(engine);
        String scenarioNotes = "Detailed scenario test notes with special chars: 100% OK!";

        int runId = repository.recordSimulationRun(topId, topName, stats, 45, scenarioNotes);
        assertTrue(runId > 0);

        Optional<SimulationRunRecord> recordOpt = repository.getRunById(runId);
        assertTrue(recordOpt.isPresent());
        SimulationRunRecord record = recordOpt.get();

        assertEquals(runId, record.getId());
        assertEquals(topId, record.getTopologyId());
        assertEquals(topName, record.getTopologyName());
        assertNotNull(record.getRunTimestamp());
        assertEquals(45, record.getSimulationTicks());
        assertEquals(stats.getTotalPackets(), record.getTotalPackets());
        assertEquals(stats.getDeliveredCount(), record.getDeliveredPackets());
        assertEquals(stats.getFailedCount(), record.getFailedPackets());
        assertEquals(stats.getDroppedCount(), record.getDroppedPackets());
        assertEquals(stats.getDeliveryRatio(), record.getDeliveryRatio(), 0.001);
        assertEquals(stats.getAverageDeliveryDelay(), record.getAvgDeliveryDelay(), 0.001);
        assertEquals(stats.getCurrentlyFailedLinks(), record.getLinkFailures());
        assertEquals(0, record.getLinkFailures(), "Link was restored before save, so currently failed links must be 0");
        assertEquals(stats.getTotalReroutes(), record.getReroutes());
        assertEquals(scenarioNotes, record.getNotes());

        // Clean up
        repository.deleteRun(runId);
        topologyRepository.deleteTopologyPermanently(topId);
    }

    @Test
    void testHandlingRunsWithNullTopologyIdAndRetentionOnTopologyDeletion() throws Exception {
        if (!isDatabaseAvailable()) {
            return;
        }

        long suffix = System.currentTimeMillis();
        String standaloneName = "Standalone_Unlinked_Run_" + suffix;

        SimulationEngine engine = new SimulationEngine(new Network(), new DijkstraRouter());
        NetworkStatistics stats = new NetworkStatistics(engine);

        // 1. Record run directly with null topologyId (e.g. unsaved network simulation)
        int run1 = repository.recordSimulationRun(null, standaloneName, stats, 15, "Unlinked run notes");
        assertTrue(run1 > 0);

        Optional<SimulationRunRecord> rec1 = repository.getRunById(run1);
        assertTrue(rec1.isPresent());
        assertNull(rec1.get().getTopologyId(), "topologyId must be null");
        assertEquals(standaloneName, rec1.get().getTopologyName(), "Denormalized name preserved");

        // 2. Record run with topology, then permanently delete topology
        String topName = "Ephemeral_Top_" + suffix;
        Network net = new Network();
        net.addNode(new Computer("C2", "Client", 100, 100));
        int topId = topologyRepository.saveTopology(topName, "Ephemeral topology", net, false);

        int run2 = repository.recordSimulationRun(topId, topName, stats, 25, "Pre-deletion run notes");
        assertTrue(run2 > 0);

        // Permanently delete the topology
        boolean deleted = topologyRepository.deleteTopologyPermanently(topId);
        assertTrue(deleted);

        // Verify history retention: run record still exists, topologyId set to NULL, denormalized name intact
        Optional<SimulationRunRecord> rec2 = repository.getRunById(run2);
        assertTrue(rec2.isPresent(), "Simulation run must NOT be deleted when topology is deleted");
        assertNull(rec2.get().getTopologyId(), "Foreign key must become null via ON DELETE SET NULL");
        assertEquals(topName, rec2.get().getTopologyName(), "Denormalized topology name must persist");
        assertEquals("Pre-deletion run notes", rec2.get().getNotes());

        // Clean up
        repository.deleteRun(run1);
        repository.deleteRun(run2);
    }
}

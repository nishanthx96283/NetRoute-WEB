package com.netroute.repository;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.simulation.SimulationEngine;
import com.netroute.statistics.NetworkStatistics;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test verifying real MySQL connectivity and CRUD operations.
 * <p>
 * Gracefully skipped if a local MySQL instance with {@code netroute_db} is not reachable,
 * allowing standard offline Maven builds to pass cleanly.
 */
class DatabaseConnectionIntegrationTest {

    private static DatabaseConnection dbConn;
    private static boolean isAvailable = false;

    @BeforeAll
    static void checkDatabaseAvailability() {
        dbConn = new DatabaseConnection();
        DatabaseConnection.ConnectionResult result = dbConn.testConnection();
        isAvailable = result.isSuccess();
    }

    @Test
    void canConnectToDatabaseWhenConfigured() {
        Assumptions.assumeTrue(isAvailable, "Skipping: MySQL is not running or credentials not set.");
        DatabaseConnection.ConnectionResult result = dbConn.testConnection();
        assertTrue(result.isSuccess());
        assertNotNull(result.getDatabaseProductName());
    }

    @Test
    void fullTopologyAndSimulationLifecycleIntegration() throws SQLException, DuplicateTopologyNameException {
        Assumptions.assumeTrue(isAvailable, "Skipping: MySQL is not running or credentials not set.");

        TopologyRepository topologyRepo = new TopologyRepository(dbConn);
        SimulationHistoryRepository historyRepo = new SimulationHistoryRepository(dbConn);

        String testName = "TestNet_" + System.currentTimeMillis();
        Network sampleNet = GraphManager.createSampleNetwork().getNetwork();

        // 1. Save new topology
        int savedId = topologyRepo.saveTopology(testName, "Automated Integration Test", sampleNet, false);
        assertTrue(savedId > 0);

        try {
            // 2. Duplicate name detection
            assertThrows(DuplicateTopologyNameException.class, () ->
                    topologyRepo.saveTopology(testName, "Duplicate", sampleNet, false));

            // 3. Overwrite existing topology
            int overwrittenId = topologyRepo.saveTopology(testName, "Overwritten", sampleNet, true);
            assertEquals(savedId, overwrittenId);

            // 4. Load topology back and verify fidelity
            Optional<SavedTopology> loadedOpt = topologyRepo.loadTopology(savedId);
            assertTrue(loadedOpt.isPresent());
            SavedTopology loaded = loadedOpt.get();
            assertEquals(testName, loaded.getName());
            assertEquals(sampleNet.nodeCount(), loaded.getNetwork().nodeCount());
            assertEquals(sampleNet.linkCount(), loaded.getNetwork().linkCount());

            // 5. Record simulation run associated with this topology
            SimulationEngine engine = new SimulationEngine(sampleNet);
            NetworkStatistics stats = new NetworkStatistics(engine);
            int runId = historyRepo.recordSimulationRun(savedId, testName, stats, 15L, "Integration Test Run");
            assertTrue(runId > 0);

            // 6. Verify run was recorded
            List<SimulationRunRecord> runs = historyRepo.listRunsForTopology(savedId);
            assertFalse(runs.isEmpty());
            assertEquals(testName, runs.get(0).getTopologyName());

            // 7. Test archival
            assertTrue(topologyRepo.archiveTopology(savedId));
            List<TopologySummary> active = topologyRepo.listTopologies(false);
            assertFalse(active.stream().anyMatch(t -> t.getId() == savedId));

            assertTrue(topologyRepo.restoreTopology(savedId));
            List<TopologySummary> activeAgain = topologyRepo.listTopologies(false);
            assertTrue(activeAgain.stream().anyMatch(t -> t.getId() == savedId));

            // 8. Delete topology permanently and verify history retention
            assertTrue(topologyRepo.deleteTopologyPermanently(savedId));

            // Confirm topology is deleted
            assertTrue(topologyRepo.loadTopology(savedId).isEmpty());

            // Confirm simulation run still exists with null topology_id and intact topology_name
            Optional<SimulationRunRecord> runAfterDelete = historyRepo.getRunById(runId);
            assertTrue(runAfterDelete.isPresent());
            assertNull(runAfterDelete.get().getTopologyId());
            assertEquals(testName, runAfterDelete.get().getTopologyName());

            // Clean up test simulation run
            historyRepo.deleteRun(runId);
        } finally {
            // Safety cleanup
            topologyRepo.deleteTopologyPermanently(savedId);
        }
    }
}

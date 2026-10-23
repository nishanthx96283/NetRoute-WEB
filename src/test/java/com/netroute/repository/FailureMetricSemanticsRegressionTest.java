package com.netroute.repository;

import com.netroute.graph.Network;
import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.Router;
import com.netroute.model.Server;
import com.netroute.routing.DijkstraRouter;
import com.netroute.service.TopologyStorageService;
import com.netroute.simulation.SimulationEngine;
import com.netroute.statistics.NetworkStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test suite verifying the corrected Failures metric semantics:
 * The Failures column represents the number of links currently in the FAILED/INACTIVE
 * state at the moment the simulation run is saved, NOT cumulative failure events.
 */
class FailureMetricSemanticsRegressionTest {

    private DatabaseConnection databaseConnection;
    private SimulationHistoryRepository historyRepository;
    private TopologyRepository topologyRepository;
    private TopologyStorageService storageService;

    @BeforeEach
    void setUp() {
        databaseConnection = new DatabaseConnection();
        historyRepository = new SimulationHistoryRepository(databaseConnection);
        topologyRepository = new TopologyRepository(databaseConnection, historyRepository);
        storageService = new TopologyStorageService(databaseConnection, topologyRepository, historyRepository);
    }

    private boolean isDatabaseAvailable() {
        return databaseConnection.isAvailable();
    }

    private Network createThreeLinkNetwork() {
        Network net = new Network();
        net.addNode(new Computer("PC1", "Terminal 1", 50, 50));
        net.addNode(new Router("R1", "Router 1", 150, 50));
        net.addNode(new Router("R2", "Router 2", 250, 50));
        net.addNode(new Server("S1", "Server 1", 350, 50));

        net.addLink(new NetworkLink("L1", "PC1", "R1", 1.0, 1.0, 10, false));
        net.addLink(new NetworkLink("L2", "R1", "R2", 1.0, 1.0, 10, false));
        net.addLink(new NetworkLink("L3", "R2", "S1", 1.0, 1.0, 10, false));
        return net;
    }

    @Test
    void testNetworkModelAuthoritativeFailureCounting() {
        Network net = createThreeLinkNetwork();
        assertEquals(3, net.linkCount());

        // 1. Initially no links are failed
        assertEquals(0, net.countInactiveLinks(), "Initial network must have 0 failed links");

        // 2. Fail two links
        net.setLinkActive("L1", false);
        net.setLinkActive("L2", false);
        assertEquals(2, net.countInactiveLinks(), "Two links failed must report 2");

        // 3. Restore one of those links
        net.setLinkActive("L1", true);
        assertEquals(1, net.countInactiveLinks(), "Restoring L1 while L2 remains failed must report 1");

        // 4. Fail one additional link while one remains failed
        net.setLinkActive("L3", false);
        assertEquals(2, net.countInactiveLinks(), "Failing L3 while L2 remains failed must report 2");

        // 5. Restore all failed links
        net.setLinkActive("L2", true);
        net.setLinkActive("L3", true);
        assertEquals(0, net.countInactiveLinks(), "Restoring all links must report 0");
    }

    @Test
    void testSimulationRunPersistenceFailureSemanticsAcrossAllLifecycleTransitions() throws Exception {
        if (!isDatabaseAvailable()) {
            return;
        }

        long suffix = System.currentTimeMillis();
        String topName = "FailSemantics_" + suffix;
        Network net = createThreeLinkNetwork();
        int topId = topologyRepository.saveTopology(topName, "Semantics regression test", net, false);

        SimulationEngine engine = new SimulationEngine(net, new DijkstraRouter());
        engine.submitPacket("PC1", "S1", 100);

        // Step 0: Save when NO links are failed -> save Failures = 0
        NetworkStatistics stats0 = new NetworkStatistics(engine);
        int run0 = historyRepository.recordSimulationRun(topId, topName, stats0, 10, "Run 0: No failures");
        Optional<SimulationRunRecord> rec0 = historyRepository.getRunById(run0);
        assertTrue(rec0.isPresent());
        assertEquals(0, rec0.get().getLinkFailures(), "No links failed must persist Failures = 0");

        // Step 1: Two links failed -> save Failures = 2
        engine.failLink("L1");
        engine.failLink("L2");
        NetworkStatistics stats1 = new NetworkStatistics(engine);
        assertEquals(2, engine.getLinkFailureCount(), "Cumulative counter is 2");
        assertEquals(2, net.countInactiveLinks(), "Currently inactive links is 2");

        int run1 = historyRepository.recordSimulationRun(topId, topName, stats1, 20, "Run 1: Two links failed");
        Optional<SimulationRunRecord> rec1 = historyRepository.getRunById(run1);
        assertTrue(rec1.isPresent());
        assertEquals(2, rec1.get().getLinkFailures(), "Two links failed must persist Failures = 2");

        // Step 2: Restore one of those links -> save Failures = 1
        engine.restoreLink("L1");
        NetworkStatistics stats2 = new NetworkStatistics(engine);
        assertEquals(2, engine.getLinkFailureCount(), "Cumulative counter remains 2");
        assertEquals(1, net.countInactiveLinks(), "Currently inactive links is now 1");

        int run2 = historyRepository.recordSimulationRun(topId, topName, stats2, 30, "Run 2: One link restored");
        Optional<SimulationRunRecord> rec2 = historyRepository.getRunById(run2);
        assertTrue(rec2.isPresent());
        assertEquals(1, rec2.get().getLinkFailures(), "Restoring one link must persist Failures = 1 (NOT cumulative 2)");

        // Step 3: Fail one additional link while one remains failed -> save Failures = 2
        engine.failLink("L3");
        NetworkStatistics stats3 = new NetworkStatistics(engine);
        assertEquals(3, engine.getLinkFailureCount(), "Cumulative counter is 3");
        assertEquals(2, net.countInactiveLinks(), "Currently inactive links is 2 (L2 and L3)");

        int run3 = historyRepository.recordSimulationRun(topId, topName, stats3, 40, "Run 3: Additional link failed");
        Optional<SimulationRunRecord> rec3 = historyRepository.getRunById(run3);
        assertTrue(rec3.isPresent());
        assertEquals(2, rec3.get().getLinkFailures(), "Additional link failed while one remains failed must persist Failures = 2 (NOT cumulative 3)");

        // Step 4: Restore all failed links -> save Failures = 0
        engine.restoreLink("L2");
        engine.restoreLink("L3");
        NetworkStatistics stats4 = new NetworkStatistics(engine);
        assertEquals(3, engine.getLinkFailureCount(), "Cumulative counter remains 3");
        assertEquals(0, net.countInactiveLinks(), "Currently inactive links is now 0");

        int run4 = historyRepository.recordSimulationRun(topId, topName, stats4, 50, "Run 4: All links restored");
        Optional<SimulationRunRecord> rec4 = historyRepository.getRunById(run4);
        assertTrue(rec4.isPresent());
        assertEquals(0, rec4.get().getLinkFailures(), "Restoring all links must persist Failures = 0 (NOT cumulative 3)");

        // Clean up
        historyRepository.deleteRun(run0);
        historyRepository.deleteRun(run1);
        historyRepository.deleteRun(run2);
        historyRepository.deleteRun(run3);
        historyRepository.deleteRun(run4);
        topologyRepository.deleteTopologyPermanently(topId);
    }

    @Test
    void testUnifiedSaveWorkflowPersistsCurrentlyFailedLinks() throws Exception {
        if (!isDatabaseAvailable()) {
            return;
        }

        long suffix = System.currentTimeMillis();
        String topName = "Unified_FailSemantics_" + suffix;
        Network net = createThreeLinkNetwork();

        // Fail 2 links, then restore 1 link -> exactly 1 link failed
        net.setLinkActive("L1", false);
        net.setLinkActive("L2", false);
        net.setLinkActive("L1", true);
        assertEquals(1, net.countInactiveLinks());

        SimulationEngine engine = new SimulationEngine(net, new DijkstraRouter());
        engine.submitPacket("PC1", "S1", 100);
        NetworkStatistics stats = new NetworkStatistics(engine);

        UnifiedSaveResult result = storageService.saveUnified(
                null,
                topName,
                "Testing unified save failure semantics",
                net,
                false,
                stats,
                25L,
                "Unified save notes"
        );
        assertNotNull(result);
        assertTrue(result.hasSimulationRun());

        int runId = result.getSimulationRunId();
        Optional<SimulationRunRecord> recordOpt = storageService.getRunById(runId);
        assertTrue(recordOpt.isPresent());
        assertEquals(1, recordOpt.get().getLinkFailures(), "Unified save must persist currently failed links = 1");

        // Clean up
        storageService.deleteRun(runId);
        storageService.deleteTopologyPermanently(result.getTopologyId());
    }
}

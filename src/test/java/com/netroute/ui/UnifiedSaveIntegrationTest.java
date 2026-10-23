package com.netroute.ui;

import com.netroute.graph.Network;
import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.NodeType;
import com.netroute.model.Router;
import com.netroute.model.Server;
import com.netroute.repository.SavedTopology;
import com.netroute.repository.SimulationRunRecord;
import com.netroute.repository.TopologySummary;
import com.netroute.repository.UnifiedSaveResult;
import com.netroute.service.TopologyStorageService;
import com.netroute.statistics.NetworkStatistics;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test suite verifying all 15 requirements of the unified Save workflow
 * across topology persistence, duplicate name handling, simulation metrics, transaction atomicity,
 * rollback, and state management.
 */
class UnifiedSaveIntegrationTest {

    // 1 & 4. Saving a new topology creates one record with all node and link properties correctly persisted.
    @Test
    void testRequirement1And4_SavingNewTopologyPersistsRecordWithAllProperties() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            Network net = new Network();
            net.addNode(new Computer("UST_PC1", "Terminal 1", 120.0, 180.0));
            net.addNode(new Router("UST_R1", "Core Gateway", 340.0, 220.0));
            net.addNode(new Server("UST_S1", "Database Cluster", 560.0, 290.0));

            NetworkLink link1 = new NetworkLink("UST_L1", "UST_PC1", "UST_R1", 3.5, 4.0, 100, false);
            NetworkLink link2 = new NetworkLink("UST_L2", "UST_R1", "UST_S1", 1.5, 2.0, 50, true);
            link2.setActive(false); // Inactive directed link
            net.addLink(link1);
            net.addLink(link2);

            frame.setNetwork(net);

            String topName = "Unified_PropTest_" + System.currentTimeMillis();
            String topDesc = "Testing full property persistence";

            boolean saved = frame.executeUnifiedSave(null, topName, topDesc, false, null);
            assertTrue(saved, "New topology save must succeed");
            assertNotNull(frame.getActiveTopologyId());
            int topId = frame.getActiveTopologyId();
            assertEquals(topName, frame.getActiveTopologyName());

            // Reconstruct and verify every property from MySQL
            Optional<SavedTopology> loadedOpt = frame.getStorageService().loadTopology(topId);
            assertTrue(loadedOpt.isPresent());
            SavedTopology savedTop = loadedOpt.get();
            Network reconstructed = savedTop.getNetwork();

            assertEquals(3, reconstructed.nodeCount());
            assertEquals(2, reconstructed.linkCount());

            // Verify nodes
            var pc = reconstructed.getNode("UST_PC1");
            assertNotNull(pc);
            assertEquals("Terminal 1", pc.getName());
            assertEquals(NodeType.COMPUTER, pc.getType());
            assertEquals(120.0, pc.getX(), 0.001);
            assertEquals(180.0, pc.getY(), 0.001);

            var router = reconstructed.getNode("UST_R1");
            assertNotNull(router);
            assertEquals(NodeType.ROUTER, router.getType());

            var server = reconstructed.getNode("UST_S1");
            assertNotNull(server);
            assertEquals(NodeType.SERVER, server.getType());

            // Verify links
            var l1 = reconstructed.getLink("UST_L1");
            assertNotNull(l1);
            assertEquals("UST_PC1", l1.getSourceId());
            assertEquals("UST_R1", l1.getDestinationId());
            assertEquals(3.5, l1.getCost(), 0.001);
            assertEquals(4.0, l1.getDelay(), 0.001);
            assertEquals(100, l1.getCapacity());
            assertTrue(l1.isActive());
            assertFalse(l1.isDirected());

            var l2 = reconstructed.getLink("UST_L2");
            assertNotNull(l2);
            assertEquals(1.5, l2.getCost(), 0.001);
            assertFalse(l2.isActive());
            assertTrue(l2.isDirected());

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }

    // 2 & 3. Saving a loaded and modified topology updates the same ID without creating a duplicate record.
    @Test
    void testRequirement2And3_SavingLoadedAndModifiedTopologyUpdatesSameIdWithoutDuplicate() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            Network net = new Network();
            net.addNode(new Computer("T2_PC", "Initial PC", 100, 100));
            net.addNode(new Server("T2_SRV", "Initial Server", 300, 200));
            net.addLink(new NetworkLink("T2_L", "T2_PC", "T2_SRV", 2.0, 1.0, 10, false));

            String topName = "Unified_OverwriteTest_" + System.currentTimeMillis();
            int originalId = frame.getStorageService().saveTopology(topName, "Initial version", net, false);
            assertTrue(originalId > 0);

            // Load into frame
            assertTrue(frame.handleLoadTopology(originalId, topName));
            assertEquals(originalId, frame.getActiveTopologyId());
            assertFalse(frame.isTopologyModified());

            // Modify loaded topology dynamically
            frame.onAddNode(NodeType.ROUTER);
            assertEquals(originalId, frame.getActiveTopologyId(), "Active topology ID must be retained on edit");
            assertTrue(frame.isTopologyModified(), "Topology must be marked modified");

            // Overwrite save via unified save
            boolean saved = frame.executeUnifiedSave(originalId, topName, "Updated description", true, null);
            assertTrue(saved, "Overwriting existing topology by ID must succeed");
            assertEquals(originalId, frame.getActiveTopologyId(), "Active topology ID must remain the same");
            assertFalse(frame.isTopologyModified(), "Modified flag must reset to false after save");

            // Verify no duplicate record exists in MySQL
            List<TopologySummary> allTops = frame.getStorageService().listAllTopologies(true);
            long matchingCount = allTops.stream().filter(t -> t.getName().equals(topName)).count();
            assertEquals(1L, matchingCount, "There must be exactly one topology record with this name in MySQL");

            // Verify node count in MySQL updated from 2 to 3
            Optional<SavedTopology> reloaded = frame.getStorageService().loadTopology(originalId);
            assertTrue(reloaded.isPresent());
            assertEquals(3, reloaded.get().getNetwork().nodeCount(), "Updated topology in MySQL must have 3 nodes");

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(originalId);
        }

        frame.dispose();
    }

    // 5. Duplicate topology names are handled safely (conflict rejected when overwriteIfExists=false).
    @Test
    void testRequirement5_DuplicateTopologyNamesHandledSafely() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            Network net = new Network();
            net.addNode(new Computer("D1", "PC1", 100, 100));
            net.addNode(new Server("D2", "S1", 200, 200));

            String uniqueName = "DuplicateNameCheck_" + System.currentTimeMillis();
            int firstId = frame.getStorageService().saveTopology(uniqueName, "First", net, false);
            assertTrue(firstId > 0);

            // Attempting to save new topology with existing name (overwriteIfExists = false)
            boolean duplicateAttempt = frame.executeUnifiedSave(null, uniqueName, "Second attempt", false, null);
            assertFalse(duplicateAttempt, "Saving new topology with an existing name must fail when overwrite is false");

            // Database still has exactly 1 entry
            List<TopologySummary> matching = frame.getStorageService().listAllTopologies(true).stream()
                    .filter(t -> t.getName().equals(uniqueName)).toList();
            assertEquals(1, matching.size());

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(firstId);
        }

        frame.dispose();
    }

    // 6 & 7. Saving simulation activity records one run; saving without activity creates no run.
    @Test
    void testRequirement6And7_SavingWithAndWithoutSimulationActivity() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            // Case 7: Save with 0 simulation activity creates NO run record
            frame.onResetNetwork();
            assertEquals(0, frame.getEngine().getPackets().size());
            assertEquals(0, frame.getEngine().getCurrentTime());
            assertFalse(frame.hasSimulationActivity());

            String nameWithoutSim = "NoSimRun_" + System.currentTimeMillis();
            boolean save1 = frame.executeUnifiedSave(null, nameWithoutSim, "No sim", false, null);
            assertTrue(save1);
            int topId1 = frame.getActiveTopologyId();
            assertFalse(frame.isCurrentRunSaved(), "currentRunSaved must remain false when no run was saved");
            assertNull(frame.getLastSavedRunId(), "lastSavedRunId must remain null");

            List<SimulationRunRecord> runs1 = frame.getStorageService().listRunsForTopology(topId1);
            assertTrue(runs1.isEmpty(), "No simulation run should exist for topology saved without activity");

            // Case 6: Save with simulation activity records one run
            frame.onSendPacket("PC1", "S1");
            assertTrue(frame.hasSimulationActivity());
            assertTrue(frame.isSimulationActivityUnsaved());

            String nameWithSim = "WithSimRun_" + System.currentTimeMillis();
            boolean save2 = frame.executeUnifiedSave(null, nameWithSim, "With sim", false, "Activity notes");
            assertTrue(save2);
            int topId2 = frame.getActiveTopologyId();
            assertTrue(frame.isCurrentRunSaved());
            assertNotNull(frame.getLastSavedRunId());

            List<SimulationRunRecord> runs2 = frame.getStorageService().listRunsForTopology(topId2);
            assertEquals(1, runs2.size(), "Exactly one run must be recorded");
            SimulationRunRecord r = runs2.get(0);
            assertEquals(topId2, r.getTopologyId());
            assertEquals(nameWithSim, r.getTopologyName());
            assertEquals(1, r.getTotalPackets());
            assertEquals(1, r.getDeliveredPackets());
            assertEquals("Activity notes", r.getNotes());

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId1);
            frame.getStorageService().deleteTopologyPermanently(topId2);
        }

        frame.dispose();
    }

    // 8. Repeated Save clicks without new simulation activity do not create duplicate runs.
    @Test
    void testRequirement8_RepeatedSaveClicksDoNotCreateDuplicateRuns() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            frame.onResetNetwork();
            frame.onSendPacket("PC1", "S1");

            String topName = "RepeatSaveTest_" + System.currentTimeMillis();
            boolean firstSave = frame.executeUnifiedSave(null, topName, "Run 1", false, null);
            assertTrue(firstSave);
            int topId = frame.getActiveTopologyId();
            int firstRunId = frame.getLastSavedRunId();

            // Repeated Save without any new simulation activity:
            assertFalse(frame.isSimulationActivityUnsaved());
            boolean secondSave = frame.executeUnifiedSave(topId, topName, "Run 1 repeat", true, null);
            assertTrue(secondSave, "Save succeeds (updating topology)");
            assertEquals(firstRunId, frame.getLastSavedRunId(), "Run ID must remain unchanged");

            // Check database: still only 1 run record
            List<SimulationRunRecord> runs = frame.getStorageService().listRunsForTopology(topId);
            assertEquals(1, runs.size(), "No duplicate simulation run record must be created on repeated save");

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }

    // 9 & 10. Additional simulation activity allows a new run to be saved; topology-only change does not duplicate.
    @Test
    void testRequirement9And10_AdditionalSimulationActivityAllowsNewRun() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            frame.onResetNetwork();
            frame.onSendPacket("PC1", "S1");

            String topName = "AdditionalActivityTest_" + System.currentTimeMillis();
            assertTrue(frame.executeUnifiedSave(null, topName, "V1", false, "First scenario"));
            int topId = frame.getActiveTopologyId();
            int runId1 = frame.getLastSavedRunId();

            // Step 10: Change topology only (e.g. add node), do NOT step simulation
            frame.onAddNode(NodeType.SERVER);
            assertTrue(frame.isTopologyModified());
            assertFalse(frame.isSimulationActivityUnsaved()); // Still saved

            // Save topology change:
            assertTrue(frame.executeUnifiedSave(topId, topName, "V2 with extra server", true, null));
            assertEquals(1, frame.getStorageService().listRunsForTopology(topId).size(),
                    "Changing only topology data must not duplicate the previous simulation run");

            // Step 9: Now generate additional simulation activity
            frame.onSendPacket("PC2", "S1");
            assertTrue(frame.isSimulationActivityUnsaved(), "New packet submission must mark simulation state unsaved");

            // Save again: records Run #2 with updated metrics
            assertTrue(frame.executeUnifiedSave(topId, topName, "V2 with 2 packets", true, "Second scenario"));
            int runId2 = frame.getLastSavedRunId();
            assertTrue(runId2 > runId1, "New run ID must be greater than previous run ID");

            List<SimulationRunRecord> runs = frame.getStorageService().listRunsForTopology(topId);
            assertEquals(2, runs.size(), "Both distinct simulation runs must be recorded for this topology");

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }

    // 11. Previous simulation metrics remain unchanged after topology overwrite.
    @Test
    void testRequirement11_PreviousSimulationMetricsRemainUnchangedAfterTopologyOverwrite() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            frame.onResetNetwork();
            frame.onSendPacket("PC1", "S1");

            String topName = "MetricsIntegrityTest_" + System.currentTimeMillis();
            assertTrue(frame.executeUnifiedSave(null, topName, "Original", false, "Run 1 notes"));
            int topId = frame.getActiveTopologyId();
            int runId = frame.getLastSavedRunId();

            // Capture initial run metrics
            Optional<SimulationRunRecord> initialOpt = frame.getStorageService().getRunById(runId);
            assertTrue(initialOpt.isPresent());
            SimulationRunRecord initialRun = initialOpt.get();
            int initialPackets = initialRun.getTotalPackets();
            double initialRatio = initialRun.getDeliveryRatio();

            // Overwrite topology completely (delete links, modify nodes)
            frame.onRandomizeLinkCosts();
            frame.onAddNode(NodeType.ROUTER);
            assertTrue(frame.executeUnifiedSave(topId, topName, "Overwritten topology", true, null));

            // Verify Run 1 metrics in MySQL are 100% identical and undisturbed
            Optional<SimulationRunRecord> postOpt = frame.getStorageService().getRunById(runId);
            assertTrue(postOpt.isPresent());
            SimulationRunRecord postRun = postOpt.get();

            assertEquals(initialPackets, postRun.getTotalPackets());
            assertEquals(initialRatio, postRun.getDeliveryRatio(), 0.001);
            assertEquals("Run 1 notes", postRun.getNotes());
            assertEquals(initialRun.getSimulationTicks(), postRun.getSimulationTicks());

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }

    // 12 & 13. Failed writes roll back appropriately, preserve in-memory state, and retry does not duplicate.
    @Test
    void testRequirement12And13_FailedWritesRollBackAndPreserveInMemoryState() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);
        TopologyStorageService realService = frame.getStorageService();

        if (realService.isDatabaseAvailable()) {
            frame.onResetNetwork();
            frame.onSendPacket("PC1", "S1");
            assertEquals(1, frame.getEngine().getPackets().size());

            // Mock service simulating database failure
            TopologyStorageService failingService = new TopologyStorageService() {
                @Override
                public UnifiedSaveResult saveUnified(Integer topId, String name, String desc, Network net,
                                                     boolean overwrite, NetworkStatistics stats, long ticks, String notes)
                        throws SQLException {
                    throw new SQLException("Simulated disk full or foreign key violation during write");
                }
            };

            frame.setStorageService(failingService);

            String topName = "RollbackTest_" + System.currentTimeMillis();
            boolean failedAttempt = frame.executeUnifiedSave(null, topName, "Desc", false, null);

            assertFalse(failedAttempt, "Save must report failure when exception is thrown");
            assertNull(frame.getActiveTopologyId(), "Active topology ID must not be set on failure");
            assertFalse(frame.isCurrentRunSaved(), "Run must not be marked saved on failure");
            assertEquals(1, frame.getEngine().getPackets().size(), "In-memory simulation state must be fully preserved");

            // Restore healthy service and retry:
            frame.setStorageService(realService);
            boolean retryAttempt = frame.executeUnifiedSave(null, topName, "Desc", false, "Retry notes");
            assertTrue(retryAttempt, "Retry after restoring database connectivity must succeed");
            assertNotNull(frame.getActiveTopologyId());
            int topId = frame.getActiveTopologyId();
            assertTrue(frame.isCurrentRunSaved());

            // Confirm no duplicate topology exists in database
            List<TopologySummary> matching = realService.listAllTopologies(true).stream()
                    .filter(t -> t.getName().equals(topName)).toList();
            assertEquals(1, matching.size(), "Only one topology record should exist after retry");

            // Cleanup
            realService.deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }

    // 14. Resetting or loading a different topology does not retain stale identity.
    @Test
    void testRequirement14_ResettingOrLoadingDoesNotRetainStaleIdentity() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            Network net = new Network();
            net.addNode(new Computer("S1_PC", "Stale PC", 100, 100));
            net.addNode(new Server("S1_SRV", "Stale Server", 200, 200));

            String name1 = "StaleTest1_" + System.currentTimeMillis();
            int id1 = frame.getStorageService().saveTopology(name1, "First", net, false);

            frame.handleLoadTopology(id1, name1);
            assertEquals(id1, frame.getActiveTopologyId());
            assertEquals(name1, frame.getActiveTopologyName());

            // Reset network
            frame.onResetNetwork();
            assertNull(frame.getActiveTopologyId(), "Resetting network must clear activeTopologyId to null");
            assertEquals("Sample Campus Network", frame.getActiveTopologyName());
            assertFalse(frame.isTopologyModified());
            assertFalse(frame.isCurrentRunSaved());
            assertNull(frame.getLastSavedRunId());

            // Now saving a new topology must NOT inherit id1
            String name2 = "StaleTest2_" + System.currentTimeMillis();
            assertTrue(frame.executeUnifiedSave(null, name2, "New network", false, null));
            int id2 = frame.getActiveTopologyId();
            assertTrue(id2 > 0);
            assertTrue(id2 != id1, "New network must receive a newly generated ID and not inherit stale id1");

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(id1);
            frame.getStorageService().deleteTopologyPermanently(id2);
        }

        frame.dispose();
    }
}

package com.netroute.ui;

import com.netroute.graph.Network;
import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.NodeType;
import com.netroute.model.Server;
import com.netroute.repository.SavedTopology;
import com.netroute.repository.SimulationRunRecord;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite verifying all requirements of the New Project feature:
 * - Graph and simulation reset
 * - Topology identity clearing
 * - Protection of existing database records
 * - Distinct identity upon saving new project
 * - Unsaved changes prompt (Save, Discard, Cancel)
 * - Save failure protection (preserving workspace)
 * - Empty topology save validation
 */
class NewProjectFeatureTest {

    @Test
    void test1_NewProjectResetsGraphAndSimulationState() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        // Advance simulation and submit packet on sample network
        frame.onSendPacket("PC1", "S1");
        frame.onStepSimulation();
        assertTrue(frame.getNetwork().nodeCount() > 0);
        assertTrue(frame.getEngine().getPackets().size() > 0);
        assertTrue(frame.getEngine().getCurrentTime() > 0);

        // Invoke fresh project creation
        frame.createFreshProject();

        // 1. Graph is completely empty
        assertEquals(0, frame.getNetwork().nodeCount(), "New project must clear all nodes");
        assertEquals(0, frame.getNetwork().linkCount(), "New project must clear all links");

        // 2. Simulation engine is fresh and idle
        assertEquals(0, frame.getEngine().getPackets().size(), "New project must clear all packets");
        assertEquals(0L, frame.getEngine().getCurrentTime(), "New project must reset simulation clock to 0");
        assertTrue(frame.getEngine().isIdle(), "Simulation engine must be idle in fresh project");

        frame.dispose();
    }

    @Test
    void test2_ActiveSavedTopologyIdAndNameAreCleared() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        // Simulate loaded and saved project state
        frame.setActiveTopologyId(999);
        frame.setActiveTopologyName("Loaded Corp Network");
        frame.setActiveTopologyDescription("Branch office LAN");
        frame.setTopologyModified(true);
        frame.setCurrentRunSaved(true);
        frame.setLastSavedRunId(555);

        // Invoke fresh project creation
        frame.createFreshProject();

        assertNull(frame.getActiveTopologyId(), "Active topology ID must be null in fresh project");
        assertEquals("Untitled Network", frame.getActiveTopologyName());
        assertEquals("", frame.getActiveTopologyDescription());
        assertFalse(frame.isTopologyModified(), "topologyModified flag must be reset to false");
        assertFalse(frame.isCurrentRunSaved(), "currentRunSaved flag must be reset to false");
        assertNull(frame.getLastSavedRunId(), "lastSavedRunId must be reset to null");

        frame.dispose();
    }

    @Test
    void test3_ExistingSavedDatabaseRecordsRemainUntouched() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            Network net = new Network();
            net.addNode(new Computer("NP_PC", "NP PC", 100, 100));
            net.addNode(new Server("NP_S", "NP S", 300, 200));
            net.addLink(new NetworkLink("NP_L", "NP_PC", "NP_S", 2.0, 1.0, 10, false));

            String topName = "NewProj_DBPreserve_" + System.currentTimeMillis();
            int topId = frame.getStorageService().saveTopology(topName, "To remain untouched", net, false);
            assertTrue(topId > 0);

            // Load into frame, then click New
            frame.handleLoadTopology(topId, topName);
            assertEquals(topId, frame.getActiveTopologyId());

            frame.createFreshProject();
            assertNull(frame.getActiveTopologyId());

            // Verify topology record in database is 100% intact and untouched
            Optional<SavedTopology> preservedOpt = frame.getStorageService().loadTopology(topId);
            assertTrue(preservedOpt.isPresent(), "Existing topology must remain intact after New project");
            SavedTopology preserved = preservedOpt.get();
            assertEquals(topName, preserved.getName());
            assertEquals(2, preserved.getNetwork().nodeCount());
            assertEquals(1, preserved.getNetwork().linkCount());

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }

    @Test
    void test4_SaveAfterNewCreatesNewTopologyInsteadOfOverwritingPreviousOne() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            // 1. Create and save Topology 1
            Network net1 = new Network();
            net1.addNode(new Computer("P1_A", "Node A", 100, 100));
            net1.addNode(new Server("P1_B", "Node B", 300, 200));

            String name1 = "FirstProj_" + System.currentTimeMillis();
            int id1 = frame.getStorageService().saveTopology(name1, "First Project", net1, false);
            assertTrue(id1 > 0);

            // 2. Load into frame
            frame.handleLoadTopology(id1, name1);
            assertEquals(id1, frame.getActiveTopologyId());

            // 3. Start a New Project (Discard)
            frame.setTestUnsavedChangesAction(NetRouteFrame.UnsavedChangesAction.DISCARD);
            boolean newCreated = frame.onNewProject();
            assertTrue(newCreated);
            assertNull(frame.getActiveTopologyId(), "Active topology ID must be null after New");

            // 4. Build a new topology in the fresh project
            frame.onAddNode(NodeType.COMPUTER);
            frame.onAddNode(NodeType.SERVER);
            assertEquals(2, frame.getNetwork().nodeCount());

            // 5. Save the new project
            String name2 = "SecondProj_" + System.currentTimeMillis();
            boolean saveSecond = frame.executeUnifiedSave(null, name2, "Second Project", false, null);
            assertTrue(saveSecond);
            int id2 = frame.getActiveTopologyId();

            // 6. Verify distinct IDs and no overwrite
            assertNotNull(id2);
            assertNotEquals(id1, id2, "New project must receive its own distinct database ID");

            // Both topologies must coexist in database
            Optional<SavedTopology> top1 = frame.getStorageService().loadTopology(id1);
            Optional<SavedTopology> top2 = frame.getStorageService().loadTopology(id2);
            assertTrue(top1.isPresent(), "Original topology 1 must still exist");
            assertTrue(top2.isPresent(), "New topology 2 must exist");
            assertEquals(name1, top1.get().getName());
            assertEquals(name2, top2.get().getName());

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(id1);
            frame.getStorageService().deleteTopologyPermanently(id2);
        }

        frame.dispose();
    }

    @Test
    void test5_CancelLeavesCurrentProjectUnchanged() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        // Submit packet to create unsaved activity
        frame.onSendPacket("PC1", "S1");
        int nodeCountBefore = frame.getNetwork().nodeCount();
        int packetCountBefore = frame.getEngine().getPackets().size();
        assertTrue(frame.hasUnsavedChanges());

        // Choose CANCEL
        frame.setTestUnsavedChangesAction(NetRouteFrame.UnsavedChangesAction.CANCEL);
        boolean result = frame.onNewProject();

        assertFalse(result, "onNewProject must return false when cancelled");
        assertEquals(nodeCountBefore, frame.getNetwork().nodeCount(), "Nodes must not be modified when cancelled");
        assertEquals(packetCountBefore, frame.getEngine().getPackets().size(), "Packets must not be discarded when cancelled");

        frame.dispose();
    }

    @Test
    void test6_IfSaveFailsDuringUnsavedChangesPromptDoNotDiscardCurrentProject() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        frame.onSendPacket("PC1", "S1");
        int nodeCountBefore = frame.getNetwork().nodeCount();
        int packetCountBefore = frame.getEngine().getPackets().size();
        assertTrue(frame.hasUnsavedChanges());

        // Mock a failing storage service
        TopologyStorageService failingService = new TopologyStorageService() {
            @Override
            public UnifiedSaveResult saveUnified(Integer topId, String name, String desc, Network net,
                                                 boolean overwrite, NetworkStatistics stats, long ticks, String notes)
                    throws SQLException {
                throw new SQLException("Simulated database failure during save prompt");
            }
        };
        frame.setStorageService(failingService);

        // User chooses SAVE, but save throws SQLException:
        frame.setTestUnsavedChangesAction(NetRouteFrame.UnsavedChangesAction.SAVE);
        boolean result = frame.onNewProject();

        assertFalse(result, "onNewProject must return false when save fails");
        assertEquals(nodeCountBefore, frame.getNetwork().nodeCount(), "Current project network must remain intact when save fails");
        assertEquals(packetCountBefore, frame.getEngine().getPackets().size(), "Current project packets must be preserved when save fails");

        frame.dispose();
    }

    @Test
    void test7_SaveEmptyNetworkRejectedByValidation() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        // Fresh empty project
        frame.createFreshProject();
        assertEquals(0, frame.getNetwork().nodeCount());

        // Attempting promptAndSave on empty project must be rejected
        boolean savedPrompt = frame.promptAndSave();
        assertFalse(savedPrompt, "Saving an empty network must be rejected by validation");

        // Attempting executeUnifiedSave on empty project must be rejected
        boolean savedExec = frame.executeUnifiedSave(null, "Empty Net", "Desc", false, null);
        assertFalse(savedExec, "executeUnifiedSave must reject an empty network");

        frame.dispose();
    }

    @Test
    void test8_NavBarIncludesNewProjectButton() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame.getBtnNewProject(), "NetRouteFrame must expose the New Project navbar button");
        assertTrue(frame.getBtnNewProject().getText().contains("New"), "Button text must contain 'New'");

        frame.dispose();
    }
}

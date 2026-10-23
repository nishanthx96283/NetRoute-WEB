package com.netroute.ui;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.NodeType;
import com.netroute.model.Route;
import com.netroute.repository.SavedTopology;
import com.netroute.repository.SimulationRunRecord;
import com.netroute.routing.DijkstraRouter;
import com.netroute.service.TopologyStorageService;
import com.netroute.simulation.SimulationEngine;
import com.netroute.statistics.NetworkStatistics;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetRouteFrameTest {

    @Test
    void topologyPanelInitializesWithNetwork() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        Network network = GraphManager.createSampleNetwork().getNetwork();
        TopologyPanel panel = new TopologyPanel(network);
        assertNotNull(panel);

        DijkstraRouter router = new DijkstraRouter();
        Optional<Route> route = router.findRoute(network, "PC1", "S1");
        assertTrue(route.isPresent());

        panel.setHighlightedRoute(route.get());
        panel.clearHighlightedRoute();
    }

    @Test
    void statisticsPanelUpdatesFromNetworkStatistics() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        Network network = GraphManager.createSampleNetwork().getNetwork();
        SimulationEngine engine = new SimulationEngine(network);
        NetworkStatistics stats = new NetworkStatistics(engine);

        StatisticsPanel panel = new StatisticsPanel();
        panel.updateStatistics(stats);
        assertNotNull(panel);
    }

    @Test
    void controlPanelPopulatesFromNetwork() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        Network network = GraphManager.createSampleNetwork().getNetwork();
        ControlPanel panel = new ControlPanel();
        panel.populate(network);
        assertNotNull(panel);
    }

    @Test
    void netRouteFrameIntegratesRoutingAndSimulation() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        // Test route calculation callback
        frame.onFindRoute("PC1", "S1");

        // Test packet sending callback
        frame.onSendPacket("PC1", "S1");

        // Test link failure and recovery
        frame.onFailLink("L4");
        frame.onRestoreLink("L4");

        // Test simulation step
        frame.onStepSimulation();

        // Test reset
        frame.onResetNetwork();

        frame.dispose();
    }

    @Test
    void netRouteFrameHandlesRandomizeLinkCostsCallback() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        frame.onFindRoute("PC1", "S1");
        frame.onRandomizeLinkCosts();

        frame.dispose();
    }

    @Test
    void netRouteFrameHandlesDynamicNodeAndLinkCreation() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        // Add dynamic node PC4
        frame.onAddNode(NodeType.COMPUTER);

        // Connect PC4 to R1
        frame.onConnectNodes("PC4", "R1", 2.5);

        // Calculate route from newly added node
        frame.onFindRoute("PC4", "S1");

        frame.dispose();
    }

    @Test
    void netRouteFrameHandlesNodeAndLinkDeletion() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        // Find initial route
        frame.onFindRoute("PC1", "S1");

        // Delete link L5 (R1-S1), should recalculate route via R2
        frame.onDeleteLink("L5");

        // Delete node PC1, should clear route and endpoints
        frame.onDeleteNode("PC1");

        // Attempting to delete non-existent node or link should log error without throwing
        frame.onDeleteNode("NONEXISTENT");
        frame.onDeleteLink("NONEXISTENT");

        frame.dispose();
    }

    @Test
    void netRouteFrameNavigatesBetweenLandingAndSimulator() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        assertEquals(NetRouteFrame.CARD_LANDING, frame.getCurrentCard());

        frame.showSimulatorPage();
        assertEquals(NetRouteFrame.CARD_SIMULATOR, frame.getCurrentCard());

        frame.showLandingPage();
        assertEquals(NetRouteFrame.CARD_LANDING, frame.getCurrentCard());

        frame.onLaunchSimulator();
        assertEquals(NetRouteFrame.CARD_SIMULATOR, frame.getCurrentCard());

        frame.dispose();
    }

    @Test
    void netRouteFrameNavigatesToTechStackAndAbout() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        frame.showTechStackPage();
        assertEquals(NetRouteFrame.CARD_TECH_STACK, frame.getCurrentCard());

        frame.showAboutPage();
        assertEquals(NetRouteFrame.CARD_ABOUT, frame.getCurrentCard());

        frame.onShowTechStack();
        assertEquals(NetRouteFrame.CARD_TECH_STACK, frame.getCurrentCard());

        frame.onShowAbout();
        assertEquals(NetRouteFrame.CARD_ABOUT, frame.getCurrentCard());

        frame.showLandingPage();
        assertEquals(NetRouteFrame.CARD_LANDING, frame.getCurrentCard());

        frame.dispose();
    }

    @Test
    void landingPageInitializesCorrectly() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        LandingPage.Listener listener = new LandingPage.Listener() {
            @Override
            public void onLaunchSimulator() {}
            @Override
            public void onShowTechStack() {}
            @Override
            public void onShowAbout() {}
            @Override
            public void onShowHistory() {}
        };
        LandingPage page = new LandingPage(listener);
        assertNotNull(page);
        page.setSize(1280, 800);
        page.doLayout();

        // Recursively inspect text components on LandingPage to confirm Database exists,
        // Interactive Workspace is removed, and Planned Enhancement badge is removed
        boolean foundDatabase = false;
        boolean foundInteractiveWorkspace = false;
        boolean foundPlannedBadge = false;
        java.util.Queue<java.awt.Component> queue = new java.util.LinkedList<>();
        queue.add(page);
        while (!queue.isEmpty()) {
            java.awt.Component c = queue.poll();
            if (c instanceof javax.swing.JLabel) {
                String text = ((javax.swing.JLabel) c).getText();
                if (text != null) {
                    if (text.contains("Database")) foundDatabase = true;
                    if (text.contains("Interactive Workspace")) foundInteractiveWorkspace = true;
                    if (text.contains("Planned Enhancement")) foundPlannedBadge = true;
                }
            } else if (c instanceof javax.swing.JButton) {
                String text = ((javax.swing.JButton) c).getText();
                if (text != null && text.contains("Workspace")) foundInteractiveWorkspace = true;
            }
            if (c instanceof java.awt.Container) {
                for (java.awt.Component child : ((java.awt.Container) c).getComponents()) {
                    queue.add(child);
                }
            }
        }
        assertTrue(foundDatabase, "LandingPage must contain the Database card");
        assertFalse(foundInteractiveWorkspace, "LandingPage must not contain Interactive Workspace card");
        assertFalse(foundPlannedBadge, "LandingPage must not contain Planned Enhancement badge");
    }

    @Test
    void techStackAndAboutPagesInitializeCorrectly() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        TechStackPage.NavigationHandler techNav = new TechStackPage.NavigationHandler() {
            @Override
            public void onBackToHome() {}
            @Override
            public void onLaunchSimulator() {}
        };
        TechStackPage techPage = new TechStackPage(techNav);
        assertNotNull(techPage);

        AboutPage.NavigationHandler aboutNav = new AboutPage.NavigationHandler() {
            @Override
            public void onBackToHome() {}
            @Override
            public void onLaunchSimulator() {}
        };
        AboutPage aboutPage = new AboutPage(aboutNav);
        assertNotNull(aboutPage);
        aboutPage.setSize(1280, 800);
        aboutPage.doLayout();

        // Verify top-level structure: North header and Center scroll pane
        assertEquals(2, aboutPage.getComponentCount());
        assertTrue(aboutPage.getComponent(1) instanceof javax.swing.JScrollPane);
        javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane) aboutPage.getComponent(1);
        javax.swing.JPanel scrollContent = (javax.swing.JPanel) scrollPane.getViewport().getView();
        assertNotNull(scrollContent);

        // Verify all direct children of scrollContent share consistent left alignment
        for (int i = 0; i < scrollContent.getComponentCount(); i++) {
            java.awt.Component child = scrollContent.getComponent(i);
            assertEquals(java.awt.Component.LEFT_ALIGNMENT, child.getAlignmentX(),
                    "Child at index " + i + " must have LEFT_ALIGNMENT");
        }
    }

    @Test
    void controlPanelIncludesSaveTopologyButton() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ControlPanel panel = new ControlPanel();
        assertNotNull(panel.getBtnSave(), "ControlPanel must have Save button");
        assertTrue(panel.getBtnSave().getText().contains("Save"));

        boolean[] called = {false};
        panel.setListener(new ControlPanel.Listener() {
            @Override public void onFindRoute(String src, String dst) {}
            @Override public void onSendPacket(String src, String dst) {}
            @Override public void onStepSimulation() {}
            @Override public void onFailLink(String linkId) {}
            @Override public void onRestoreLink(String linkId) {}
            @Override public void onResetNetwork() {}
            @Override public void onRandomizeLinkCosts() {}
            @Override public void onAddNode(NodeType type) {}
            @Override public void onConnectNodes(String a, String b, double cost) {}
            @Override public void onDeleteNode(String nodeId) {}
            @Override public void onDeleteLink(String linkId) {}
            @Override public void onSave() { called[0] = true; }
        });

        panel.triggerSave();
        assertTrue(called[0], "triggerSave() must invoke onSave on listener");
    }

    @Test
    void netRouteFrameExecutesSaveTopologyAndHandlesDuplicates() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            String testName = "FrameSaveTest_" + System.currentTimeMillis();

            // 1. Initial save
            boolean saved = frame.executeSaveTopology(testName, "Frame Test Desc", false);
            assertTrue(saved, "Initial save should succeed");

            // 2. Duplicate detection without overwrite
            boolean duplicateAttempt = frame.executeSaveTopology(testName, "Duplicate Desc", false);
            assertFalse(duplicateAttempt, "Duplicate name save without overwrite should be rejected");

            // 3. Overwrite existing
            boolean overwritten = frame.executeSaveTopology(testName, "Overwritten Desc", true);
            assertTrue(overwritten, "Save with overwrite=true should succeed");

            // Cleanup
            try {
                java.util.Optional<com.netroute.repository.SavedTopology> top =
                        frame.getStorageService().loadTopologyByName(testName);
                if (top.isPresent()) {
                    frame.getStorageService().deleteTopologyPermanently(top.get().getId());
                }
            } catch (Exception ignored) {
            }
        }

        frame.dispose();
    }

    @Test
    void historyPageNavigationSwitchesCurrentCard() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        assertNotNull(frame.getHistoryPage(), "Frame must initialize HistoryPage");
        frame.showHistoryPage();
        assertEquals(NetRouteFrame.CARD_HISTORY, frame.getCurrentCard());

        frame.onShowHistory();
        assertEquals(NetRouteFrame.CARD_HISTORY, frame.getCurrentCard());

        frame.showLandingPage();
        assertEquals(NetRouteFrame.CARD_LANDING, frame.getCurrentCard());

        frame.dispose();
    }

    @Test
    void netRouteFrameLoadsTopologyAndResetsSimulation() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            // Build a distinct custom network with 3 nodes
            Network customNet = new Network();
            customNet.addNode(new com.netroute.model.Computer("C10", "Host 10", 100, 100));
            customNet.addNode(new com.netroute.model.Router("R10", "Router 10", 250, 150));
            customNet.addNode(new com.netroute.model.Server("S10", "Server 10", 400, 200));
            customNet.addLink(new com.netroute.model.NetworkLink("L10A", "C10", "R10", 3.0, 5.0, 10, false));
            customNet.addLink(new com.netroute.model.NetworkLink("L10B", "R10", "S10", 2.0, 4.0, 10, false));

            String testName = "LoadTest_" + System.currentTimeMillis();
            int topId = frame.getStorageService().saveTopology(testName, "Custom test topology", customNet, false);
            assertTrue(topId > 0);

            // Initially frame has 6 sample nodes
            assertEquals(6, frame.getNetwork().nodeCount());

            // Load the saved topology into simulator
            boolean loaded = frame.handleLoadTopology(topId, testName);
            assertTrue(loaded, "handleLoadTopology should return true");

            // Verify network in simulator was replaced
            assertEquals(NetRouteFrame.CARD_SIMULATOR, frame.getCurrentCard());
            assertEquals(3, frame.getNetwork().nodeCount());
            assertEquals(2, frame.getNetwork().linkCount());
            assertTrue(frame.getNetwork().findNode("C10").isPresent());
            assertTrue(frame.getNetwork().findNode("R10").isPresent());
            assertTrue(frame.getNetwork().findNode("S10").isPresent());
            assertFalse(frame.getNetwork().findNode("PC1").isPresent());

            // Verify clean engine reset (no stale packets)
            assertEquals(0, frame.getEngine().getSentCount());
            assertTrue(frame.getEngine().getPackets().isEmpty());

            // Verify routing still works on loaded network
            Optional<Route> routeOpt = frame.getRouter().findRoute(frame.getNetwork(), "C10", "S10");
            assertTrue(routeOpt.isPresent());
            assertEquals(5.0, routeOpt.get().getTotalCost(), 0.001);

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }

    @Test
    void netRouteFrameDeletesTopologyAndPreservesSimulationRuns() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            Network tempNet = new Network();
            tempNet.addNode(new com.netroute.model.Computer("D_PC1", "Delete PC", 150, 150));
            tempNet.addNode(new com.netroute.model.Server("D_S1", "Delete Server", 350, 250));
            tempNet.addLink(new com.netroute.model.NetworkLink("D_L1", "D_PC1", "D_S1", 1.0, 2.0, 10, false));

            String testName = "DeleteTest_" + System.currentTimeMillis();
            int topId = frame.getStorageService().saveTopology(testName, "Topology to delete", tempNet, false);

            // Record a simulation run against this topology
            SimulationEngine sim = new SimulationEngine(tempNet);
            sim.submitPacket("D_PC1", "D_S1", 1);
            sim.run();
            NetworkStatistics stats = new NetworkStatistics(sim);
            int runId = frame.getStorageService().recordSimulationRun(
                    topId,
                    testName,
                    stats,
                    10L,
                    "Pre-deletion simulation run"
            );
            assertTrue(runId > 0);

            // Delete topology via NetRouteFrame
            boolean deleted = frame.handleDeleteTopology(topId, testName);
            assertTrue(deleted, "handleDeleteTopology should return true");

            // Verify topology is deleted from topologies table
            Optional<SavedTopology> loaded = frame.getStorageService().loadTopology(topId);
            assertTrue(loaded.isEmpty(), "Deleted topology should no longer exist in database");

            // Verify history retention: simulation run still exists!
            List<SimulationRunRecord> recentRuns = frame.getStorageService().listRecentRuns(20);
            boolean foundRun = false;
            for (SimulationRunRecord r : recentRuns) {
                if (r.getId() == runId) {
                    foundRun = true;
                    // Topology name was denormalized and retained
                    assertEquals(testName, r.getTopologyName());
                    // Foreign key is now NULL because ON DELETE SET NULL was configured
                    assertNull(r.getTopologyId(), "Run topology_id must be null after topology deletion");
                    break;
                }
            }
            assertTrue(foundRun, "Simulation run record must be retained in simulation_runs table after topology deletion");
        }

        frame.dispose();
    }

    @Test
    void controlPanelIncludesFinishAndSaveRunButton() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ControlPanel panel = new ControlPanel();
        assertNotNull(panel.getBtnFinishRun(), "ControlPanel must provide backward-compatible getBtnFinishRun()");
        assertTrue(panel.getBtnFinishRun().getText().contains("Save"));

        boolean[] called = {false};
        panel.setListener(new ControlPanel.Listener() {
            @Override public void onFindRoute(String src, String dst) {}
            @Override public void onSendPacket(String src, String dst) {}
            @Override public void onStepSimulation() {}
            @Override public void onFailLink(String linkId) {}
            @Override public void onRestoreLink(String linkId) {}
            @Override public void onResetNetwork() {}
            @Override public void onRandomizeLinkCosts() {}
            @Override public void onAddNode(NodeType type) {}
            @Override public void onConnectNodes(String a, String b, double cost) {}
            @Override public void onDeleteNode(String nodeId) {}
            @Override public void onDeleteLink(String linkId) {}
            @Override public void onSave() { called[0] = true; }
        });

        panel.triggerFinishAndSaveRun();
        assertTrue(called[0], "triggerFinishAndSaveRun() must delegate to save action on listener");
    }

    @Test
    void netRouteFrameRecordsCompletedSimulationRunForSavedTopology() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            // 1. Build and save a custom 3-node network
            Network testNet = new Network();
            testNet.addNode(new com.netroute.model.Computer("RC_PC1", "Run PC", 100, 100));
            testNet.addNode(new com.netroute.model.Router("RC_R1", "Run Router", 250, 150));
            testNet.addNode(new com.netroute.model.Server("RC_S1", "Run Server", 400, 200));
            testNet.addLink(new com.netroute.model.NetworkLink("RC_L1", "RC_PC1", "RC_R1", 2.0, 3.0, 10, false));
            testNet.addLink(new com.netroute.model.NetworkLink("RC_L2", "RC_R1", "RC_S1", 1.0, 2.0, 10, false));

            String topName = "RunTest_Saved_" + System.currentTimeMillis();
            int topId = frame.getStorageService().saveTopology(topName, "Run test topology", testNet, false);
            assertTrue(topId > 0);

            // 2. Load into simulator
            boolean loaded = frame.handleLoadTopology(topId, topName);
            assertTrue(loaded);
            assertEquals(topId, frame.getActiveTopologyId());
            assertEquals(topName, frame.getActiveTopologyName());

            // 3. Execute simulation events
            frame.onSendPacket("RC_PC1", "RC_S1");
            frame.onSendPacket("RC_PC1", "RC_S1");
            frame.onFailLink("RC_L2");
            frame.onStepSimulation();

            // 4. Finish & Save Run
            String notes = "Automated test scenario with 2 packets and 1 failure";
            boolean savedRun = frame.executeFinishAndSaveRun(notes);
            assertTrue(savedRun, "executeFinishAndSaveRun should succeed");
            assertTrue(frame.isCurrentRunSaved(), "currentRunSaved must be true after saving");
            assertNotNull(frame.getLastSavedRunId(), "lastSavedRunId must be non-null");

            // 5. Query MySQL to verify exact persisted metrics
            int runId = frame.getLastSavedRunId();
            Optional<SimulationRunRecord> runOpt = frame.getStorageService().getRunById(runId);
            assertTrue(runOpt.isPresent());
            SimulationRunRecord record = runOpt.get();

            assertEquals(topId, record.getTopologyId());
            assertEquals(topName, record.getTopologyName());
            assertEquals(2, record.getTotalPackets());
            assertEquals(2, record.getDeliveredPackets());
            assertEquals(0, record.getFailedPackets());
            assertEquals(0, record.getDroppedPackets());
            assertEquals(100.0, record.getDeliveryRatio(), 0.001);
            assertTrue(record.getAvgDeliveryDelay() > 0.0);
            assertEquals(1, record.getLinkFailures());
            assertEquals(notes, record.getNotes());
            assertTrue(record.getSimulationTicks() > 0);

            // 6. Verify in-memory statistics are preserved
            assertEquals(2, frame.getEngine().getPackets().size());

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }

    @Test
    void netRouteFrameHandlesUnsavedTopologyCorrectly() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            // Default network is unsaved
            frame.onResetNetwork();
            assertNull(frame.getActiveTopologyId(), "Active topology ID must be null for unsaved network");
            assertEquals("Sample Campus Network", frame.getActiveTopologyName());

            // Submit packet
            frame.onSendPacket("PC1", "S1");
            assertEquals(1, frame.getEngine().getPackets().size());

            // Finish & Save run
            boolean saved = frame.executeFinishAndSaveRun("Unsaved run test");
            assertTrue(saved);
            assertTrue(frame.isCurrentRunSaved());
            assertNotNull(frame.getLastSavedRunId());

            // Verify run in database has null topology_id and accurate name snapshot
            Optional<SimulationRunRecord> runOpt = frame.getStorageService().getRunById(frame.getLastSavedRunId());
            assertTrue(runOpt.isPresent());
            SimulationRunRecord record = runOpt.get();

            assertNull(record.getTopologyId(), "Unsaved network run must store NULL topology_id");
            assertEquals("Sample Campus Network", record.getTopologyName());
            assertEquals(1, record.getTotalPackets());
            assertEquals("Unsaved run test", record.getNotes());
        }

        frame.dispose();
    }

    @Test
    void netRouteFramePreventsDuplicateRunSaves() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            frame.onResetNetwork();
            frame.onSendPacket("PC1", "S1");

            // First save: succeeds
            boolean firstSave = frame.executeFinishAndSaveRun("First save");
            assertTrue(firstSave);
            assertTrue(frame.isCurrentRunSaved());
            int runId1 = frame.getLastSavedRunId();

            // Second save without new simulation events: blocked!
            boolean duplicateSave = frame.executeFinishAndSaveRun("Duplicate attempt");
            assertFalse(duplicateSave, "Repeated save without new events must be rejected");

            boolean duplicatePrompt = frame.promptAndSaveSimulationRun("Prompt duplicate attempt");
            assertFalse(duplicatePrompt, "Prompt save without new events must be rejected");

            // Generate new simulation activity
            frame.onSendPacket("PC2", "S1");
            assertFalse(frame.isCurrentRunSaved(), "currentRunSaved must reset to false upon new packet submission");

            // Now save succeeds for updated state
            boolean thirdSave = frame.executeFinishAndSaveRun("Extended run save");
            assertTrue(thirdSave);
            assertTrue(frame.getLastSavedRunId() > runId1);
        }

        frame.dispose();
    }

    @Test
    void netRouteFrameAllowsRetryAfterFailedDatabaseWrite() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        TopologyStorageService realService = frame.getStorageService();
        if (realService.isDatabaseAvailable()) {
            frame.onResetNetwork();
            frame.onSendPacket("PC1", "S1");

            // Subclass that simulates MySQL failure
            TopologyStorageService failingService = new TopologyStorageService() {
                private boolean failing = true;

                @Override
                public int recordSimulationRun(Integer topId, String topName, NetworkStatistics stats, long ticks, String notes) throws java.sql.SQLException {
                    if (failing) {
                        throw new java.sql.SQLException("Simulated database outage");
                    }
                    return realService.recordSimulationRun(topId, topName, stats, ticks, notes);
                }
            };

            frame.setStorageService(failingService);

            // Attempt save during outage: fails cleanly
            boolean failedAttempt = frame.executeFinishAndSaveRun("Failing attempt");
            assertFalse(failedAttempt, "Save should fail when database throws SQLException");
            assertFalse(frame.isCurrentRunSaved(), "currentRunSaved must remain false on failure");
            assertEquals(1, frame.getEngine().getPackets().size(), "In-memory packets must be preserved on failure");

            // Restore connection and retry: succeeds
            frame.setStorageService(realService);
            boolean retryAttempt = frame.executeFinishAndSaveRun("Successful retry");
            assertTrue(retryAttempt, "Retry must succeed after database connection is restored");
            assertTrue(frame.isCurrentRunSaved());
            assertNotNull(frame.getLastSavedRunId());
        }

        frame.dispose();
    }

    @Test
    void resettingOrLoadingTopologyDoesNotCreateDuplicateOrAccidentalRun() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        // 1. Initial state without simulation activity cannot be saved
        boolean emptySave = frame.executeFinishAndSaveRun("Blank");
        assertFalse(emptySave, "Empty simulation with 0 packets and 0 ticks must not be saved");

        // 2. Submit packet, then reset network
        frame.onSendPacket("PC1", "S1");
        frame.onResetNetwork();

        // 3. Clean network must not auto-save anything and cannot be saved with 0 activity
        assertFalse(frame.isCurrentRunSaved());
        assertEquals(0, frame.getEngine().getPackets().size());
        boolean afterResetSave = frame.executeFinishAndSaveRun("After reset");
        assertFalse(afterResetSave, "Fresh network after reset cannot be saved with 0 activity");

        frame.dispose();
    }

    @Test
    void modifiedTopologyPreservesSavedTopologyIdAndMarksModified() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        assertNotNull(frame);
        frame.setSkipConfirmationForTesting(true);

        if (frame.getStorageService().isDatabaseAvailable()) {
            Network net = new Network();
            net.addNode(new com.netroute.model.Computer("MC1", "Mod PC", 100, 100));
            net.addNode(new com.netroute.model.Server("MS1", "Mod Server", 300, 200));
            net.addLink(new com.netroute.model.NetworkLink("ML1", "MC1", "MS1", 1.0, 2.0, 10, false));

            String topName = "ModTest_" + System.currentTimeMillis();
            int topId = frame.getStorageService().saveTopology(topName, "To be modified", net, false);

            frame.handleLoadTopology(topId, topName);
            assertEquals(topId, frame.getActiveTopologyId());
            assertFalse(frame.isTopologyModified());

            // Modify topology dynamically by adding a node
            frame.onAddNode(NodeType.COMPUTER);
            assertEquals(topId, frame.getActiveTopologyId(), "Active topology ID must be preserved when loaded topology is modified");
            assertTrue(frame.isTopologyModified(), "Topology must be marked modified");

            // Run simulation on modified topology
            frame.onSendPacket("MC1", "MS1");
            boolean saved = frame.executeFinishAndSaveRun("Run on modified network");
            assertTrue(saved);

            Optional<SimulationRunRecord> runOpt = frame.getStorageService().getRunById(frame.getLastSavedRunId());
            assertTrue(runOpt.isPresent());
            assertEquals(topId, runOpt.get().getTopologyId(), "Run must be associated with active topology ID");

            // Cleanup
            frame.getStorageService().deleteTopologyPermanently(topId);
        }

        frame.dispose();
    }
}

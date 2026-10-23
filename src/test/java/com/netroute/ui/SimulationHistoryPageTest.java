package com.netroute.ui;

import com.netroute.graph.Network;
import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.Router;
import com.netroute.model.Server;
import com.netroute.repository.DatabaseConnection;
import com.netroute.repository.SimulationRunRecord;
import com.netroute.service.TopologyStorageService;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationHistoryPageTest {

    private static class StubStorageService extends TopologyStorageService {
        private boolean available = true;
        private final List<SimulationRunRecord> runs = new ArrayList<>();
        private int refreshCallCount = 0;

        public StubStorageService() {
            super(new DatabaseConnection());
        }

        @Override
        public boolean isDatabaseAvailable() {
            return available;
        }

        @Override
        public List<SimulationRunRecord> listAllSimulationRuns() throws SQLException {
            refreshCallCount++;
            if (!available) {
                throw new SQLException("Simulated connection failure to netroute_db");
            }
            return new ArrayList<>(runs);
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public void addRun(SimulationRunRecord run) {
            runs.add(run);
        }

        public void clear() {
            runs.clear();
        }

        public int getRefreshCallCount() {
            return refreshCallCount;
        }
    }

    private static class RecordingListener implements SimulationHistoryPage.Listener {
        boolean homeClicked = false;
        boolean launchClicked = false;

        @Override
        public void onBackToHome() {
            homeClicked = true;
        }

        @Override
        public void onLaunchSimulator() {
            launchClicked = true;
        }
    }

    @Test
    void testOfflineStateDisplaysWarningWhenDatabaseUnavailable() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(false);
        RecordingListener listener = new RecordingListener();

        SimulationHistoryPage page = new SimulationHistoryPage(service, listener);
        assertNotNull(page);
        assertEquals(0, page.getDisplayedRowCount());

        boolean foundOffline = searchComponentTree(page, "Database Unavailable");
        assertTrue(foundOffline, "Must display Database Unavailable warning");
    }

    @Test
    void testEmptyStateDisplaysFriendlyMessageWhenNoRunsRecorded() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        service.clear();
        RecordingListener listener = new RecordingListener();

        SimulationHistoryPage page = new SimulationHistoryPage(service, listener);
        assertNotNull(page);
        assertEquals(0, page.getDisplayedRowCount());

        boolean foundEmpty = searchComponentTree(page, "No Simulation Runs Found");
        assertTrue(foundEmpty, "Must display empty state banner when runs list is empty");
    }

    @Test
    void testRendersRunsTableWithMetricsAndNullTopologyIdHandling() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        LocalDateTime now = LocalDateTime.now();

        // Run 1: Linked to topology 101, 100% delivery
        service.addRun(new SimulationRunRecord(
                1, 101, "Campus Backbone Alpha", now.minusMinutes(30),
                120, 20, 20, 0, 0, 100.0, 3.45, 0, 0, "All packets delivered successfully."
        ));

        // Run 2: Unlinked (null topology_id), 80% delivery with failures
        service.addRun(new SimulationRunRecord(
                2, null, "Deleted Mesh Topology", now.minusMinutes(10),
                250, 15, 12, 2, 1, 80.0, 5.20, 1, 2, "Link failed during transmission."
        ));

        RecordingListener listener = new RecordingListener();
        SimulationHistoryPage page = new SimulationHistoryPage(service, listener);

        assertEquals(2, page.getDisplayedRowCount());
        List<SimulationRunRecord> runs = page.getDisplayedRuns();
        assertEquals(2, runs.size());
        assertEquals("Campus Backbone Alpha", runs.get(0).getTopologyName());
        assertEquals(Integer.valueOf(101), runs.get(0).getTopologyId());

        assertEquals("Deleted Mesh Topology", runs.get(1).getTopologyName());
        assertNull(runs.get(1).getTopologyId(), "Run 2 must have null topology_id");

        // Verify table model columns
        JTable table = page.getTable();
        assertEquals(13, table.getColumnCount());
        assertEquals("#1", table.getValueAt(0, 0));
        assertEquals("Campus Backbone Alpha", table.getValueAt(0, 1));
        assertEquals("#101", table.getValueAt(0, 2));
        assertEquals(120L, table.getValueAt(0, 4));
        assertEquals(20, table.getValueAt(0, 5));
        assertEquals(20, table.getValueAt(0, 6));
        assertEquals(0, table.getValueAt(0, 7));
        assertEquals(0, table.getValueAt(0, 8));
        assertEquals("100.0%", table.getValueAt(0, 9));
        assertEquals("3.45", table.getValueAt(0, 10));

        // Row 2 values (null topology ID rendered as — (Deleted))
        assertEquals("#2", table.getValueAt(1, 0));
        assertEquals("Deleted Mesh Topology", table.getValueAt(1, 1));
        assertEquals("— (Deleted)", table.getValueAt(1, 2));
        assertEquals("80.0%", table.getValueAt(1, 9));
    }

    @Test
    void testSearchFilterBehavior() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        LocalDateTime now = LocalDateTime.now();

        service.addRun(new SimulationRunRecord(1, 10, "Datacenter Spine", now, 50, 10, 10, 0, 0, 100.0, 2.0, 0, 0, ""));
        service.addRun(new SimulationRunRecord(2, 20, "Campus Ring", now, 60, 10, 9, 1, 0, 90.0, 3.0, 1, 1, ""));
        service.addRun(new SimulationRunRecord(3, 30, "Datacenter Leaf", now, 70, 10, 8, 2, 0, 80.0, 4.0, 0, 0, ""));

        RecordingListener listener = new RecordingListener();
        SimulationHistoryPage page = new SimulationHistoryPage(service, listener);
        assertEquals(3, page.getDisplayedRowCount());

        // 1. Filter by substring of topology name: "Datacenter"
        page.filter("Datacenter");
        assertEquals(2, page.getDisplayedRowCount());
        assertTrue(page.getDisplayedRuns().stream().allMatch(r -> r.getTopologyName().contains("Datacenter")));

        // 2. Case-insensitive search: "campus"
        page.filter("campus");
        assertEquals(1, page.getDisplayedRowCount());
        assertEquals("Campus Ring", page.getDisplayedRuns().get(0).getTopologyName());

        // 3. Filter by Run ID: "3"
        page.filter("3");
        assertEquals(1, page.getDisplayedRowCount());
        assertEquals(3, page.getDisplayedRuns().get(0).getId());

        // 4. Query with 0 matches
        page.filter("NonexistentTopologyXYZ");
        assertEquals(0, page.getDisplayedRowCount());
        assertTrue(searchComponentTree(page, "No Matching Runs"));

        // 5. Clear filter restores all
        page.filter("");
        assertEquals(3, page.getDisplayedRowCount());
    }

    @Test
    void testRefreshReloadsRunsFromDatabase() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        service.addRun(new SimulationRunRecord(1, 1, "Run 1", LocalDateTime.now(), 10, 5, 5, 0, 0, 100.0, 1.0, 0, 0, ""));

        RecordingListener listener = new RecordingListener();
        SimulationHistoryPage page = new SimulationHistoryPage(service, listener);
        assertEquals(1, page.getDisplayedRowCount());
        int initialCallCount = service.getRefreshCallCount();

        // Add a 2nd run to backend and trigger refresh
        service.addRun(new SimulationRunRecord(2, 1, "Run 2", LocalDateTime.now(), 20, 5, 4, 1, 0, 80.0, 2.0, 0, 0, ""));
        page.getRefreshButton().doClick();

        assertTrue(service.getRefreshCallCount() > initialCallCount);
        assertEquals(2, page.getDisplayedRowCount());
    }

    @Test
    void testHeaderNavigationButtonsTriggerCallbacks() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        RecordingListener listener = new RecordingListener();

        SimulationHistoryPage page = new SimulationHistoryPage(service, listener);

        JButton btnHome = findButtonWithText(page, "← Home");
        assertNotNull(btnHome, "Home button must exist");
        btnHome.doClick();
        assertTrue(listener.homeClicked, "Clicking ← Home must invoke onBackToHome");

        JButton btnSim = findButtonWithText(page, "Launch Simulator");
        assertNotNull(btnSim, "Simulator button must exist");
        btnSim.doClick();
        assertTrue(listener.launchClicked, "Clicking Launch Simulator must invoke onLaunchSimulator");
    }

    @Test
    void testSimulationRunDetailDialogRendersAllMetricsReadonly() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        LocalDateTime now = LocalDateTime.of(2026, 10, 23, 10, 30, 0);
        SimulationRunRecord record = new SimulationRunRecord(
                42, 105, "Enterprise WAN Core", now,
                500, 100, 95, 3, 2, 95.0, 4.25, 2, 4, "High volume peak traffic test with simulated link outages."
        );

        SimulationHistoryPage.SimulationRunDetailDialog dialog =
                new SimulationHistoryPage.SimulationRunDetailDialog(null, record);
        assertNotNull(dialog);

        // Verify title & contents
        assertEquals("Simulation Run #42 Details", dialog.getTitle());
        assertTrue(searchComponentTree(dialog, "Run #42 — Enterprise WAN Core"));
        assertTrue(searchComponentTree(dialog, "Associated Topology ID: #105"));
        assertTrue(searchComponentTree(dialog, "500 ticks"));
        assertTrue(searchComponentTree(dialog, "100"));
        assertTrue(searchComponentTree(dialog, "95"));
        assertTrue(searchComponentTree(dialog, "95.00%"));
        assertTrue(searchComponentTree(dialog, "4.25 ticks"));
        assertTrue(searchComponentTree(dialog, "2 failed / 4 reroutes"));
        assertTrue(searchComponentTree(dialog, "High volume peak traffic test with simulated link outages."));

        dialog.dispose();
    }

    @Test
    void testDetailViewingAndNavigationDoesNotMutateActiveSimulatorState() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        NetRouteFrame frame = new NetRouteFrame();
        frame.setSkipConfirmationForTesting(true);

        // 1. Establish custom network state
        Network net = new Network();
        net.addNode(new Computer("CLI_1", "Client 1", 100, 100));
        net.addNode(new Router("RTR_1", "Router 1", 200, 100));
        net.addNode(new Server("SRV_1", "Server 1", 300, 100));
        net.addLink(new NetworkLink("LK_1", "CLI_1", "RTR_1", 2.0, 2.0, 10, false));
        net.addLink(new NetworkLink("LK_2", "RTR_1", "SRV_1", 3.0, 3.0, 10, false));
        frame.setNetwork(net);

        frame.onSendPacket("CLI_1", "SRV_1");
        frame.onStepSimulation();
        frame.onStepSimulation();

        // Capture snapshot of simulator state
        int initialNodeCount = frame.getNetwork().nodeCount();
        int initialLinkCount = frame.getNetwork().getLinks().size();
        long initialTicks = frame.getEngine().getCurrentTime();
        int initialPackets = frame.getEngine().getPackets().size();
        String initialTopName = frame.getActiveTopologyName();
        Integer initialTopId = frame.getActiveTopologyId();
        boolean initialSavedStatus = frame.isCurrentRunSaved();

        assertTrue(initialTicks > 0, "Simulation should have advanced ticks");
        assertTrue(initialPackets > 0, "Simulation should have active/delivered packets");

        // 2. Navigate to Simulation History page
        frame.showSimulationHistoryPage();
        assertEquals(NetRouteFrame.CARD_SIMULATION_HISTORY, frame.getCurrentCard());

        SimulationHistoryPage historyPage = frame.getSimulationHistoryPage();
        assertNotNull(historyPage);

        // Perform interactions on history page: filter, refresh, detail dialog
        historyPage.filter("Nonexistent");
        historyPage.filter("");

        if (historyPage.getDisplayedRowCount() > 0) {
            SimulationRunRecord run = historyPage.getDisplayedRuns().get(0);
            SimulationHistoryPage.SimulationRunDetailDialog dialog =
                    new SimulationHistoryPage.SimulationRunDetailDialog(frame, run);
            dialog.dispose();
        }

        // 3. Return to simulator
        frame.showSimulatorPage();
        assertEquals(NetRouteFrame.CARD_SIMULATOR, frame.getCurrentCard());

        // 4. Verify ZERO mutation to simulator state
        assertEquals(initialNodeCount, frame.getNetwork().nodeCount(), "Node count must not change");
        assertEquals(initialLinkCount, frame.getNetwork().getLinks().size(), "Link count must not change");
        assertEquals(initialTicks, frame.getEngine().getCurrentTime(), "Simulation clock must not change");
        assertEquals(initialPackets, frame.getEngine().getPackets().size(), "Packet count must not change");
        assertEquals(initialTopName, frame.getActiveTopologyName(), "Topology name must not change");
        assertEquals(initialTopId, frame.getActiveTopologyId(), "Active topology ID must not change");
        assertEquals(initialSavedStatus, frame.isCurrentRunSaved(), "Saved status must not change");

        frame.dispose();
    }

    // ==========================================
    // UI Tree Search Helpers
    // ==========================================

    private static boolean searchComponentTree(Component root, String textToFind) {
        if (root instanceof JLabel lbl) {
            if (lbl.getText() != null && lbl.getText().contains(textToFind)) {
                return true;
            }
        }
        if (root instanceof JButton btn) {
            if (btn.getText() != null && btn.getText().contains(textToFind)) {
                return true;
            }
        }
        if (root instanceof JTextArea ta) {
            if (ta.getText() != null && ta.getText().contains(textToFind)) {
                return true;
            }
        }
        if (root instanceof JTextField tf) {
            if (tf.getText() != null && tf.getText().contains(textToFind)) {
                return true;
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (searchComponentTree(child, textToFind)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JButton findButtonWithText(Component root, String text) {
        if (root instanceof JButton btn) {
            if (btn.getText() != null && btn.getText().contains(text)) {
                return btn;
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButtonWithText(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

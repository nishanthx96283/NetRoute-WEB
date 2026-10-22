package com.netroute.ui;

import com.netroute.repository.DatabaseConnection;
import com.netroute.repository.TopologySummary;
import com.netroute.service.TopologyStorageService;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryPageTest {

    private static class StubStorageService extends TopologyStorageService {
        private boolean available = true;
        private final List<TopologySummary> summaries = new ArrayList<>();
        private boolean refreshCalled = false;

        public StubStorageService() {
            super(new DatabaseConnection());
        }

        @Override
        public boolean isDatabaseAvailable() {
            return available;
        }

        @Override
        public List<TopologySummary> listActiveTopologies() throws SQLException {
            refreshCalled = true;
            return new ArrayList<>(summaries);
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public void addSummary(TopologySummary summary) {
            summaries.add(summary);
        }

        public void clear() {
            summaries.clear();
        }
    }

    private static class RecordingListener implements HistoryPage.Listener {
        boolean homeClicked = false;
        boolean launchClicked = false;
        TopologySummary loadedSummary = null;
        TopologySummary deletedSummary = null;

        @Override
        public void onBackToHome() {
            homeClicked = true;
        }

        @Override
        public void onLaunchSimulator() {
            launchClicked = true;
        }

        @Override
        public void onLoadTopology(TopologySummary summary) {
            loadedSummary = summary;
        }

        @Override
        public void onDeleteTopology(TopologySummary summary) {
            deletedSummary = summary;
        }
    }

    @Test
    void historyPageDisplaysOfflineStateWhenDatabaseUnavailable() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(false);
        RecordingListener listener = new RecordingListener();

        HistoryPage page = new HistoryPage(service, listener);
        assertNotNull(page);
        assertEquals(0, page.getDisplayedCardCount());

        boolean foundOfflineText = searchComponentTree(page, "Database Unavailable");
        assertTrue(foundOfflineText, "Must display offline warning when database is unavailable");
    }

    @Test
    void historyPageDisplaysEmptyStateWhenNoTopologiesExist() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        service.clear();
        RecordingListener listener = new RecordingListener();

        HistoryPage page = new HistoryPage(service, listener);
        assertNotNull(page);
        assertEquals(0, page.getDisplayedCardCount());

        boolean foundEmptyState = searchComponentTree(page, "No Saved Topologies Found");
        assertTrue(foundEmptyState, "Must display empty state banner when topology list is empty");
    }

    @Test
    void historyPageRendersTopologyCardsWithDetails() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        LocalDateTime now = LocalDateTime.now();
        service.addSummary(new TopologySummary(
                101,
                "Campus Backbone Alpha",
                "High capacity core network with dual routers.",
                6,
                7,
                false,
                now.minusDays(1),
                now
        ));
        service.addSummary(new TopologySummary(
                102,
                "Datacenter Pod 3",
                "Leaf-spine mesh architecture.",
                12,
                24,
                false,
                now.minusHours(5),
                now.minusMinutes(10)
        ));

        RecordingListener listener = new RecordingListener();
        HistoryPage page = new HistoryPage(service, listener);

        assertEquals(2, page.getDisplayedCardCount());
        assertEquals("Campus Backbone Alpha", page.getLoadedSummaries().get(0).getName());
        assertEquals("Datacenter Pod 3", page.getLoadedSummaries().get(1).getName());

        assertTrue(searchComponentTree(page, "Campus Backbone Alpha"));
        assertTrue(searchComponentTree(page, "Datacenter Pod 3"));
        assertTrue(searchComponentTree(page, "ID #101"));
        assertTrue(searchComponentTree(page, "ID #102"));
        assertTrue(searchComponentTree(page, "6 Nodes"));
        assertTrue(searchComponentTree(page, "7 Links"));
        assertTrue(searchComponentTree(page, "Leaf-spine mesh architecture."));
    }

    @Test
    void historyPageTriggersLoadAndDeleteCallbacks() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        TopologySummary summary = new TopologySummary(
                205,
                "WAN Simulation Beta",
                "Geographic mesh network.",
                4,
                5,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        service.addSummary(summary);

        RecordingListener listener = new RecordingListener();
        HistoryPage page = new HistoryPage(service, listener);

        // Find Load button and click it
        JButton btnLoad = findButtonWithText(page, "Load");
        assertNotNull(btnLoad, "Card must have a Load button");
        btnLoad.doClick();
        assertNotNull(listener.loadedSummary);
        assertEquals(205, listener.loadedSummary.getId());
        assertEquals("WAN Simulation Beta", listener.loadedSummary.getName());

        // Find Delete button and click it
        JButton btnDelete = findButtonWithText(page, "Delete");
        assertNotNull(btnDelete, "Card must have a Delete button");
        btnDelete.doClick();
        assertNotNull(listener.deletedSummary);
        assertEquals(205, listener.deletedSummary.getId());
    }

    @Test
    void historyPageHeaderButtonsTriggerNavigationAndRefresh() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        StubStorageService service = new StubStorageService();
        service.setAvailable(true);
        RecordingListener listener = new RecordingListener();
        HistoryPage page = new HistoryPage(service, listener);

        page.getBtnHomeHeader().doClick();
        assertTrue(listener.homeClicked, "Home button must trigger onBackToHome");

        page.getBtnLaunchHeader().doClick();
        assertTrue(listener.launchClicked, "Launch Simulator button must trigger onLaunchSimulator");

        service.refreshCalled = false;
        page.getBtnRefreshHeader().doClick();
        assertTrue(service.refreshCalled, "Refresh button must query service and refresh");
    }

    private static boolean searchComponentTree(Component comp, String expectedSubstring) {
        if (comp instanceof JLabel lbl) {
            if (lbl.getText() != null && lbl.getText().contains(expectedSubstring)) {
                return true;
            }
        } else if (comp instanceof JButton btn) {
            if (btn.getText() != null && btn.getText().contains(expectedSubstring)) {
                return true;
            }
        } else if (comp instanceof JTextArea txt) {
            if (txt.getText() != null && txt.getText().contains(expectedSubstring)) {
                return true;
            }
        }
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (searchComponentTree(child, expectedSubstring)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JButton findButtonWithText(Component comp, String expectedSubstring) {
        if (comp instanceof JButton btn) {
            if (btn.getText() != null && btn.getText().contains(expectedSubstring)) {
                return btn;
            }
        }
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButtonWithText(child, expectedSubstring);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

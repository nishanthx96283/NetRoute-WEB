package com.netroute.service;

import com.netroute.graph.Network;
import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.Server;
import com.netroute.repository.DatabaseConnection;
import com.netroute.repository.SavedTopology;
import com.netroute.repository.TopologySummary;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologyStorageServiceTest {

    @Test
    void validatesBlankTopologyName() {
        TopologyStorageService service = new TopologyStorageService(new DatabaseConnection());
        Network network = new Network();

        assertThrows(IllegalArgumentException.class, () ->
                service.saveTopology(null, "desc", network, false));

        assertThrows(IllegalArgumentException.class, () ->
                service.saveTopology("   ", "desc", network, false));
    }

    @Test
    void validatesEmptyNetwork() {
        TopologyStorageService service = new TopologyStorageService(new DatabaseConnection());
        Network emptyNet = new Network();

        assertThrows(IllegalArgumentException.class, () ->
                service.saveTopology("ValidName", "desc", emptyNet, false));
    }

    @Test
    void serviceExposesConnectionDiagnostics() {
        TopologyStorageService service = new TopologyStorageService(new DatabaseConnection());
        DatabaseConnection.ConnectionResult result = service.testConnection();
        assertNotNull(result);
        assertNotNull(result.getMessage());
    }

    @Test
    void loadNonExistentTopologyReturnsEmpty() throws SQLException {
        TopologyStorageService service = new TopologyStorageService(new DatabaseConnection());
        if (service.isDatabaseAvailable()) {
            Optional<SavedTopology> result = service.loadTopology(-999999);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void deleteNonExistentTopologyReturnsFalse() throws SQLException {
        TopologyStorageService service = new TopologyStorageService(new DatabaseConnection());
        if (service.isDatabaseAvailable()) {
            boolean deleted = service.deleteTopologyPermanently(-999999);
            assertFalse(deleted);
        }
    }

    @Test
    void serviceSupportsSaveLoadListAndDeleteCycle() throws Exception {
        TopologyStorageService service = new TopologyStorageService(new DatabaseConnection());
        if (!service.isDatabaseAvailable()) {
            return;
        }

        Network net = new Network();
        net.addNode(new Computer("PC_CYCLE", "Cycle PC", 100, 100));
        net.addNode(new Server("S_CYCLE", "Cycle Server", 300, 200));
        net.addLink(new NetworkLink("L_CYCLE", "PC_CYCLE", "S_CYCLE", 2.5, 4.0, 10, false));

        String name = "ServiceCycleTest_" + System.currentTimeMillis();
        int id = service.saveTopology(name, "Testing full cycle", net, false);
        assertTrue(id > 0);

        // List active topologies
        List<TopologySummary> active = service.listActiveTopologies();
        boolean foundInList = active.stream().anyMatch(s -> s.getId() == id && s.getName().equals(name));
        assertTrue(foundInList, "Saved topology should appear in active topologies list");

        // Load topology
        Optional<SavedTopology> loadedOpt = service.loadTopology(id);
        assertTrue(loadedOpt.isPresent(), "Loaded topology should be present");
        SavedTopology loaded = loadedOpt.get();
        assertEquals(name, loaded.getName());
        assertEquals(2, loaded.getNetwork().nodeCount());
        assertEquals(1, loaded.getNetwork().linkCount());
        assertTrue(loaded.getNetwork().findNode("PC_CYCLE").isPresent());
        assertTrue(loaded.getNetwork().findNode("S_CYCLE").isPresent());

        // Delete permanently
        boolean deleted = service.deleteTopologyPermanently(id);
        assertTrue(deleted, "deleteTopologyPermanently should return true");

        // Verify it is gone
        Optional<SavedTopology> afterDelete = service.loadTopology(id);
        assertTrue(afterDelete.isEmpty(), "Topology must not exist after permanent deletion");
    }
}

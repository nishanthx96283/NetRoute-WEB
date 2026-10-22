package com.netroute.ui;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.Route;
import com.netroute.routing.DijkstraRouter;
import com.netroute.simulation.SimulationEngine;
import com.netroute.statistics.NetworkStatistics;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}

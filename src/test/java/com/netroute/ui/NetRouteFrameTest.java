package com.netroute.ui;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.NodeType;
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
}

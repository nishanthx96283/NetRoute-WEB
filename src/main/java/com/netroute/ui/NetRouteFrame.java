package com.netroute.ui;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.model.Packet;
import com.netroute.model.PacketStatus;
import com.netroute.model.Route;
import com.netroute.routing.DijkstraRouter;
import com.netroute.simulation.SimulationEngine;
import com.netroute.statistics.NetworkStatistics;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Main application window for NetRoute network simulator.
 * <p>
 * Integrates the network topology canvas, control panel, statistics display,
 * and activity event log. Connects to {@link DijkstraRouter}, {@link SimulationEngine},
 * and {@link NetworkStatistics}.
 */
public class NetRouteFrame extends JFrame implements ControlPanel.Listener {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private GraphManager graphManager;
    private Network network;
    private DijkstraRouter router;
    private SimulationEngine engine;

    private TopologyPanel topologyPanel;
    private ControlPanel controlPanel;
    private StatisticsPanel statisticsPanel;
    private JTextArea logArea;

    public NetRouteFrame() {
        super("NetRoute - Network Traffic & Routing Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setPreferredSize(new Dimension(1220, 800));

        initBackend();
        initComponents();
        initMenuBar();

        pack();
        setLocationRelativeTo(null);

        log("NetRoute initialized with sample campus network.");
        log("Sample topology: 3 Computers (PC1, PC2, PC3), 2 Routers (R1, R2), 1 Server (S1).");
        log("Ready. Select Source and Destination, then calculate route or send packets.");
    }

    private void initBackend() {
        this.graphManager = GraphManager.createSampleNetwork();
        this.network = graphManager.getNetwork();
        this.router = new DijkstraRouter();
        this.engine = new SimulationEngine(network, router);
    }

    private void initComponents() {
        setLayout(new BorderLayout(4, 4));

        // 1. Center Topology Canvas
        topologyPanel = new TopologyPanel(network);
        topologyPanel.setBorder(BorderFactory.createTitledBorder("Network Topology Map (Drag nodes to reposition)"));

        // 2. Right Side: Controls and Statistics in a vertical box
        controlPanel = new ControlPanel();
        controlPanel.setListener(this);
        controlPanel.populate(network);

        statisticsPanel = new StatisticsPanel();
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));

        JPanel rightContent = new JPanel();
        rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
        rightContent.add(controlPanel);
        rightContent.add(statisticsPanel);

        JScrollPane rightSidebarScroll = new JScrollPane(rightContent);
        rightSidebarScroll.setPreferredSize(new Dimension(420, 720));
        rightSidebarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightSidebarScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rightSidebarScroll.setBorder(null);

        // Center split pane between canvas and right sidebar
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, topologyPanel, rightSidebarScroll);
        centerSplit.setResizeWeight(0.68);
        centerSplit.setContinuousLayout(true);
        add(centerSplit, BorderLayout.CENTER);

        // 3. Bottom: Event & Simulation Activity Log
        logArea = new JTextArea(8, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(250, 250, 252));
        logArea.setForeground(new Color(33, 37, 41));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Event & Simulation Activity Log"),
                BorderFactory.createEmptyBorder(2, 4, 4, 4)
        ));
        add(logScroll, BorderLayout.SOUTH);
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        JMenuItem itemReset = new JMenuItem("Reset Sample Network");
        itemReset.addActionListener(e -> onResetNetwork());
        JMenuItem itemExit = new JMenuItem("Exit");
        itemExit.addActionListener(e -> System.exit(0));
        menuFile.add(itemReset);
        menuFile.addSeparator();
        menuFile.add(itemExit);

        JMenu menuSim = new JMenu("Simulation");
        JMenuItem itemRandomize = new JMenuItem("Randomize Link Costs");
        itemRandomize.addActionListener(e -> onRandomizeLinkCosts());
        JMenuItem itemStep = new JMenuItem("Step Simulation (1 Tick)");
        itemStep.addActionListener(e -> onStepSimulation());
        JMenuItem itemClearLog = new JMenuItem("Clear Log");
        itemClearLog.addActionListener(e -> logArea.setText(""));
        menuSim.add(itemRandomize);
        menuSim.add(itemStep);
        menuSim.add(itemClearLog);

        JMenu menuHelp = new JMenu("Help");
        JMenuItem itemAbout = new JMenuItem("About NetRoute");
        itemAbout.addActionListener(e -> showAboutDialog());
        menuHelp.add(itemAbout);

        menuBar.add(menuFile);
        menuBar.add(menuSim);
        menuBar.add(menuHelp);
        setJMenuBar(menuBar);
    }

    // ---- ControlPanel.Listener Callbacks ----

    @Override
    public void onFindRoute(String sourceId, String destinationId) {
        topologyPanel.setSelectedEndpoints(sourceId, destinationId);
        Optional<Route> routeOpt = router.findRoute(network, sourceId, destinationId);

        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            topologyPanel.setHighlightedRoute(route);
            String pathStr = String.join(" -> ", route.getNodeIds());
            String summary = String.format("Route: %s (Total Cost: %.1f)", pathStr, route.getTotalCost());
            controlPanel.setRouteResult(summary, true);

            log(String.format("[Dijkstra] Shortest path %s -> %s: %s | Cost: %.1f | Hops: %d",
                    sourceId, destinationId, pathStr, route.getTotalCost(), route.getLinks().size()));
        } else {
            topologyPanel.clearHighlightedRoute();
            controlPanel.setRouteResult("No active route found between " + sourceId + " and " + destinationId, false);
            log(String.format("[Dijkstra] No route exists from %s to %s. A link may be inactive or partitioned.",
                    sourceId, destinationId));
        }
    }

    @Override
    public void onSendPacket(String sourceId, String destinationId) {
        topologyPanel.setSelectedEndpoints(sourceId, destinationId);

        // Submit packet to existing engine
        Packet packet = engine.submitPacket(sourceId, destinationId, 1);
        log(String.format("[Packet Submit] %s created from %s -> %s at tick %d.",
                packet.getId(), sourceId, destinationId, packet.getCreatedAt()));

        // Run simulation to completion
        engine.run();

        // Highlight packet route if available
        if (packet.getRoute().isPresent()) {
            topologyPanel.setHighlightedRoute(packet.getRoute().get());
        }

        // Report result
        if (packet.getStatus() == PacketStatus.DELIVERED) {
            long deliveredTick = packet.getDeliveredAt().orElse(0L);
            long delay = deliveredTick - packet.getCreatedAt();
            log(String.format("[Packet Delivered] %s arrived at destination %s at tick %d (Delay: %d ticks). Visited: %s",
                    packet.getId(), destinationId, deliveredTick, delay, packet.getVisitedNodeIds()));

            if (packet.getRerouteCount() > 0) {
                log(String.format("[Reroute] Packet %s was rerouted %d time(s) due to link failures during transit.",
                        packet.getId(), packet.getRerouteCount()));
            }
            controlPanel.setRouteResult(String.format("%s DELIVERED in %d ticks (Reroutes: %d)",
                    packet.getId(), delay, packet.getRerouteCount()), true);
        } else {
            String reason = packet.getReason().orElse("No available route");
            log(String.format("[Packet %s] %s: %s", packet.getId(), packet.getStatus(), reason));
            controlPanel.setRouteResult(String.format("%s %s: %s", packet.getId(), packet.getStatus(), reason), false);
        }

        // Update statistics and view
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));
        topologyPanel.repaint();
    }

    @Override
    public void onStepSimulation() {
        long prevTick = engine.getCurrentTime();
        engine.step();
        long newTick = engine.getCurrentTime();

        log(String.format("[Simulation Step] Advanced tick %d -> %d. Idle: %b",
                prevTick, newTick, engine.isIdle()));

        statisticsPanel.updateStatistics(new NetworkStatistics(engine));
        topologyPanel.repaint();
    }

    @Override
    public void onFailLink(String linkId) {
        NetworkLink link = network.getLink(linkId);
        if (link == null) {
            log("[Link Error] Unknown link ID: " + linkId);
            return;
        }

        if (!link.isActive()) {
            log("[Link] Link " + linkId + " is already inactive/failed.");
            return;
        }

        // Deactivate and reroute affected in-transit packets
        engine.failLink(linkId);

        log(String.format("[Link Failed] Link %s (%s - %s) deactivated. Rerouting affected packets if any.",
                linkId, link.getSourceId(), link.getDestinationId()));

        controlPanel.populateLinks(network);
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));
        topologyPanel.repaint();
    }

    @Override
    public void onRestoreLink(String linkId) {
        NetworkLink link = network.getLink(linkId);
        if (link == null) {
            log("[Link Error] Unknown link ID: " + linkId);
            return;
        }

        if (link.isActive()) {
            log("[Link] Link " + linkId + " is already active.");
            return;
        }

        engine.restoreLink(linkId);

        log(String.format("[Link Restored] Link %s (%s - %s) restored to ACTIVE state.",
                linkId, link.getSourceId(), link.getDestinationId()));

        controlPanel.populateLinks(network);
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));
        topologyPanel.repaint();
    }

    @Override
    public void onRandomizeLinkCosts() {
        graphManager.randomizeLinkCosts();
        controlPanel.populateLinks(network);
        topologyPanel.repaint();
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));

        log("[Cost Randomization] All link costs randomized to positive integers (1-10). Active/inactive statuses preserved.");

        // Automatically recalculate route for the selected endpoints if present
        String src = controlPanel.getSelectedSource();
        String dst = controlPanel.getSelectedDestination();
        if (src != null && dst != null) {
            onFindRoute(src, dst);
        }
    }

    @Override
    public void onAddNode(NodeType type) {
        try {
            NetworkNode node = graphManager.addDynamicNode(type);
            controlPanel.populate(network);
            topologyPanel.repaint();
            statisticsPanel.updateStatistics(new NetworkStatistics(engine));

            log(String.format("[Node Created] Added %s '%s' (%s) at (%.0f, %.0f). Initially unconnected.",
                    node.getType(), node.getId(), node.getName(), node.getX(), node.getY()));
        } catch (Exception ex) {
            log("[Node Error] " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Node Creation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onConnectNodes(String sourceId, String destinationId, double cost) {
        try {
            NetworkLink link = graphManager.connectNodes(sourceId, destinationId, cost);
            engine.queueFor(link.getId()); // Register queue in SimulationEngine

            controlPanel.populateLinks(network);
            topologyPanel.repaint();
            statisticsPanel.updateStatistics(new NetworkStatistics(engine));

            log(String.format("[Link Created] Connected %s <-> %s with link %s (Cost: %.1f).",
                    sourceId, destinationId, link.getId(), link.getCost()));

            // Recalculate route if endpoints match or could benefit from the new link
            String src = controlPanel.getSelectedSource();
            String dst = controlPanel.getSelectedDestination();
            if (src != null && dst != null) {
                onFindRoute(src, dst);
            }
        } catch (IllegalArgumentException ex) {
            log("[Connection Error] " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Link Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean skipConfirmationForTesting = false;

    void setSkipConfirmationForTesting(boolean skip) {
        this.skipConfirmationForTesting = skip;
    }

    private boolean confirmDeletion(String message, String title) {
        if (skipConfirmationForTesting || !isVisible() || GraphicsEnvironment.isHeadless()) {
            return true;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return confirm == JOptionPane.YES_OPTION;
    }

    @Override
    public void onDeleteNode(String nodeId) {
        if (nodeId == null || nodeId.trim().isEmpty() || !network.containsNode(nodeId.trim())) {
            log("[Node Error] Cannot delete node: node does not exist: " + nodeId);
            return;
        }
        String id = nodeId.trim();

        if (!confirmDeletion("Are you sure you want to delete node '" + id + "' and all connected links?",
                "Confirm Node Deletion")) {
            log("[Node Deletion Cancelled] Deletion of node " + id + " cancelled by user.");
            return;
        }

        String prevSrc = controlPanel.getSelectedSource();
        String prevDst = controlPanel.getSelectedDestination();

        List<String> incidentLinkIds = graphManager.getIncidentLinkIds(id);

        for (String linkId : incidentLinkIds) {
            engine.removeLink(linkId);
        }
        engine.removeNode(id);
        graphManager.removeNode(id);

        controlPanel.populate(network);
        topologyPanel.repaint();
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));

        log(String.format("[Node Deleted] Removed node '%s' and %d connected link(s): %s.",
                id, incidentLinkIds.size(), incidentLinkIds));

        if (id.equals(prevSrc) || id.equals(prevDst)) {
            topologyPanel.clearHighlightedRoute();
            topologyPanel.setSelectedEndpoints(controlPanel.getSelectedSource(), controlPanel.getSelectedDestination());
            controlPanel.setRouteResult("Route cleared: node " + id + " was removed.", false);
        } else {
            String newSrc = controlPanel.getSelectedSource();
            String newDst = controlPanel.getSelectedDestination();
            if (newSrc != null && newDst != null && !newSrc.equals(newDst)) {
                onFindRoute(newSrc, newDst);
            } else {
                topologyPanel.clearHighlightedRoute();
                topologyPanel.setSelectedEndpoints(newSrc, newDst);
            }
        }
    }

    @Override
    public void onDeleteLink(String linkId) {
        if (linkId == null || linkId.trim().isEmpty() || !network.containsLink(linkId.trim())) {
            log("[Link Error] Cannot delete link: link does not exist: " + linkId);
            return;
        }
        String id = linkId.trim();

        if (!confirmDeletion("Are you sure you want to delete link '" + id + "'?",
                "Confirm Link Deletion")) {
            log("[Link Deletion Cancelled] Deletion of link " + id + " cancelled by user.");
            return;
        }

        NetworkLink link = network.getLink(id);
        String desc = link != null ? (link.getSourceId() + " <-> " + link.getDestinationId()) : "";

        engine.removeLink(id);
        graphManager.removeLink(id);

        controlPanel.populateLinks(network);
        topologyPanel.repaint();
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));

        log(String.format("[Link Deleted] Removed link '%s' (%s) from network and simulation engine.", id, desc));

        String src = controlPanel.getSelectedSource();
        String dst = controlPanel.getSelectedDestination();
        if (src != null && dst != null && !src.equals(dst)) {
            onFindRoute(src, dst);
        } else {
            topologyPanel.clearHighlightedRoute();
        }
    }

    @Override
    public void onResetNetwork() {
        initBackend();
        topologyPanel.setNetwork(network);
        controlPanel.populate(network);
        controlPanel.setRouteResult("Network reset to default state.", true);
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));

        log("--------------------------------------------------");
        log("[Reset] Network reloaded with clean sample topology and new simulation engine.");
    }

    private void log(String message) {
        String timeStr = LocalTime.now().format(TIME_FORMAT);
        logArea.append("[" + timeStr + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showAboutDialog() {
        String msg = "NetRoute Network Simulator\n\n"
                + "A Java desktop application for simulating network traffic, Dijkstra routing,\n"
                + "discrete-event packet simulation, dynamic link failures, and performance metrics.\n\n"
                + "College Data Structures & Algorithms Project.";
        JOptionPane.showMessageDialog(this, msg, "About NetRoute", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            NetRouteFrame frame = new NetRouteFrame();
            frame.setVisible(true);
        });
    }
}

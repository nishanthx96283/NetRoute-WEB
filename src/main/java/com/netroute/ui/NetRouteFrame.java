package com.netroute.ui;

import com.netroute.graph.GraphManager;
import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.model.Packet;
import com.netroute.model.PacketStatus;
import com.netroute.model.Route;
import com.netroute.repository.DuplicateTopologyNameException;
import com.netroute.repository.UnifiedSaveResult;
import com.netroute.routing.DijkstraRouter;
import com.netroute.service.TopologyStorageService;
import com.netroute.simulation.SimulationEngine;
import com.netroute.statistics.NetworkStatistics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Main application window for NetRoute network simulator.
 * <p>
 * Features a modern dark theme and card-based navigation between a dedicated Landing Page
 * and the interactive Network Simulator workspace.
 */
public class NetRouteFrame extends JFrame implements ControlPanel.Listener, LandingPage.Listener {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final String CARD_LANDING = "LANDING";
    public static final String CARD_SIMULATOR = "SIMULATOR";
    public static final String CARD_TECH_STACK = "TECH_STACK";
    public static final String CARD_ABOUT = "ABOUT";
    public static final String CARD_HISTORY = "HISTORY";
    public static final String CARD_SIMULATION_HISTORY = "SIMULATION_HISTORY";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);
    private String currentCard = CARD_LANDING;

    // Backend models
    private GraphManager graphManager;
    private Network network;
    private DijkstraRouter router;
    private SimulationEngine engine;
    private TopologyStorageService storageService;

    // UI Components
    private LandingPage landingPage;
    private TechStackPage techStackPage;
    private AboutPage aboutPage;
    private HistoryPage historyPage;
    private SimulationHistoryPage simulationHistoryPage;
    private JPanel simulatorView;
    private TopologyPanel topologyPanel;
    private ControlPanel controlPanel;
    private StatisticsPanel statisticsPanel;
    private JTextArea logArea;
    private JLabel lblStatusBadge;
    private JButton btnNewProject;

    public enum UnsavedChangesAction {
        PROMPT,
        SAVE,
        DISCARD,
        CANCEL
    }

    private boolean skipConfirmationForTesting = false;
    private UnsavedChangesAction testUnsavedChangesAction = UnsavedChangesAction.DISCARD;

    // State and persistence tracking
    private Integer activeTopologyId = null;
    private String activeTopologyName = "Sample Campus Network";
    private String activeTopologyDescription = "";
    private boolean topologyModified = false;
    private boolean currentRunSaved = false;
    private Integer lastSavedRunId = null;

    public NetRouteFrame() {
        super("NetRoute - Network Traffic & Routing Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 740));
        setPreferredSize(new Dimension(1260, 820));

        initBackend();
        initComponents();
        initMenuBar();

        pack();
        setLocationRelativeTo(null);

        // Open to Landing Page by default
        showLandingPage();

        log("NetRoute initialized with sample campus network.");
        log("Sample topology: 3 Computers (PC1, PC2, PC3), 2 Routers (R1, R2), 1 Server (S1).");
        log("Ready. Select Source and Destination, then calculate route or send packets.");
    }

    private void initBackend() {
        this.graphManager = GraphManager.createSampleNetwork();
        this.network = graphManager.getNetwork();
        this.router = new DijkstraRouter();
        this.engine = new SimulationEngine(network, router);
        this.storageService = new TopologyStorageService();
        this.activeTopologyId = null;
        this.activeTopologyName = "Sample Campus Network";
        this.activeTopologyDescription = "";
        this.topologyModified = false;
        this.currentRunSaved = false;
        this.lastSavedRunId = null;
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_MAIN);

        // 1. Landing Page Card
        landingPage = new LandingPage(this);
        rootPanel.add(landingPage, CARD_LANDING);

        // 2. Tech Stack Page Card
        TechStackPage.NavigationHandler techStackNav = new TechStackPage.NavigationHandler() {
            @Override
            public void onBackToHome() {
                showLandingPage();
            }

            @Override
            public void onLaunchSimulator() {
                showSimulatorPage();
            }
        };
        techStackPage = new TechStackPage(techStackNav);
        rootPanel.add(techStackPage, CARD_TECH_STACK);

        // 3. About Page Card
        AboutPage.NavigationHandler aboutNav = new AboutPage.NavigationHandler() {
            @Override
            public void onBackToHome() {
                showLandingPage();
            }

            @Override
            public void onLaunchSimulator() {
                showSimulatorPage();
            }
        };
        aboutPage = new AboutPage(aboutNav);
        rootPanel.add(aboutPage, CARD_ABOUT);

        // 4. History Page Card
        HistoryPage.Listener historyNav = new HistoryPage.Listener() {
            @Override
            public void onBackToHome() {
                showLandingPage();
            }

            @Override
            public void onLaunchSimulator() {
                showSimulatorPage();
            }

            @Override
            public void onLoadTopology(com.netroute.repository.TopologySummary summary) {
                handleLoadTopology(summary.getId(), summary.getName());
            }

            @Override
            public void onDeleteTopology(com.netroute.repository.TopologySummary summary) {
                handleDeleteTopology(summary.getId(), summary.getName());
            }
        };
        historyPage = new HistoryPage(storageService, historyNav);
        rootPanel.add(historyPage, CARD_HISTORY);

        // 5. Simulation History Page Card
        SimulationHistoryPage.Listener simHistoryNav = new SimulationHistoryPage.Listener() {
            @Override
            public void onBackToHome() {
                showLandingPage();
            }

            @Override
            public void onLaunchSimulator() {
                showSimulatorPage();
            }
        };
        simulationHistoryPage = new SimulationHistoryPage(storageService, simHistoryNav);
        rootPanel.add(simulationHistoryPage, CARD_SIMULATION_HISTORY);

        // 6. Simulator Page Card
        simulatorView = createSimulatorView();
        rootPanel.add(simulatorView, CARD_SIMULATOR);

        add(rootPanel, BorderLayout.CENTER);
    }

    private JPanel createSimulatorView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setBackground(Theme.BG_MAIN);

        // Top Navigation Bar
        view.add(createSimulatorNavBar(), BorderLayout.NORTH);

        // Center: Topology Canvas & Right Control/Statistics Sidebar
        topologyPanel = new TopologyPanel(network);
        topologyPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        controlPanel = new ControlPanel();
        controlPanel.setListener(this);
        controlPanel.populate(network);

        statisticsPanel = new StatisticsPanel();
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));

        JPanel rightContent = new JPanel();
        rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
        rightContent.setBackground(Theme.BG_PANEL);
        rightContent.add(controlPanel);
        rightContent.add(Box.createRigidArea(new Dimension(0, 4)));
        rightContent.add(statisticsPanel);

        JScrollPane rightSidebarScroll = new JScrollPane(rightContent);
        rightSidebarScroll.setPreferredSize(new Dimension(420, 720));
        rightSidebarScroll.setBackground(Theme.BG_PANEL);
        rightSidebarScroll.getViewport().setBackground(Theme.BG_PANEL);
        rightSidebarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightSidebarScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rightSidebarScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        rightSidebarScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, topologyPanel, rightSidebarScroll);
        centerSplit.setResizeWeight(0.68);
        centerSplit.setContinuousLayout(true);
        centerSplit.setDividerSize(6);
        centerSplit.setBackground(Theme.BG_MAIN);
        centerSplit.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        view.add(centerSplit, BorderLayout.CENTER);

        // Bottom: Activity Log
        view.add(createActivityLogPanel(), BorderLayout.SOUTH);
        return view;
    }

    private JPanel createSimulatorNavBar() {
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setBackground(Theme.BG_PANEL);
        navBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // Left Branding & Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        // Logo Icon
        JLabel logoIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ACCENT_BLUE);
                g2.drawLine(5, 17, 12, 5);
                g2.drawLine(12, 5, 19, 17);
                g2.drawLine(5, 17, 19, 17);
                g2.setColor(Theme.ACCENT_CYAN);
                g2.fillOval(8, 2, 8, 8);
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.fillOval(2, 14, 6, 6);
                g2.fillOval(16, 14, 6, 6);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(24, 22);
            }
        };

        JLabel lblBrand = new JLabel("NetRoute");
        lblBrand.setFont(Theme.FONT_TITLE);
        lblBrand.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSep = new JLabel("›");
        lblSep.setFont(Theme.FONT_TITLE);
        lblSep.setForeground(Theme.TEXT_MUTED);

        JLabel lblTitle = new JLabel("Network Simulator");
        lblTitle.setFont(Theme.FONT_BODY_BOLD);
        lblTitle.setForeground(Theme.ACCENT_CYAN);

        btnNewProject = Theme.createButton("＋ New", Theme.ButtonStyle.SECONDARY);
        btnNewProject.setPreferredSize(new Dimension(80, 28));
        btnNewProject.setFont(Theme.FONT_SMALL);
        btnNewProject.setToolTipText("Start a fresh network project (Ctrl+N)");
        btnNewProject.addActionListener(e -> onNewProject());

        left.add(logoIcon);
        left.add(lblBrand);
        left.add(lblSep);
        left.add(lblTitle);
        left.add(Box.createRigidArea(new Dimension(10, 0)));
        left.add(btnNewProject);

        // Right Actions & Status Badge
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        lblStatusBadge = new JLabel("  ● Simulation Ready  ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblStatusBadge.setFont(Theme.FONT_SMALL);
        lblStatusBadge.setForeground(Theme.STATUS_SUCCESS);
        lblStatusBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JButton btnHome = Theme.createButton("← Home", Theme.ButtonStyle.SECONDARY);
        btnHome.setPreferredSize(new Dimension(95, 30));
        btnHome.setFont(Theme.FONT_SMALL);
        btnHome.addActionListener(e -> showLandingPage());

        JButton btnSave = Theme.createButton("💾 Save", Theme.ButtonStyle.PRIMARY);
        btnSave.setPreferredSize(new Dimension(100, 30));
        btnSave.setFont(Theme.FONT_SMALL);
        btnSave.addActionListener(e -> onSave());

        JButton btnHistory = Theme.createButton("📂 History", Theme.ButtonStyle.SECONDARY);
        btnHistory.setPreferredSize(new Dimension(100, 30));
        btnHistory.setFont(Theme.FONT_SMALL);
        btnHistory.addActionListener(e -> showHistoryPage());

        JButton btnSimHistory = Theme.createButton("📊 Run History", Theme.ButtonStyle.SECONDARY);
        btnSimHistory.setPreferredSize(new Dimension(115, 30));
        btnSimHistory.setFont(Theme.FONT_SMALL);
        btnSimHistory.setToolTipText("View recorded simulation runs and metrics");
        btnSimHistory.addActionListener(e -> showSimulationHistoryPage());

        JButton btnReset = Theme.createButton("Reset Network", Theme.ButtonStyle.OUTLINE);
        btnReset.setPreferredSize(new Dimension(115, 30));
        btnReset.setFont(Theme.FONT_SMALL);
        btnReset.addActionListener(e -> onResetNetwork());

        right.add(lblStatusBadge);
        right.add(btnSave);
        right.add(btnHistory);
        right.add(btnSimHistory);
        right.add(btnReset);
        right.add(btnHome);

        navBar.add(left, BorderLayout.WEST);
        navBar.add(right, BorderLayout.EAST);
        return navBar;
    }

    private JPanel createActivityLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Header with title and Clear Log button
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Activity & Event Log");
        lblTitle.setFont(Theme.FONT_SMALL);
        lblTitle.setForeground(Theme.TEXT_SECONDARY);

        JButton btnClear = Theme.createButton("Clear", Theme.ButtonStyle.OUTLINE);
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnClear.setPreferredSize(new Dimension(55, 20));
        btnClear.addActionListener(e -> {
            if (logArea != null) {
                logArea.setText("");
            }
        });

        header.add(lblTitle, BorderLayout.WEST);
        header.add(btnClear, BorderLayout.EAST);

        logArea = new JTextArea(6, 60);
        logArea.setEditable(false);
        logArea.setFont(Theme.FONT_MONO);
        logArea.setBackground(Theme.BG_INPUT);
        logArea.setForeground(Theme.TEXT_PRIMARY);
        logArea.setCaretColor(Theme.ACCENT_CYAN);
        logArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        logScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel.add(header, BorderLayout.NORTH);
        panel.add(logScroll, BorderLayout.CENTER);
        return panel;
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Theme.BG_PANEL);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));

        JMenu menuFile = createStyledMenu("File");
        JMenuItem itemNew = createStyledMenuItem("＋ New Project");
        itemNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemNew.addActionListener(e -> onNewProject());
        JMenuItem itemHome = createStyledMenuItem("Home");
        itemHome.addActionListener(e -> showLandingPage());
        JMenuItem itemSimulator = createStyledMenuItem("Network Simulator");
        itemSimulator.addActionListener(e -> showSimulatorPage());
        JMenuItem itemHistory = createStyledMenuItem("📂 Saved Topologies & History");
        itemHistory.addActionListener(e -> showHistoryPage());
        JMenuItem itemSimHistory = createStyledMenuItem("📊 Simulation Run History");
        itemSimHistory.addActionListener(e -> showSimulationHistoryPage());
        JMenuItem itemTechStack = createStyledMenuItem("Tech Stack & Architecture");
        itemTechStack.addActionListener(e -> showTechStackPage());
        JMenuItem itemAboutFile = createStyledMenuItem("About NetRoute");
        itemAboutFile.addActionListener(e -> showAboutPage());
        JMenuItem itemSave = createStyledMenuItem("💾 Save to Database...");
        itemSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        itemSave.addActionListener(e -> onSave());
        JMenuItem itemReset = createStyledMenuItem("Reset Sample Network");
        itemReset.addActionListener(e -> onResetNetwork());
        JMenuItem itemExit = createStyledMenuItem("Exit");
        itemExit.addActionListener(e -> System.exit(0));

        menuFile.add(itemNew);
        menuFile.addSeparator();
        menuFile.add(itemHome);
        menuFile.add(itemSimulator);
        menuFile.add(itemHistory);
        menuFile.add(itemSimHistory);
        menuFile.add(itemSave);
        menuFile.add(itemTechStack);
        menuFile.add(itemAboutFile);
        menuFile.addSeparator();
        menuFile.add(itemReset);
        menuFile.addSeparator();
        menuFile.add(itemExit);

        JMenu menuSim = createStyledMenu("Simulation");
        JMenuItem itemRandomize = createStyledMenuItem("Randomize Link Costs");
        itemRandomize.addActionListener(e -> onRandomizeLinkCosts());
        JMenuItem itemStep = createStyledMenuItem("Step Simulation (1 Tick)");
        itemStep.addActionListener(e -> onStepSimulation());
        JMenuItem itemSimHistory2 = createStyledMenuItem("📊 View Simulation History...");
        itemSimHistory2.addActionListener(e -> showSimulationHistoryPage());
        JMenuItem itemClearLog = createStyledMenuItem("Clear Log");
        itemClearLog.addActionListener(e -> {
            if (logArea != null) {
                logArea.setText("");
            }
        });
        menuSim.add(itemRandomize);
        menuSim.add(itemStep);
        menuSim.add(itemSimHistory2);
        menuSim.add(itemClearLog);

        JMenu menuHelp = createStyledMenu("Help");
        JMenuItem itemAbout = createStyledMenuItem("About NetRoute");
        itemAbout.addActionListener(e -> showAboutPage());
        menuHelp.add(itemAbout);

        menuBar.add(menuFile);
        menuBar.add(menuSim);
        menuBar.add(menuHelp);
        setJMenuBar(menuBar);
    }

    private JMenu createStyledMenu(String title) {
        JMenu menu = new JMenu(title);
        menu.setFont(Theme.FONT_BODY);
        menu.setForeground(Theme.TEXT_PRIMARY);
        return menu;
    }

    private JMenuItem createStyledMenuItem(String title) {
        JMenuItem item = new JMenuItem(title);
        item.setFont(Theme.FONT_BODY);
        return item;
    }

    // ==========================================
    // Navigation
    // ==========================================

    public void showLandingPage() {
        cardLayout.show(rootPanel, CARD_LANDING);
        this.currentCard = CARD_LANDING;
    }

    public void showSimulatorPage() {
        cardLayout.show(rootPanel, CARD_SIMULATOR);
        this.currentCard = CARD_SIMULATOR;
        if (topologyPanel != null) {
            topologyPanel.repaint();
        }
    }

    public void showTechStackPage() {
        cardLayout.show(rootPanel, CARD_TECH_STACK);
        this.currentCard = CARD_TECH_STACK;
    }

    public void showAboutPage() {
        cardLayout.show(rootPanel, CARD_ABOUT);
        this.currentCard = CARD_ABOUT;
    }

    public void showHistoryPage() {
        cardLayout.show(rootPanel, CARD_HISTORY);
        this.currentCard = CARD_HISTORY;
        if (historyPage != null) {
            historyPage.refresh();
        }
    }

    public void showSimulationHistoryPage() {
        cardLayout.show(rootPanel, CARD_SIMULATION_HISTORY);
        this.currentCard = CARD_SIMULATION_HISTORY;
        if (simulationHistoryPage != null) {
            simulationHistoryPage.refresh();
        }
    }

    public String getCurrentCard() {
        return currentCard;
    }

    public SimulationHistoryPage getSimulationHistoryPage() {
        return simulationHistoryPage;
    }

    @Override
    public void onLaunchSimulator() {
        showSimulatorPage();
    }

    @Override
    public void onShowTechStack() {
        showTechStackPage();
    }

    @Override
    public void onShowAbout() {
        showAboutPage();
    }

    @Override
    public void onShowHistory() {
        showHistoryPage();
    }

    @Override
    public void onShowSimulationHistory() {
        showSimulationHistoryPage();
    }

    private void setStatusBadge(String text, Color color) {
        if (lblStatusBadge != null) {
            lblStatusBadge.setText("  ● " + text + "  ");
            lblStatusBadge.setForeground(color);
        }
    }

    // ==========================================
    // ControlPanel.Listener Implementations
    // ==========================================

    @Override
    public void onFindRoute(String sourceId, String destinationId) {
        topologyPanel.setSelectedEndpoints(sourceId, destinationId);

        if (!network.containsNode(sourceId) || !network.containsNode(destinationId)) {
            topologyPanel.clearHighlightedRoute();
            controlPanel.setRouteResult("Invalid endpoint: node does not exist.", false);
            log(String.format("[Dijkstra Error] Unknown endpoints (%s -> %s).", sourceId, destinationId));
            return;
        }

        Optional<Route> routeOpt = router.findRoute(network, sourceId, destinationId);
        if (routeOpt.isPresent()) {
            Route route = routeOpt.get();
            topologyPanel.setHighlightedRoute(route);
            String pathStr = String.join(" -> ", route.getNodeIds());
            String summary = String.format("Route: %s (Total Cost: %.1f)", pathStr, route.getTotalCost());
            controlPanel.setRouteResult(summary, true);

            log(String.format("[Dijkstra] Shortest path %s -> %s: %s | Cost: %.1f | Hops: %d",
                    sourceId, destinationId, pathStr, route.getTotalCost(), route.getLinks().size()));
            setStatusBadge("Route Found", Theme.ACCENT_CYAN);
        } else {
            topologyPanel.clearHighlightedRoute();
            controlPanel.setRouteResult("No active route found between " + sourceId + " and " + destinationId, false);
            log(String.format("[Dijkstra] No route exists from %s to %s. A link may be inactive or partitioned.",
                    sourceId, destinationId));
            setStatusBadge("No Route", Theme.STATUS_ERROR);
        }
    }

    @Override
    public void onSendPacket(String sourceId, String destinationId) {
        if (sourceId == null || destinationId == null || !network.containsNode(sourceId) || !network.containsNode(destinationId)) {
            topologyPanel.clearHighlightedRoute();
            controlPanel.setRouteResult("Select valid source and destination nodes.", false);
            log(String.format("[Packet Error] Unknown or missing endpoints (%s -> %s).", sourceId, destinationId));
            setStatusBadge("Invalid Endpoints", Theme.STATUS_ERROR);
            return;
        }

        this.currentRunSaved = false;
        topologyPanel.setSelectedEndpoints(sourceId, destinationId);
        setStatusBadge("Transmitting", Theme.STATUS_WARNING);

        Packet packet = engine.submitPacket(sourceId, destinationId, 1);
        log(String.format("[Packet Submit] %s created from %s -> %s at tick %d.",
                packet.getId(), sourceId, destinationId, packet.getCreatedAt()));

        // Run simulation to completion
        engine.run();

        if (packet.getRoute().isPresent()) {
            topologyPanel.setHighlightedRoute(packet.getRoute().get());
        }

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
            setStatusBadge("Delivered", Theme.STATUS_SUCCESS);
        } else {
            String reason = packet.getReason().orElse("No available route");
            log(String.format("[Packet %s] %s: %s", packet.getId(), packet.getStatus(), reason));
            controlPanel.setRouteResult(String.format("%s %s: %s", packet.getId(), packet.getStatus(), reason), false);
            setStatusBadge("Packet Failed", Theme.STATUS_ERROR);
        }

        statisticsPanel.updateStatistics(new NetworkStatistics(engine));
        topologyPanel.repaint();
    }

    @Override
    public void onStepSimulation() {
        this.currentRunSaved = false;
        long prevTick = engine.getCurrentTime();
        engine.step();
        long newTick = engine.getCurrentTime();

        log(String.format("[Simulation Step] Advanced tick %d -> %d. Idle: %b",
                prevTick, newTick, engine.isIdle()));

        setStatusBadge(engine.isIdle() ? "Simulation Ready" : "In Progress",
                engine.isIdle() ? Theme.STATUS_SUCCESS : Theme.STATUS_WARNING);

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

        engine.failLink(linkId);
        log(String.format("[Link Failed] Link %s (%s - %s) deactivated. Rerouting affected packets if any.",
                linkId, link.getSourceId(), link.getDestinationId()));

        controlPanel.populateLinks(network);
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));
        topologyPanel.repaint();
        setStatusBadge("Link Deactivated", Theme.STATUS_WARNING);
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
        setStatusBadge("Link Restored", Theme.STATUS_SUCCESS);
    }

    @Override
    public void onRandomizeLinkCosts() {
        markTopologyModified();
        graphManager.randomizeLinkCosts();
        controlPanel.populateLinks(network);
        topologyPanel.repaint();
        statisticsPanel.updateStatistics(new NetworkStatistics(engine));

        log("[Cost Randomization] All link costs randomized to positive integers (1-10). Active/inactive statuses preserved.");

        String src = controlPanel.getSelectedSource();
        String dst = controlPanel.getSelectedDestination();
        if (src != null && dst != null) {
            onFindRoute(src, dst);
        }
        setStatusBadge("Costs Randomized", Theme.ACCENT_CYAN);
    }

    @Override
    public void onAddNode(NodeType type) {
        try {
            NetworkNode node = graphManager.addDynamicNode(type);
            markTopologyModified();
            controlPanel.populate(network);
            topologyPanel.repaint();
            statisticsPanel.updateStatistics(new NetworkStatistics(engine));

            log(String.format("[Node Created] Added %s '%s' (%s) at (%.0f, %.0f). Initially unconnected.",
                    node.getType(), node.getId(), node.getName(), node.getX(), node.getY()));
            setStatusBadge("Node Added", Theme.STATUS_SUCCESS);
        } catch (Exception ex) {
            log("[Node Error] " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Node Creation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onConnectNodes(String sourceId, String destinationId, double cost) {
        try {
            NetworkLink link = graphManager.connectNodes(sourceId, destinationId, cost);
            markTopologyModified();
            engine.queueFor(link.getId());

            controlPanel.populateLinks(network);
            topologyPanel.repaint();
            statisticsPanel.updateStatistics(new NetworkStatistics(engine));

            log(String.format("[Link Created] Connected %s <-> %s with link %s (Cost: %.1f).",
                    sourceId, destinationId, link.getId(), link.getCost()));

            String src = controlPanel.getSelectedSource();
            String dst = controlPanel.getSelectedDestination();
            if (src != null && dst != null) {
                onFindRoute(src, dst);
            }
            setStatusBadge("Link Created", Theme.STATUS_SUCCESS);
        } catch (IllegalArgumentException ex) {
            log("[Connection Error] " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Link Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setSkipConfirmationForTesting(boolean skip) {
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
        markTopologyModified();

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
        setStatusBadge("Node Deleted", Theme.STATUS_ERROR);
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
        markTopologyModified();

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
        setStatusBadge("Link Deleted", Theme.STATUS_ERROR);
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
        setStatusBadge("Simulation Ready", Theme.STATUS_SUCCESS);
    }

    // ==========================================
    // New Project Implementation
    // ==========================================

    public boolean onNewProject() {
        return onNewProject(skipConfirmationForTesting ? testUnsavedChangesAction : UnsavedChangesAction.PROMPT);
    }

    public boolean onNewProject(UnsavedChangesAction requestedAction) {
        UnsavedChangesAction action = requestedAction != null ? requestedAction : UnsavedChangesAction.PROMPT;

        if (hasUnsavedChanges()) {
            if (action == UnsavedChangesAction.PROMPT && !GraphicsEnvironment.isHeadless() && isVisible()) {
                Object[] options = {"Save", "Discard", "Cancel"};
                int choice = JOptionPane.showOptionDialog(
                        this,
                        "You have unsaved changes in the current project.\n\n"
                                + "• Save — save current topology and simulation metrics before starting a new project.\n"
                                + "• Discard — discard all unsaved changes and open a blank project.\n"
                                + "• Cancel — return to the current project without changes.",
                        "Unsaved Changes",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0]
                );
                if (choice == 0) { // Save
                    action = UnsavedChangesAction.SAVE;
                } else if (choice == 1) { // Discard
                    action = UnsavedChangesAction.DISCARD;
                } else { // Cancel or closed
                    action = UnsavedChangesAction.CANCEL;
                }
            }

            if (action == UnsavedChangesAction.SAVE) {
                boolean saved = promptAndSave();
                if (!saved) {
                    log("[New Project Cancelled] Save was not completed; staying in current project.");
                    return false;
                }
                createFreshProject();
                return true;
            } else if (action == UnsavedChangesAction.DISCARD) {
                log("[New Project] Discarding unsaved changes.");
                createFreshProject();
                return true;
            } else {
                log("[New Project Cancelled] Operation cancelled by user.");
                return false;
            }
        }

        // No unsaved changes
        if (!skipConfirmationForTesting && !GraphicsEnvironment.isHeadless() && isVisible() && network != null && network.nodeCount() > 0) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Start a fresh network project?\nThe current canvas will be cleared.",
                    "New Project",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                log("[New Project Cancelled] New project cancelled by user.");
                return false;
            }
        }

        createFreshProject();
        return true;
    }

    public void createFreshProject() {
        Network emptyNetwork = new Network();
        setNetwork(emptyNetwork);

        this.activeTopologyId = null;
        this.activeTopologyName = "Untitled Network";
        this.activeTopologyDescription = "";
        this.topologyModified = false;
        this.currentRunSaved = false;
        this.lastSavedRunId = null;

        if (this.topologyPanel != null) {
            this.topologyPanel.clearHighlightedRoute();
            this.topologyPanel.setSelectedEndpoints(null, null);
            this.topologyPanel.repaint();
        }

        if (this.controlPanel != null) {
            this.controlPanel.populate(this.network);
            this.controlPanel.setRouteResult("New project ready. Add nodes to begin.", true);
        }

        if (this.statisticsPanel != null) {
            this.statisticsPanel.updateStatistics(new NetworkStatistics(this.engine));
        }

        showSimulatorPage();
        setStatusBadge("New Project Ready", Theme.STATUS_SUCCESS);
        log("--------------------------------------------------");
        log("[New Project] Created fresh blank network project. Canvas is empty.");
        log("Ready. Use 'Add Node' in the control panel to add computers, routers, or servers.");
    }

    public boolean hasUnsavedChanges() {
        if (isSimulationActivityUnsaved()) {
            return true;
        }
        if (topologyModified) {
            return true;
        }
        if (activeTopologyId == null && network != null && network.nodeCount() > 0
                && !"Sample Campus Network".equals(activeTopologyName)) {
            return true;
        }
        return false;
    }

    public JButton getBtnNewProject() {
        return btnNewProject;
    }

    public boolean triggerNewProject() {
        return onNewProject();
    }

    public UnsavedChangesAction getTestUnsavedChangesAction() {
        return testUnsavedChangesAction;
    }

    public void setTestUnsavedChangesAction(UnsavedChangesAction testUnsavedChangesAction) {
        this.testUnsavedChangesAction = testUnsavedChangesAction;
    }

    // ==========================================
    // Unified Save Implementation
    // ==========================================

    @Override
    public void onSave() {
        promptAndSave();
    }

    @Override
    public void onSaveTopology() {
        onSave();
    }

    public boolean promptAndSave() {
        if (storageService == null || !storageService.isDatabaseAvailable()) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot connect to MySQL database.\n\n"
                                + "Please verify that MySQL server is running on localhost:3306\n"
                                + "and credentials in 'db.properties' are correct.\n"
                                + "(Run DatabaseConnection diagnostic tool to test)",
                        "Database Connection Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            log("[Database Error] Save aborted: MySQL database is unreachable.");
            return false;
        }

        if (network == null || network.nodeCount() == 0) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot save an empty network.\nPlease add at least one node before saving.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE
                );
            }
            log("[Save Aborted] Cannot save an empty network topology.");
            return false;
        }

        if (activeTopologyId != null && activeTopologyId > 0) {
            // Case A: Existing topology loaded from History or previously saved
            if (!skipConfirmationForTesting && !GraphicsEnvironment.isHeadless() && isVisible()) {
                String confirmMsg = "Topology '" + activeTopologyName + "' (ID: #" + activeTopologyId + ") is already saved in the database.\n\n"
                        + "Do you want to overwrite it with the current network configuration in MySQL?";
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        confirmMsg,
                        "Confirm Overwrite Topology",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    log("[Save Cancelled] Overwrite of topology '" + activeTopologyName + "' cancelled by user.");
                    return false;
                }
            }
            return executeUnifiedSave(activeTopologyId, activeTopologyName, activeTopologyDescription, true, null);
        } else {
            // Case B: New unsaved topology
            return promptAndSaveNewTopology(null, null);
        }
    }

    public boolean promptAndSaveNewTopology(String initialName, String initialDescription) {
        if (storageService == null || !storageService.isDatabaseAvailable()) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot connect to MySQL database.\n\n"
                                + "Please verify that MySQL server is running on localhost:3306\n"
                                + "and credentials in 'db.properties' are correct.",
                        "Database Connection Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            log("[Database Error] Save aborted: MySQL database is unreachable.");
            return false;
        }

        if (network == null || network.nodeCount() == 0) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot save an empty network.\nPlease add at least one node before saving.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE
                );
            }
            log("[Save Aborted] Cannot save an empty network topology.");
            return false;
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel lblName = new JLabel("Topology Name (required):");
        lblName.setFont(Theme.FONT_BODY_BOLD);
        String defaultName = (initialName != null && !initialName.isBlank())
                ? initialName
                : ((activeTopologyName != null && !activeTopologyName.isBlank()
                    && !activeTopologyName.contains("(Deleted")
                    && !activeTopologyName.equals("Untitled Network"))
                ? activeTopologyName : "My Network");
        JTextField txtName = new JTextField(defaultName, 22);
        Theme.styleTextField(txtName);

        JLabel lblDesc = new JLabel("Description (optional):");
        lblDesc.setFont(Theme.FONT_BODY_BOLD);
        JTextField txtDesc = new JTextField(initialDescription != null ? initialDescription : activeTopologyDescription, 22);
        Theme.styleTextField(txtDesc);

        panel.add(lblName);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(txtName);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(lblDesc);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(txtDesc);

        if (!skipConfirmationForTesting && !GraphicsEnvironment.isHeadless() && isVisible()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Save Network Topology to Database",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                log("[Save Cancelled] Save topology operation cancelled by user.");
                return false;
            }
        }

        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Topology name cannot be blank. Please enter a valid name.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return promptAndSaveNewTopology(null, desc);
        }

        return executeUnifiedSave(null, name, desc, false, null);
    }

    public boolean executeUnifiedSave(
            Integer targetTopologyId,
            String name,
            String description,
            boolean overwriteIfExists,
            String runNotes) {
        if (storageService == null || !storageService.isDatabaseAvailable()) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot connect to MySQL database.\n\nPlease verify MySQL is running and db.properties is valid.",
                        "Database Connection Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            log("[Database Error] Save aborted: MySQL database is unreachable.");
            return false;
        }

        if (network == null || network.nodeCount() == 0) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cannot save an empty network.\nPlease add at least one node to the canvas before saving.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE
                );
            }
            log("[Database Error] Save aborted: Cannot save an empty network.");
            return false;
        }

        boolean hasUnsavedRun = isSimulationActivityUnsaved();
        NetworkStatistics statsToRecord = hasUnsavedRun ? new NetworkStatistics(engine) : null;
        long ticks = hasUnsavedRun ? engine.getCurrentTime() : 0L;
        String safeNotes = (runNotes != null) ? runNotes.trim() : "";
        if (safeNotes.length() > 255) {
            safeNotes = safeNotes.substring(0, 255);
        }

        try {
            UnifiedSaveResult result = storageService.saveUnified(
                    targetTopologyId,
                    name,
                    description,
                    network,
                    overwriteIfExists,
                    statsToRecord,
                    ticks,
                    safeNotes
            );

            // Update state ONLY after successful commit
            this.activeTopologyId = result.getTopologyId();
            this.activeTopologyName = result.getTopologyName();
            this.activeTopologyDescription = (description != null) ? description.trim() : "";
            this.topologyModified = false;

            if (result.hasSimulationRun()) {
                this.currentRunSaved = true;
                this.lastSavedRunId = result.getSimulationRunId();
            }

            String actionStr = result.isCreated() ? "created as new record" : "updated existing record";
            StringBuilder sb = new StringBuilder();
            sb.append("Save Successful!\n\n");
            sb.append("• Topology: '").append(result.getTopologyName()).append("' (ID: #").append(result.getTopologyId()).append(")\n");
            sb.append("  Status: Successfully ").append(actionStr).append(" (").append(network.nodeCount()).append(" nodes, ").append(network.linkCount()).append(" links)\n\n");

            if (result.hasSimulationRun()) {
                sb.append("• Simulation Run: Recorded as Run #").append(result.getSimulationRunId()).append("\n");
                sb.append("  Metrics: ").append(statsToRecord.getTotalPackets()).append(" packets (")
                  .append(statsToRecord.getDeliveredCount()).append(" delivered, ratio: ")
                  .append(String.format("%.1f%%", statsToRecord.getDeliveryRatio())).append("), ")
                  .append(ticks).append(" ticks");
            } else if (hasSimulationActivity()) {
                sb.append("• Simulation Run: Current simulation state was previously saved (Run ID: #")
                  .append(lastSavedRunId != null ? lastSavedRunId : "N/A")
                  .append("). No duplicate run inserted.");
            } else {
                sb.append("• Simulation Run: No simulation activity to record.");
            }

            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(this, sb.toString(), "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            }

            log(String.format("[Database Save] Topology '%s' (ID #%d) %s.%s",
                    result.getTopologyName(),
                    result.getTopologyId(),
                    actionStr,
                    result.hasSimulationRun() ? " Simulation run #" + result.getSimulationRunId() + " recorded." : ""
            ));

            setStatusBadge("Saved (ID #" + result.getTopologyId() + ")", Theme.STATUS_SUCCESS);

            if (historyPage != null) {
                historyPage.refresh();
            }
            return true;

        } catch (DuplicateTopologyNameException e) {
            if (skipConfirmationForTesting) {
                log("[Duplicate Name] Test mode detected conflict for: " + name);
                return false;
            }
            Object[] options = {"Overwrite", "Rename", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "A saved topology named '" + name + "' already exists in the database.\n\n"
                            + "Choose an action:\n"
                            + "• Overwrite — replaces nodes and links, keeping the existing ID and simulation history.\n"
                            + "• Rename — enter another unique name.\n"
                            + "• Cancel — discard this save operation.",
                    "Topology Name Conflict",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]
            );

            if (choice == 0) { // Overwrite
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to overwrite topology '" + name + "'?\n"
                                + "Existing nodes and links in database will be replaced.",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    log("[Save Cancelled] Overwrite of topology '" + name + "' cancelled by user.");
                    return false;
                }
                return executeUnifiedSave(e.getExistingTopologyId(), name, description, true, runNotes);
            } else if (choice == 1) { // Rename
                return promptAndSaveNewTopology(name + "_copy", description);
            } else { // Cancel
                log("[Save Cancelled] Save cancelled due to name conflict: " + name);
                return false;
            }
        } catch (Exception e) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to save to MySQL database:\n" + e.getMessage()
                                + "\n\nCurrent in-memory network and simulation state are preserved. You may retry.",
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            log("[Database Error] Save failed: " + e.getMessage());
            setStatusBadge("Save Error", Theme.STATUS_ERROR);
            return false;
        }
    }

    public boolean promptAndSaveTopology(String initialName, String initialDescription) {
        return promptAndSaveNewTopology(initialName, initialDescription);
    }

    public boolean executeSaveTopology(String name, String description, boolean overwrite) {
        return executeUnifiedSave(null, name, description, overwrite, null);
    }

    public boolean handleLoadTopology(int id, String name) {
        if (!skipConfirmationForTesting) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to load topology '" + name + "' (ID: " + id + ")?\n\n"
                            + "This will replace the current network canvas and reset active packets and statistics.",
                    "Confirm Load Topology",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                log("[Load Cancelled] Loading of topology '" + name + "' cancelled by user.");
                return false;
            }
        }

        try {
            Optional<com.netroute.repository.SavedTopology> loadedOpt = storageService.loadTopology(id);
            if (loadedOpt.isEmpty()) {
                if (!skipConfirmationForTesting) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Topology with ID " + id + " was not found in the database.",
                            "Load Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                log("[Database Error] Topology ID " + id + " not found.");
                setStatusBadge("Load Error", Theme.STATUS_ERROR);
                return false;
            }

            com.netroute.repository.SavedTopology saved = loadedOpt.get();
            setNetwork(saved.getNetwork());
            this.activeTopologyId = id;
            this.activeTopologyName = saved.getName();
            this.activeTopologyDescription = saved.getDescription();
            this.topologyModified = false;
            this.currentRunSaved = false;
            this.lastSavedRunId = null;

            showSimulatorPage();

            String msg = String.format("Topology '%s' loaded successfully!\nNodes: %d | Links: %d",
                    saved.getName(), this.network.nodeCount(), this.network.linkCount());
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        msg,
                        "Topology Loaded",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            log(String.format("[Database Load] Topology '%s' (ID: %d) successfully loaded (%d nodes, %d links).",
                    saved.getName(), id, this.network.nodeCount(), this.network.linkCount()));
            setStatusBadge("Topology Loaded", Theme.STATUS_SUCCESS);
            return true;
        } catch (Exception e) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to load topology from database:\n" + e.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            log("[Database Error] Failed to load topology ID " + id + ": " + e.getMessage());
            setStatusBadge("Load Error", Theme.STATUS_ERROR);
            return false;
        }
    }

    public boolean handleDeleteTopology(int id, String name) {
        if (!skipConfirmationForTesting) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to permanently delete topology '" + name + "' (ID: " + id + ")?\n\n"
                            + "• All associated nodes and links will be removed from the database.\n"
                            + "• Past simulation run history and metrics will be preserved.\n\n"
                            + "This action cannot be undone. Do you wish to proceed?",
                    "Confirm Delete Topology",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) {
                log("[Delete Cancelled] Deletion of topology '" + name + "' cancelled by user.");
                return false;
            }
        }

        try {
            boolean deleted = storageService.deleteTopologyPermanently(id);
            if (deleted) {
                if (this.activeTopologyId != null && this.activeTopologyId.equals(id)) {
                    this.activeTopologyId = null;
                    this.activeTopologyName = name + " (Deleted from DB)";
                }
                log(String.format("[Database Delete] Topology '%s' (ID: %d) permanently deleted from database.", name, id));
                setStatusBadge("Topology Deleted", Theme.STATUS_WARNING);
                if (historyPage != null) {
                    historyPage.refresh();
                }
                if (!skipConfirmationForTesting) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Topology '" + name + "' (ID: " + id + ") has been permanently deleted from the database.",
                            "Topology Deleted",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
                return true;
            } else {
                if (!skipConfirmationForTesting) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Topology '" + name + "' (ID: " + id + ") could not be deleted or was not found.",
                            "Delete Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                log("[Database Warning] Topology ID " + id + " not found for deletion.");
                return false;
            }
        } catch (Exception e) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to delete topology from database:\n" + e.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            log("[Database Error] Failed to delete topology ID " + id + ": " + e.getMessage());
            setStatusBadge("Delete Error", Theme.STATUS_ERROR);
            return false;
        }
    }

    private void markTopologyModified() {
        this.topologyModified = true;
    }

    public boolean promptAndSaveSimulationRun(String initialNotes) {
        if (storageService == null) {
            log("[Database Error] Storage service not initialized.");
            return false;
        }

        if (engine.getPackets().isEmpty() && engine.getCurrentTime() == 0) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "No simulation activity to save.\nPlease send packets or step the simulation first.",
                        "No Simulation Activity",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
            log("[Simulation] Save run skipped: no simulation activity recorded.");
            return false;
        }

        if (currentRunSaved) {
            String msg = "The current simulation run has already been saved to the database"
                    + (lastSavedRunId != null ? " (Run ID: #" + lastSavedRunId + ")" : "")
                    + ".\n\nTo save a new run, send more packets or advance simulation ticks.";
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        msg,
                        "Run Already Saved",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
            log("[Simulation] Duplicate save prevented: run was already saved (Run ID: " + lastSavedRunId + ").");
            return false;
        }

        String notes = initialNotes != null ? initialNotes : "";
        if (!skipConfirmationForTesting) {
            NetworkStatistics currentStats = new NetworkStatistics(engine);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            JLabel lblInfo = new JLabel(String.format(
                    "<html><b>Topology:</b> %s %s<br/>"
                            + "<b>Simulation Ticks:</b> %d<br/>"
                            + "<b>Packets:</b> %d (Delivered: %d, Failed: %d, Dropped: %d)<br/>"
                            + "<b>Delivery Ratio:</b> %.1f%% &nbsp;|&nbsp; <b>Avg Delay:</b> %.2f ticks<br/>"
                            + "<b>Failed Links:</b> %d &nbsp;|&nbsp; <b>Reroutes:</b> %d</html>",
                    activeTopologyName != null ? activeTopologyName : "Unsaved Network",
                    activeTopologyId != null ? "(ID #" + activeTopologyId + ")" : "(Unsaved)",
                    engine.getCurrentTime(),
                    currentStats.getTotalPackets(),
                    currentStats.getDeliveredCount(),
                    currentStats.getFailedCount(),
                    currentStats.getDroppedCount(),
                    currentStats.getDeliveryRatio(),
                    currentStats.getAverageDeliveryDelay(),
                    network.countInactiveLinks(),
                    currentStats.getTotalReroutes()
            ));
            lblInfo.setFont(Theme.FONT_BODY);

            JLabel lblNotes = new JLabel("Run Notes / Scenario Description (optional):");
            lblNotes.setFont(Theme.FONT_BODY_BOLD);
            lblNotes.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));

            JTextField txtNotes = new JTextField(notes, 24);
            Theme.styleTextField(txtNotes);

            panel.add(lblInfo);
            panel.add(lblNotes);
            panel.add(txtNotes);

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Finish & Save Simulation Run to Database",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (choice != JOptionPane.OK_OPTION) {
                log("[Simulation] Save run cancelled by user.");
                return false;
            }
            notes = txtNotes.getText().trim();
        }

        return executeFinishAndSaveRun(notes);
    }

    public boolean executeFinishAndSaveRun(String notes) {
        if (storageService == null) {
            log("[Database Error] Storage service not initialized.");
            return false;
        }

        if (engine.getPackets().isEmpty() && engine.getCurrentTime() == 0) {
            log("[Simulation] Save run skipped: no simulation activity recorded.");
            return false;
        }

        if (currentRunSaved) {
            log("[Simulation] Duplicate save prevented: run was already saved (Run ID: " + lastSavedRunId + ").");
            return false;
        }

        NetworkStatistics stats = new NetworkStatistics(engine);
        long ticks = engine.getCurrentTime();
        Integer topId = activeTopologyId;
        String topName = (activeTopologyName != null && !activeTopologyName.trim().isEmpty())
                ? activeTopologyName.trim()
                : "Unsaved Network";

        String safeNotes = notes != null ? notes.trim() : "";
        if (safeNotes.length() > 255) {
            safeNotes = safeNotes.substring(0, 255);
        }

        try {
            int runId = storageService.recordSimulationRun(topId, topName, stats, ticks, safeNotes);
            if (runId > 0) {
                this.currentRunSaved = true;
                this.lastSavedRunId = runId;

                String successMsg = String.format(
                        "Simulation run successfully recorded to MySQL!\n\n"
                                + "Run ID: #%d\n"
                                + "Topology: %s %s\n"
                                + "Simulation Ticks: %d\n"
                                + "Packets: %d (Delivered: %d, Delivery Ratio: %.1f%%)\n"
                                + "Avg Delay: %.2f ticks | Reroutes: %d",
                        runId,
                        topName,
                        topId != null ? "(ID #" + topId + ")" : "(Unsaved)",
                        ticks,
                        stats.getTotalPackets(),
                        stats.getDeliveredCount(),
                        stats.getDeliveryRatio(),
                        stats.getAverageDeliveryDelay(),
                        stats.getTotalReroutes()
                );

                if (!skipConfirmationForTesting) {
                    JOptionPane.showMessageDialog(
                            this,
                            successMsg,
                            "Simulation Run Saved",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }

                log(String.format("[Database] Simulation run #%d recorded for topology '%s' (Packets: %d, Delivered: %d, Delivery Ratio: %.1f%%).",
                        runId, topName, stats.getTotalPackets(), stats.getDeliveredCount(), stats.getDeliveryRatio()));
                setStatusBadge("Run #" + runId + " Saved", Theme.STATUS_SUCCESS);
                return true;
            } else {
                throw new SQLException("Database returned non-positive run ID: " + runId);
            }
        } catch (Exception e) {
            if (!skipConfirmationForTesting) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to record simulation run to MySQL database:\n" + e.getMessage()
                                + "\n\nCurrent in-memory statistics are preserved. You may retry after verifying MySQL connection.",
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            log("[Database Error] Failed to record simulation run: " + e.getMessage());
            setStatusBadge("Save Run Failed", Theme.STATUS_ERROR);
            return false;
        }
    }

    public Integer getActiveTopologyId() {
        return activeTopologyId;
    }

    public void setActiveTopologyId(Integer activeTopologyId) {
        this.activeTopologyId = activeTopologyId;
    }

    public String getActiveTopologyName() {
        return activeTopologyName;
    }

    public void setActiveTopologyName(String activeTopologyName) {
        this.activeTopologyName = activeTopologyName;
    }

    public boolean isCurrentRunSaved() {
        return currentRunSaved;
    }

    public Integer getLastSavedRunId() {
        return lastSavedRunId;
    }

    public void setLastSavedRunId(Integer lastSavedRunId) {
        this.lastSavedRunId = lastSavedRunId;
    }

    public void setCurrentRunSaved(boolean currentRunSaved) {
        this.currentRunSaved = currentRunSaved;
    }

    public String getActiveTopologyDescription() {
        return activeTopologyDescription;
    }

    public void setActiveTopologyDescription(String activeTopologyDescription) {
        this.activeTopologyDescription = activeTopologyDescription;
    }

    public boolean isTopologyModified() {
        return topologyModified;
    }

    public void setTopologyModified(boolean topologyModified) {
        this.topologyModified = topologyModified;
    }

    public boolean hasSimulationActivity() {
        return engine != null && (!engine.getPackets().isEmpty() || engine.getCurrentTime() > 0);
    }

    public boolean isSimulationActivityUnsaved() {
        return hasSimulationActivity() && !currentRunSaved;
    }

    public HistoryPage getHistoryPage() {
        return historyPage;
    }

    public boolean isSkipConfirmationForTesting() {
        return skipConfirmationForTesting;
    }

    public void setNetwork(Network network) {
        this.network = java.util.Objects.requireNonNull(network, "network is required");
        this.graphManager = new GraphManager(this.network);
        this.router = new DijkstraRouter();
        this.engine = new SimulationEngine(this.network, this.router);
        if (this.topologyPanel != null) {
            this.topologyPanel.setNetwork(this.network);
            this.topologyPanel.clearHighlightedRoute();
            this.topologyPanel.setSelectedEndpoints(null, null);
        }
        if (this.controlPanel != null) {
            this.controlPanel.populate(this.network);
            this.controlPanel.setRouteResult("", false);
        }
        if (this.statisticsPanel != null) {
            this.statisticsPanel.updateStatistics(new NetworkStatistics(this.engine));
        }
    }

    public GraphManager getGraphManager() {
        return graphManager;
    }

    public Network getNetwork() {
        return network;
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    public DijkstraRouter getRouter() {
        return router;
    }

    public TopologyStorageService getStorageService() {
        return storageService;
    }

    public void setStorageService(TopologyStorageService storageService) {
        this.storageService = storageService;
        if (this.historyPage != null) {
            this.historyPage.setStorageService(storageService);
        }
        if (this.simulationHistoryPage != null) {
            this.simulationHistoryPage.setStorageService(storageService);
        }
    }

    private void log(String message) {
        if (logArea == null) {
            return;
        }
        String timeStr = LocalTime.now().format(TIME_FORMAT);
        logArea.append("[" + timeStr + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showAboutDialog() {
        showAboutPage();
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            NetRouteFrame frame = new NetRouteFrame();
            frame.setVisible(true);
        });
    }
}

package com.netroute.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Dedicated Tech Stack and Architecture view for NetRoute.
 * Prominently presents the Object-Oriented Programming (OOP) concepts,
 * data structures, simulation engine, and technologies used across the project.
 */
public class TechStackPage extends JPanel {

    public interface NavigationHandler {
        void onBackToHome();
        void onLaunchSimulator();
    }

    private final NavigationHandler navHandler;

    public TechStackPage(NavigationHandler navHandler) {
        this.navHandler = navHandler;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_MAIN);

        // Fixed Top Header Bar
        add(createHeaderBar(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_MAIN);
        content.setBorder(BorderFactory.createEmptyBorder(12, 24, 32, 24));

        // 1. Title & Hero intro
        content.add(createIntroSection());

        // 2. Section B: Object-Oriented Programming (Most Prominent)
        content.add(createOopPillarsSection());

        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // 3. Section A & C: Core Language & Data Structures
        content.add(createCoreAndDsaSection());

        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // 4. Section D & E: UI/Testing & Simulation Concepts
        content.add(createUiAndSimulationSection());

        content.add(Box.createRigidArea(new Dimension(0, 28)));

        // 5. Bottom Navigation Controls
        content.add(createBottomNavSection());

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        // 6. Footer
        content.add(createFooter());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(Theme.BG_MAIN);
        scrollPane.getViewport().setBackground(Theme.BG_MAIN);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(12, 32, 12, 32)
        ));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        // Left Branding & Breadcrumb
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel() {
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

        JLabel lblSub = new JLabel("Tech Stack & Architecture");
        lblSub.setFont(Theme.FONT_BODY_BOLD);
        lblSub.setForeground(Theme.ACCENT_CYAN);

        left.add(logo);
        left.add(lblBrand);
        left.add(lblSep);
        left.add(lblSub);

        // Right Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        JButton btnBack = Theme.createButton("← Back to Home", Theme.ButtonStyle.SECONDARY);
        btnBack.setPreferredSize(new Dimension(135, 34));
        btnBack.addActionListener(e -> {
            if (navHandler != null) {
                navHandler.onBackToHome();
            }
        });

        JButton btnSim = Theme.createButton("Launch Simulator  ➔", Theme.ButtonStyle.PRIMARY);
        btnSim.setPreferredSize(new Dimension(165, 34));
        btnSim.addActionListener(e -> {
            if (navHandler != null) {
                navHandler.onLaunchSimulator();
            }
        });

        right.add(btnBack);
        right.add(btnSim);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createIntroSection() {
        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(28, 40, 16, 40));

        // Academic Badge
        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        badgePanel.setOpaque(false);
        JLabel badge = new JLabel("  CSL 202: INTRODUCTION TO OBJECT-ORIENTED PROGRAMMING  ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_ELEVATED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Theme.ACCENT_CYAN);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(Theme.FONT_SMALL);
        badge.setForeground(Theme.ACCENT_CYAN);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        badgePanel.add(badge);
        badgePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        hero.add(badgePanel);

        hero.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel lblTitle = new JLabel("Technology Stack & Architecture", SwingConstants.CENTER);
        lblTitle.setFont(Theme.FONT_HERO);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        hero.add(lblTitle);

        hero.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel lblDesc = new JLabel(
                "<html><div style='text-align: center; width: 720px;'>"
                        + "A comprehensive breakdown of the Object-Oriented Programming principles, data structures, "
                        + "discrete simulation algorithms, and Java technologies powering the NetRoute simulator."
                        + "</div></html>",
                SwingConstants.CENTER
        );
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDesc.setForeground(Theme.TEXT_SECONDARY);
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        hero.add(lblDesc);

        return hero;
    }

    /**
     * Prominently highlights the 4 pillars of OOP and related design concepts.
     */
    private JPanel createOopPillarsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        // Distinctive container card with accent cyan border
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                // Distinctive prominent accent border
                g2.setColor(Theme.ACCENT_CYAN);
                g2.setStroke(new java.awt.BasicStroke(2.0f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 22, 24));
        card.setMaximumSize(new Dimension(1080, 600));

        // Header inside OOP card
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel lblOopTitle = new JLabel("Object-Oriented Programming (Academic Focus)");
        lblOopTitle.setFont(Theme.FONT_TITLE);
        lblOopTitle.setForeground(Theme.ACCENT_CYAN);

        JLabel lblBadge = new JLabel("  PRIMARY EVALUATION PILLAR  ");
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblBadge.setForeground(Theme.TEXT_DARK);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(Theme.ACCENT_CYAN);
        lblBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        headerRow.add(lblOopTitle, BorderLayout.WEST);
        headerRow.add(lblBadge, BorderLayout.EAST);
        card.add(headerRow);

        JLabel lblSub = new JLabel("NetRoute is engineered specifically to demonstrate core OOP principles in an interactive software application:");
        lblSub.setFont(Theme.FONT_BODY);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        lblSub.setBorder(BorderFactory.createEmptyBorder(6, 0, 16, 0));
        card.add(lblSub);

        // 4 Core OOP Pillars Grid
        JPanel pillarsGrid = new JPanel(new GridLayout(2, 2, 16, 14));
        pillarsGrid.setOpaque(false);

        JPanel pnlAbstraction = createOopPillarTile(
                "1. Abstraction",
                "Abstract classes and interface contracts hide implementation complexity while defining clear APIs.",
                "• Abstract class NetworkNode defines shared node contract (ID, coordinates, getType()).\n"
                        + "• Listener interfaces (ControlPanel.Listener, LandingPage.Listener) decouple UI presentation from backend simulation logic."
        );

        JPanel pnlEncapsulation = createOopPillarTile(
                "2. Encapsulation",
                "Internal state is strictly guarded with private fields and controlled public accessors.",
                "• Model classes (NetworkLink, Packet, Route) maintain immutable properties and guarded mutations.\n"
                        + "• Graph adjacency lists and packet queues use defensive copies (Collections.unmodifiableMap, List.copyOf) to prevent unintended external tampering."
        );

        JPanel pnlInheritance = createOopPillarTile(
                "3. Inheritance",
                "Hierarchical class relationships foster code reuse and clean domain specialization.",
                "• Specialized domain nodes (Computer, Router, Server) extend abstract base class NetworkNode.\n"
                        + "• Swing desktop components extend standard library base classes (JFrame for NetRouteFrame, JPanel for custom canvases)."
        );

        JPanel pnlPolymorphism = createOopPillarTile(
                "4. Polymorphism",
                "Dynamic method dispatch and interchangeable object handling across the simulation engine.",
                "• Network topology processes any NetworkNode uniformly, delegating to specialized getType() implementations.\n"
                        + "• Custom Java2D rendering overrides paintComponent() across canvas panels.\n"
                        + "• NetRouteFrame polymorphically implements multiple listener interfaces."
        );

        pillarsGrid.add(pnlAbstraction);
        pillarsGrid.add(pnlEncapsulation);
        pillarsGrid.add(pnlInheritance);
        pillarsGrid.add(pnlPolymorphism);
        card.add(pillarsGrid);

        card.add(Box.createRigidArea(new Dimension(0, 16)));

        // Additional OOP Design & Implementation Concepts
        JPanel conceptsPanel = new JPanel(new GridLayout(1, 5, 8, 0));
        conceptsPanel.setOpaque(false);
        conceptsPanel.add(createMiniConceptTile("Classes & Objects", "Discrete models: Packet, NetworkLink, Route, QueueEntry"));
        conceptsPanel.add(createMiniConceptTile("Constructors", "State initialization & defensive boundary validation"));
        conceptsPanel.add(createMiniConceptTile("Method Overriding", "Polymorphic paintComponent, equals, and status handling"));
        conceptsPanel.add(createMiniConceptTile("Interfaces", "Clean event-driven listener callbacks decoupling UI from models"));
        conceptsPanel.add(createMiniConceptTile("Composition", "GraphManager composes Network; SimulationEngine composes Queues"));
        card.add(conceptsPanel);

        section.add(card);
        return section;
    }

    private JPanel createOopPillarTile(String title, String summary, String details) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(Theme.BG_INPUT);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSum = new JLabel("<html><div style='width: 380px;'>" + summary + "</div></html>");
        lblSum.setFont(Theme.FONT_BODY);
        lblSum.setForeground(Theme.ACCENT_CYAN);
        lblSum.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        JLabel lblDet = new JLabel("<html><div style='width: 380px; line-height: 1.3; font-size: 11px; color: #A7B5C4;'>"
                + details.replace("\n", "<br>") + "</div></html>");

        tile.add(lblTitle);
        tile.add(lblSum);
        tile.add(lblDet);
        return tile;
    }

    private JPanel createMiniConceptTile(String title, String desc) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(Theme.BG_ELEVATED);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblDesc = new JLabel("<html><div style='width: 140px; font-size: 10px; color: #A7B5C4;'>" + desc + "</div></html>");
        lblDesc.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        tile.add(lblTitle);
        tile.add(lblDesc);
        return tile;
    }

    private JPanel createCoreAndDsaSection() {
        JPanel section = new JPanel(new GridLayout(1, 2, 20, 0));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));
        section.setMaximumSize(new Dimension(1080, 220));

        // Card A: Core Language
        JPanel pnlCore = createStandardSectionCard("Core Programming Language", "⚡");
        pnlCore.add(createBulletItem("Java 21 (LTS Platform)", "NetRoute is built using modern Java 21 standard edition, taking advantage of strongly-typed object modeling, record types (Transmission), and modern collections."));
        pnlCore.add(createBulletItem("Pure Standard Library", "Runs on standard Java SE with zero third-party framework overhead (no Spring Boot or web engines), emphasizing pure OOP fundamentals."));
        pnlCore.add(createBulletItem("Java Collections Framework", "Extensive use of Map, HashMap, List, ArrayList, Set, HashSet, and Collections for graph modeling and event queues."));
        section.add(pnlCore);

        // Card C: Data Structures & Algorithms
        JPanel pnlDsa = createStandardSectionCard("Data Structures & Algorithms", "🧠");
        pnlDsa.add(createBulletItem("Graph Adjacency List", "Graph topology represented via Map<String, List<NetworkLink>> for fast O(1) node lookup and O(degree) outgoing edge traversal."));
        pnlDsa.add(createBulletItem("Dijkstra's Shortest Path", "Computes optimal routing paths using edge relaxation, path cost accumulation, and cycle avoidance."));
        pnlDsa.add(createBulletItem("Java PriorityQueue (Min-Heap)", "Utilizes java.util.PriorityQueue<QueueEntry> for O(log V) extract-min operations in Dijkstra routing."));
        pnlDsa.add(createBulletItem("Discrete Simulation Queues", "FIFO packet queues (ArrayDeque) buffering traffic on active links with capacity constraints."));
        section.add(pnlDsa);

        return section;
    }

    private JPanel createUiAndSimulationSection() {
        JPanel section = new JPanel(new GridLayout(1, 2, 20, 0));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));
        section.setMaximumSize(new Dimension(1080, 220));

        // Card D: Desktop UI & Testing
        JPanel pnlUi = createStandardSectionCard("Desktop UI & Automated Testing", "🖥️");
        pnlUi.add(createBulletItem("Java Swing & AWT", "Responsive desktop interface with custom Java2D vector rendering for nodes, links, and halo glows; CardLayout for seamless page navigation."));
        pnlUi.add(createBulletItem("JUnit 5 Test Suite", "Rigorous automated unit testing covering routing calculations, packet delivery, link failure recovery, and UI callbacks."));
        pnlUi.add(createBulletItem("Apache Maven", "Industry-standard project build, compilation, and automated test lifecycle management (pom.xml)."));
        section.add(pnlUi);

        // Card E: Simulation Concepts
        JPanel pnlSim = createStandardSectionCard("Network Simulation Concepts", "📡");
        pnlSim.add(createBulletItem("Discrete-Event Timing", "Tick-based discrete-event simulation tracking transmission delays, packet in-flight progression, and delivery ticks."));
        pnlSim.add(createBulletItem("Fault Injection & Rerouting", "Interactive link deactivation with live packet rerouting across alternative active network paths."));
        pnlSim.add(createBulletItem("Dynamic Network Management", "Runtime node and link creation, deletion with bidirectional adjacency cleanup, and cost randomization."));
        pnlSim.add(createBulletItem("Real-Time Metrics", "Accurate delivery ratios, routing costs, average transmission delays, and failure/drop accounting."));
        section.add(pnlSim);

        return section;
    }

    private JPanel createStandardSectionCard(String title, String emoji) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel lblTitle = new JLabel(emoji + "  " + title);
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(lblTitle);

        return card;
    }

    private JPanel createBulletItem(String heading, String description) {
        JPanel item = new JPanel(new BorderLayout(4, 2));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(3, 0, 5, 0));

        JLabel lblHead = new JLabel("• " + heading);
        lblHead.setFont(Theme.FONT_BODY_BOLD);
        lblHead.setForeground(Theme.ACCENT_CYAN);

        JLabel lblDesc = new JLabel("<html><div style='width: 380px; font-size: 11px; color: #A7B5C4;'>" + description + "</div></html>");
        lblDesc.setBorder(BorderFactory.createEmptyBorder(1, 12, 0, 0));

        item.add(lblHead, BorderLayout.NORTH);
        item.add(lblDesc, BorderLayout.CENTER);
        return item;
    }

    private JPanel createBottomNavSection() {
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navRow.setOpaque(false);
        navRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        JButton btnBack = Theme.createButton("← Back to Home", Theme.ButtonStyle.SECONDARY);
        btnBack.setFont(Theme.FONT_BODY_BOLD);
        btnBack.setPreferredSize(new Dimension(160, 42));
        btnBack.addActionListener(e -> {
            if (navHandler != null) {
                navHandler.onBackToHome();
            }
        });

        JButton btnSim = Theme.createButton("Launch Simulator  ➔", Theme.ButtonStyle.PRIMARY);
        btnSim.setFont(Theme.FONT_BODY_BOLD);
        btnSim.setPreferredSize(new Dimension(190, 42));
        btnSim.addActionListener(e -> {
            if (navHandler != null) {
                navHandler.onLaunchSimulator();
            }
        });

        navRow.add(btnBack);
        navRow.add(btnSim);
        return navRow;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(Theme.BG_MAIN);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel lblFoot = new JLabel("NetRoute | Network Traffic & Routing Simulator");
        lblFoot.setFont(Theme.FONT_SMALL);
        lblFoot.setForeground(Theme.TEXT_MUTED);

        footer.add(lblFoot);
        return footer;
    }
}

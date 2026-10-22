package com.netroute.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
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
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Dedicated About page for NetRoute presenting academic project background,
 * course information (CSL 202), and software engineering objectives.
 * <p>
 * Uses a left-aligned, responsive layout with consistent margins,
 * natural text wrapping, and unconstrained card heights for reliable scrolling.
 */
public class AboutPage extends JPanel {

    public interface NavigationHandler {
        void onBackToHome();
        void onLaunchSimulator();
    }

    /**
     * Scrollable panel that tracks viewport width to prevent horizontal overflow,
     * hardcoded offsets, and empty margins.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // Enforces exact viewport width matching to eliminate horizontal shifts/clipping
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private final NavigationHandler navHandler;

    public AboutPage(NavigationHandler navHandler) {
        this.navHandler = navHandler;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_MAIN);

        // Fixed Top Header Bar
        add(createHeaderBar(), BorderLayout.NORTH);

        // Scrollable content area with consistent 32px left/right margins matching the header
        ScrollablePanel scrollContent = new ScrollablePanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(Theme.BG_MAIN);
        scrollContent.setBorder(BorderFactory.createEmptyBorder(16, 32, 32, 32));

        // 1. Academic Project heading and subtitle (Left-aligned)
        scrollContent.add(createIntroSection());
        scrollContent.add(createVerticalSpacing(16));

        // 2. Course & Academic Details — four equally sized cards (Left-aligned)
        scrollContent.add(createCourseInfoCard());
        scrollContent.add(createVerticalSpacing(16));

        // 3. Project Description — one full-width card (Left-aligned)
        scrollContent.add(createProjectDescriptionCard());
        scrollContent.add(createVerticalSpacing(16));

        // 4. Core Learning Objectives & Implemented Features — responsive grid of six cards (Left-aligned)
        scrollContent.add(createHighlightsCard());
        scrollContent.add(createVerticalSpacing(24));

        // 5. Bottom action buttons — Back to Home and Launch Simulator (Left-aligned)
        scrollContent.add(createBottomNavSection());
        scrollContent.add(createVerticalSpacing(14));

        // 6. Footer (Left-aligned)
        scrollContent.add(createFooter());

        JScrollPane scrollPane = new JScrollPane(scrollContent);
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

        JLabel lblSub = new JLabel("About NetRoute");
        lblSub.setFont(Theme.FONT_BODY_BOLD);
        lblSub.setForeground(Theme.ACCENT_CYAN);

        left.add(logo);
        left.add(lblBrand);
        left.add(lblSep);
        left.add(lblSub);

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
        JPanel intro = new JPanel();
        intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));
        intro.setOpaque(false);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        intro.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        intro.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // Academic Badge (Left-aligned)
        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgePanel.setOpaque(false);
        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel badge = new JLabel("  COLLEGE ACADEMIC PROJECT  ") {
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
        badge.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        badgePanel.add(badge);
        intro.add(badgePanel);

        intro.add(Box.createRigidArea(new Dimension(0, 10)));

        // Heading (Left-aligned)
        JLabel lblTitle = new JLabel("About NetRoute", SwingConstants.LEFT);
        lblTitle.setFont(Theme.FONT_HERO);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        intro.add(lblTitle);

        intro.add(Box.createRigidArea(new Dimension(0, 6)));

        // Subtitle cleanly left-aligned with natural wrapping
        JLabel lblDesc = new JLabel(
                "<html><div style='font-size: 13px; line-height: 1.5; color: #A7B5C4;'>"
                        + "Interactive Network Traffic &amp; Routing Simulator<br>"
                        + "developed for CSL 202: Introduction to Object-Oriented Programming"
                        + "</div></html>",
                SwingConstants.LEFT
        );
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDesc.setForeground(Theme.TEXT_SECONDARY);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        intro.add(lblDesc);

        return intro;
    }

    private JPanel createCourseInfoCard() {
        JPanel card = createStandardCard();

        JLabel lblHeading = new JLabel("Course & Academic Details");
        lblHeading.setFont(Theme.FONT_SUBTITLE);
        lblHeading.setForeground(Theme.ACCENT_CYAN);
        lblHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        lblHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblHeading);

        // 4 equally sized tiles in a 1-row, 4-col GridLayout stretching across full card width
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(createCourseTile("Course Code", "CSL 202", Theme.ACCENT_CYAN));
        grid.add(createCourseTile("Course Name", "Introduction to Object-Oriented Programming", Theme.TEXT_PRIMARY));
        grid.add(createCourseTile("Project", "NetRoute — Network Traffic & Routing Simulator", Theme.TEXT_PRIMARY));
        grid.add(createCourseTile("Platform", "Java Desktop Application", Theme.ACCENT_BLUE));

        card.add(grid);
        return card;
    }

    private JPanel createCourseTile(String label, String value, Color valueColor) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(Theme.BG_INPUT);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(Theme.TEXT_MUTED);

        JLabel val = new JLabel("<html><div style='font-size: 12px; font-weight: bold; line-height: 1.35;'>" + value + "</div></html>");
        val.setForeground(valueColor);
        val.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        tile.add(lbl);
        tile.add(val);
        return tile;
    }

    private JPanel createProjectDescriptionCard() {
        JPanel card = createStandardCard();

        JLabel lblTitle = new JLabel("Project Description");
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblTitle);

        String paragraph1 = "NetRoute is a college-level Java desktop application developed as part of "
                + "CSL 202: Introduction to Object-Oriented Programming. "
                + "The project demonstrates the practical application of object-oriented programming principles through "
                + "an interactive network traffic and routing simulator.";

        String paragraph2 = "It allows users to construct network topologies, explore shortest-path routing, simulate packet "
                + "transmission, investigate link failures, and monitor network statistics. The project emphasizes modular class "
                + "design, reusable components, and the practical use of OOP concepts in software development.";

        // Natural word wrapping using JTextArea across the entire available card width
        JTextArea taP1 = new JTextArea(paragraph1);
        taP1.setLineWrap(true);
        taP1.setWrapStyleWord(true);
        taP1.setEditable(false);
        taP1.setOpaque(false);
        taP1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taP1.setForeground(Theme.TEXT_PRIMARY);
        taP1.setAlignmentX(Component.LEFT_ALIGNMENT);
        taP1.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JTextArea taP2 = new JTextArea(paragraph2);
        taP2.setLineWrap(true);
        taP2.setWrapStyleWord(true);
        taP2.setEditable(false);
        taP2.setOpaque(false);
        taP2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taP2.setForeground(Theme.TEXT_SECONDARY);
        taP2.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(taP1);
        card.add(taP2);
        return card;
    }

    private JPanel createHighlightsCard() {
        JPanel card = createStandardCard();

        JLabel lblTitle = new JLabel("Core Learning Objectives & Implemented Features");
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblTitle);

        // 6 features in a 2x3 grid, unconstrained height so all items are 100% visible
        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(createMiniHighlightTile("Interactive Topology", "Visual network graph with Computers, Routers, Servers, and mouse dragging"));
        grid.add(createMiniHighlightTile("Dijkstra Algorithm", "Optimal route calculation with PriorityQueue and edge relaxation"));
        grid.add(createMiniHighlightTile("Packet Simulation", "Discrete-event traffic simulation with FIFO queues and hop delays"));
        grid.add(createMiniHighlightTile("Fault Tolerance", "Live link failure injection with automatic alternative path rerouting"));
        grid.add(createMiniHighlightTile("Dynamic Network", "Runtime node and link addition, deletion, and cost randomization"));
        grid.add(createMiniHighlightTile("Real-Time Analytics", "Live metrics tracking delivery ratio, average delay, and event counters"));

        card.add(grid);
        return card;
    }

    private JPanel createMiniHighlightTile(String title, String desc) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(Theme.BG_INPUT);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblT = new JLabel("✔  " + title);
        lblT.setFont(Theme.FONT_BODY_BOLD);
        lblT.setForeground(Theme.ACCENT_CYAN);
        lblT.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea taD = new JTextArea(desc);
        taD.setLineWrap(true);
        taD.setWrapStyleWord(true);
        taD.setEditable(false);
        taD.setOpaque(false);
        taD.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        taD.setForeground(Theme.TEXT_SECONDARY);
        taD.setAlignmentX(Component.LEFT_ALIGNMENT);
        taD.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        tile.add(lblT);
        tile.add(taD);
        return tile;
    }

    private JPanel createBottomNavSection() {
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        navRow.setOpaque(false);
        navRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        navRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton btnBack = Theme.createButton("← Back to Home", Theme.ButtonStyle.SECONDARY);
        btnBack.setFont(Theme.FONT_BODY_BOLD);
        btnBack.setPreferredSize(new Dimension(160, 40));
        btnBack.addActionListener(e -> {
            if (navHandler != null) {
                navHandler.onBackToHome();
            }
        });

        JButton btnSim = Theme.createButton("Launch Simulator  ➔", Theme.ButtonStyle.PRIMARY);
        btnSim.setFont(Theme.FONT_BODY_BOLD);
        btnSim.setPreferredSize(new Dimension(190, 40));
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
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setBackground(Theme.BG_MAIN);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblFoot = new JLabel("NetRoute | Network Traffic & Routing Simulator");
        lblFoot.setFont(Theme.FONT_SMALL);
        lblFoot.setForeground(Theme.TEXT_MUTED);

        footer.add(lblFoot);
        return footer;
    }

    /**
     * Standard card container providing consistent styling, left alignment,
     * full width expansion, and unconstrained height.
     */
    private JPanel createStandardCard() {
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
        card.setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private Component createVerticalSpacing(int height) {
        Component box = Box.createRigidArea(new Dimension(0, height));
        if (box instanceof JComponent) {
            ((JComponent) box).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        return box;
    }
}

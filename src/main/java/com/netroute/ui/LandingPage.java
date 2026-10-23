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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Dedicated Landing Page for the NetRoute desktop application.
 * Welcomes the user with application branding, a hero section with primary call-to-actions
 * (Launch Simulator and Tech Stack), feature overview cards, and footer.
 */
public class LandingPage extends JPanel {

    public interface Listener {
        void onLaunchSimulator();
        void onShowTechStack();
        void onShowAbout();
        void onShowHistory();
        default void onShowSimulationHistory() {}
    }

    private final Listener listener;

    public LandingPage(Listener listener) {
        this.listener = listener;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_MAIN);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_MAIN);
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // 1. Header (NetRoute logo, app name, subtitle, About, Launch Simulator)
        content.add(createHeader());

        // Subtle divider
        content.add(createHorizontalDivider());

        // 2. Hero Section (Launch Simulator and Tech Stack buttons)
        content.add(createHeroSection());

        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // 3. Features Section
        content.add(createFeaturesSection());

        content.add(Box.createRigidArea(new Dimension(0, 30)));

        // 4. Footer
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

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(16, 32, 16, 32));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Left Branding: Logo Icon + App Name + Subtitle
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        // Custom drawn Network Logo Icon (28x28)
        JLabel logoIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw 3 connected nodes
                g2.setColor(Theme.ACCENT_BLUE);
                g2.drawLine(6, 20, 14, 7);
                g2.drawLine(14, 7, 22, 20);
                g2.drawLine(6, 20, 22, 20);

                // Center node
                g2.setColor(Theme.ACCENT_CYAN);
                g2.fillOval(10, 3, 8, 8);

                // Left & Right nodes
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.fillOval(2, 16, 8, 8);
                g2.fillOval(18, 16, 8, 8);

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(28, 26);
            }
        };

        JLabel lblTitle = new JLabel("NetRoute");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblDivider = new JLabel("|");
        lblDivider.setFont(Theme.FONT_TITLE);
        lblDivider.setForeground(Theme.TEXT_MUTED);

        JLabel lblSub = new JLabel("Network Traffic & Routing Simulator");
        lblSub.setFont(Theme.FONT_BODY);
        lblSub.setForeground(Theme.TEXT_SECONDARY);

        brandPanel.add(logoIcon);
        brandPanel.add(lblTitle);
        brandPanel.add(lblDivider);
        brandPanel.add(lblSub);

        // Right Navigation: About & Launch Simulator (Features removed as requested)
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        navPanel.setOpaque(false);

        JButton btnAboutNav = createNavButton("About");
        btnAboutNav.addActionListener(e -> {
            if (listener != null) {
                listener.onShowAbout();
            }
        });

        JButton btnHistoryNav = createNavButton("Topologies");
        btnHistoryNav.addActionListener(e -> {
            if (listener != null) {
                listener.onShowHistory();
            }
        });

        JButton btnSimHistoryNav = createNavButton("Run History");
        btnSimHistoryNav.addActionListener(e -> {
            if (listener != null) {
                listener.onShowSimulationHistory();
            }
        });

        JButton btnSimHeader = Theme.createButton("Launch Simulator", Theme.ButtonStyle.PRIMARY);
        btnSimHeader.setPreferredSize(new Dimension(145, 34));
        btnSimHeader.addActionListener(e -> {
            if (listener != null) {
                listener.onLaunchSimulator();
            }
        });

        navPanel.add(btnAboutNav);
        navPanel.add(btnHistoryNav);
        navPanel.add(btnSimHistoryNav);
        navPanel.add(Box.createRigidArea(new Dimension(8, 0)));
        navPanel.add(btnSimHeader);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(navPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createHeroSection() {
        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(45, 40, 30, 40));
        hero.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Eyebrow badge
        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        badgePanel.setOpaque(false);
        JLabel badge = new JLabel("  SIMULATION & ALGORITHM VISUALIZATION  ") {
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
        badgePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        hero.add(badgePanel);

        hero.add(Box.createRigidArea(new Dimension(0, 16)));

        // Headline
        JLabel lblHeadline = new JLabel("Understand Networks. Simulate Routes. Explore Possibilities.", SwingConstants.CENTER);
        lblHeadline.setFont(Theme.FONT_HERO);
        lblHeadline.setForeground(Theme.TEXT_PRIMARY);
        lblHeadline.setAlignmentX(Component.CENTER_ALIGNMENT);
        hero.add(lblHeadline);

        hero.add(Box.createRigidArea(new Dimension(0, 14)));

        // Description
        JLabel lblDesc = new JLabel(
                "<html><div style='text-align: center; width: 680px;'>"
                        + "Build network topologies, calculate shortest paths, simulate packet traffic, "
                        + "and investigate link failures in an interactive network environment."
                        + "</div></html>",
                SwingConstants.CENTER
        );
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblDesc.setForeground(Theme.TEXT_SECONDARY);
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        hero.add(lblDesc);

        hero.add(Box.createRigidArea(new Dimension(0, 28)));

        // CTA Buttons Row: Launch Simulator & Tech Stack (Replacing Explore Features)
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton btnLaunch = Theme.createButton("Launch Simulator  ➔", Theme.ButtonStyle.PRIMARY);
        btnLaunch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLaunch.setPreferredSize(new Dimension(190, 44));
        btnLaunch.addActionListener(e -> {
            if (listener != null) {
                listener.onLaunchSimulator();
            }
        });

        JButton btnTechStack = Theme.createButton("Tech Stack", Theme.ButtonStyle.SECONDARY);
        btnTechStack.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnTechStack.setPreferredSize(new Dimension(160, 44));
        btnTechStack.addActionListener(e -> {
            if (listener != null) {
                listener.onShowTechStack();
            }
        });

        btnRow.add(btnLaunch);
        btnRow.add(btnTechStack);
        hero.add(btnRow);

        return hero;
    }

    private JPanel createFeaturesSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setBorder(BorderFactory.createEmptyBorder(10, 45, 10, 45));

        // Section Title
        JLabel lblSection = new JLabel("Powerful Simulation Capabilities", SwingConstants.CENTER);
        lblSection.setFont(Theme.FONT_TITLE);
        lblSection.setForeground(Theme.ACCENT_CYAN);
        lblSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.add(lblSection);

        JLabel lblSub = new JLabel("Engineered for computer science students and network enthusiasts", SwingConstants.CENTER);
        lblSub.setFont(Theme.FONT_BODY);
        lblSub.setForeground(Theme.TEXT_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.add(lblSub);

        section.add(Box.createRigidArea(new Dimension(0, 22)));

        // 6 Feature Cards Grid
        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(1050, 340));

        grid.add(createFeatureCard(
                "🌐",
                "Network Topology",
                "Build and visualize connected computers, routers, and servers with draggable nodes and custom weighted links."
        ));

        grid.add(createFeatureCard(
                "⚡",
                "Shortest-Path Routing",
                "Explore Dijkstra's algorithm with Java PriorityQueue to find minimum-cost routes across active network links."
        ));

        grid.add(createFeatureCard(
                "📦",
                "Packet Simulation",
                "Observe discrete-event packet delivery, queuing, transit delays, and delivery failures in real time."
        ));

        grid.add(createFeatureCard(
                "🛠",
                "Dynamic Network Control",
                "Add or delete nodes and links, randomize link costs, and simulate real-world network expansions."
        ));

        grid.add(createFeatureCard(
                "📊",
                "Live Statistics",
                "Monitor delivery ratios, routing costs, average simulated delays, link failure incidents, and reroutes."
        ));

        grid.add(createFeatureCard(
                "🗄",
                "Database",
                "Stores and manages network simulation data using database integration.<br>Supports structured data persistence for future analysis and retrieval."
        ));

        section.add(grid);
        return section;
    }

    private JPanel createFeatureCard(String icon, String title, String description) {
        return createFeatureCard(icon, title, description, null);
    }

    private JPanel createFeatureCard(String icon, String title, String description, String badge) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        lblIcon.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);
        titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_SUBTITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        titlePanel.add(lblTitle);

        if (badge != null && !badge.isEmpty()) {
            titlePanel.add(Box.createRigidArea(new Dimension(8, 0)));
            JLabel lblBadge = new JLabel(badge) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.BG_ELEVATED);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(Theme.BORDER_FOCUS);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblBadge.setForeground(Theme.ACCENT_CYAN);
            lblBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            titlePanel.add(lblBadge);
        }

        JLabel lblDesc = new JLabel("<html><div style='width: 250px; line-height: 1.35;'>" + description + "</div></html>");
        lblDesc.setFont(Theme.FONT_BODY);
        lblDesc.setForeground(Theme.TEXT_SECONDARY);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblIcon);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(titlePanel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(lblDesc);

        return card;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(Theme.BG_MAIN);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lblFoot = new JLabel("NetRoute | Network Traffic & Routing Simulator");
        lblFoot.setFont(Theme.FONT_SMALL);
        lblFoot.setForeground(Theme.TEXT_MUTED);

        footer.add(lblFoot);
        return footer;
    }

    private JPanel createHorizontalDivider() {
        JPanel div = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.BORDER_SUBTLE);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        div.setPreferredSize(new Dimension(100, 1));
        div.setOpaque(false);
        return div;
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_BODY);
        btn.setForeground(Theme.TEXT_SECONDARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(Theme.ACCENT_CYAN);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(Theme.TEXT_SECONDARY);
            }
        });
        return btn;
    }
}

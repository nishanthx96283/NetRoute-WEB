package com.netroute.ui;

import com.netroute.repository.TopologySummary;
import com.netroute.service.TopologyStorageService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dedicated History page for NetRoute displaying saved network topologies from MySQL database.
 * <p>
 * Presents each saved topology with its name, description, node count, link count,
 * database ID, and timestamps, along with Load and Delete controls.
 */
public class HistoryPage extends JPanel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public interface Listener {
        void onBackToHome();
        void onLaunchSimulator();
        void onLoadTopology(TopologySummary summary);
        void onDeleteTopology(TopologySummary summary);
    }

    /**
     * Scrollable panel that tracks viewport width to ensure consistent margins and responsive cards.
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
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private TopologyStorageService storageService;
    private final Listener listener;

    private final JPanel cardsContainer;
    private final JLabel lblStatusInfo;
    private final List<TopologySummary> loadedSummaries = new ArrayList<>();

    private JButton btnRefreshHeader;
    private JButton btnHomeHeader;
    private JButton btnLaunchHeader;

    public HistoryPage(TopologyStorageService storageService, Listener listener) {
        this.storageService = storageService;
        this.listener = listener;

        setLayout(new BorderLayout());
        setBackground(Theme.BG_MAIN);

        // Fixed Top Navigation Header Bar
        add(createHeaderBar(), BorderLayout.NORTH);

        // Scrollable content area
        ScrollablePanel scrollContent = new ScrollablePanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(Theme.BG_MAIN);
        scrollContent.setBorder(BorderFactory.createEmptyBorder(16, 32, 32, 32));

        // 1. Intro Section (Title, Subtitle, Divider)
        scrollContent.add(createIntroSection());
        scrollContent.add(Box.createRigidArea(new Dimension(0, 16)));

        // 2. Summary Status Banner
        lblStatusInfo = new JLabel("Connecting to database...");
        lblStatusInfo.setFont(Theme.FONT_BODY);
        lblStatusInfo.setForeground(Theme.TEXT_SECONDARY);

        JPanel statusBanner = createStatusBanner();
        scrollContent.add(statusBanner);
        scrollContent.add(Box.createRigidArea(new Dimension(0, 18)));

        // 3. Topology Cards Container
        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setOpaque(false);
        cardsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollContent.add(cardsContainer);

        scrollContent.add(Box.createRigidArea(new Dimension(0, 24)));

        // 4. Bottom Navigation Buttons
        scrollContent.add(createBottomNavSection());
        scrollContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // 5. Footer
        scrollContent.add(createFooter());

        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        scrollPane.setBackground(Theme.BG_MAIN);
        scrollPane.getViewport().setBackground(Theme.BG_MAIN);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        // Initial populate
        refresh();
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

        JLabel lblSub = new JLabel("Saved Topologies & History");
        lblSub.setFont(Theme.FONT_BODY_BOLD);
        lblSub.setForeground(Theme.ACCENT_CYAN);

        left.add(logo);
        left.add(lblBrand);
        left.add(lblSep);
        left.add(lblSub);

        // Right Navigation Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        btnRefreshHeader = Theme.createButton("↻ Refresh", Theme.ButtonStyle.SECONDARY);
        btnRefreshHeader.setPreferredSize(new Dimension(105, 32));
        btnRefreshHeader.setFont(Theme.FONT_SMALL);
        btnRefreshHeader.addActionListener(e -> refresh());

        btnHomeHeader = Theme.createButton("← Home", Theme.ButtonStyle.SECONDARY);
        btnHomeHeader.setPreferredSize(new Dimension(90, 32));
        btnHomeHeader.setFont(Theme.FONT_SMALL);
        btnHomeHeader.addActionListener(e -> {
            if (listener != null) {
                listener.onBackToHome();
            }
        });

        btnLaunchHeader = Theme.createButton("Launch Simulator", Theme.ButtonStyle.PRIMARY);
        btnLaunchHeader.setPreferredSize(new Dimension(145, 32));
        btnLaunchHeader.setFont(Theme.FONT_SMALL);
        btnLaunchHeader.addActionListener(e -> {
            if (listener != null) {
                listener.onLaunchSimulator();
            }
        });

        right.add(btnRefreshHeader);
        right.add(btnHomeHeader);
        right.add(btnLaunchHeader);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createIntroSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("Saved Network Topologies");
        lblTitle.setFont(Theme.FONT_HERO);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSubtitle = new JLabel("Browse, inspect, load, and manage network topology snapshots stored in MySQL netroute_db.");
        lblSubtitle.setFont(Theme.FONT_BODY);
        lblSubtitle.setForeground(Theme.TEXT_SECONDARY);

        panel.add(lblTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(lblSubtitle);
        panel.add(Box.createRigidArea(new Dimension(0, 14)));

        // Subtle divider
        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.BORDER_SUBTLE);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(100, 1));
        divider.setOpaque(false);
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(divider);

        return panel;
    }

    private JPanel createStatusBanner() {
        JPanel banner = new JPanel(new BorderLayout(12, 0));
        banner.setBackground(Theme.BG_PANEL);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        banner.add(lblStatusInfo, BorderLayout.WEST);

        JLabel lblHint = new JLabel("MySQL Storage • Full Graph Reconstruction Supported");
        lblHint.setFont(Theme.FONT_SMALL);
        lblHint.setForeground(Theme.TEXT_MUTED);
        banner.add(lblHint, BorderLayout.EAST);

        return banner;
    }

    private JPanel createBottomNavSection() {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        nav.setOpaque(false);
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnBack = Theme.createButton("← Back to Home", Theme.ButtonStyle.SECONDARY);
        btnBack.setPreferredSize(new Dimension(140, 36));
        btnBack.setFont(Theme.FONT_BODY_BOLD);
        btnBack.addActionListener(e -> {
            if (listener != null) {
                listener.onBackToHome();
            }
        });

        JButton btnSim = Theme.createButton("Launch Simulator", Theme.ButtonStyle.PRIMARY);
        btnSim.setPreferredSize(new Dimension(150, 36));
        btnSim.setFont(Theme.FONT_BODY_BOLD);
        btnSim.addActionListener(e -> {
            if (listener != null) {
                listener.onLaunchSimulator();
            }
        });

        nav.add(btnBack);
        nav.add(btnSim);
        return nav;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("NetRoute • Java Swing Network Traffic & Routing Simulator  |  CSL 202 Academic Project");
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);

        footer.add(lbl);
        return footer;
    }

    /**
     * Refreshes the topology list by querying the database storage service.
     */
    public void refresh() {
        loadedSummaries.clear();
        cardsContainer.removeAll();

        if (storageService == null || !storageService.isDatabaseAvailable()) {
            lblStatusInfo.setText("● Database: Offline / Disconnected");
            lblStatusInfo.setForeground(Theme.STATUS_WARNING);
            cardsContainer.add(createDatabaseOfflineCard());
        } else {
            try {
                List<TopologySummary> topologies = storageService.listActiveTopologies();
                loadedSummaries.addAll(topologies);

                lblStatusInfo.setText(String.format("● Database: Connected (netroute_db)  •  Total Saved Topologies: %d", topologies.size()));
                lblStatusInfo.setForeground(Theme.STATUS_SUCCESS);

                if (topologies.isEmpty()) {
                    cardsContainer.add(createEmptyStateCard());
                } else {
                    for (TopologySummary summary : topologies) {
                        cardsContainer.add(createTopologyCard(summary));
                        cardsContainer.add(Box.createRigidArea(new Dimension(0, 14)));
                    }
                }
            } catch (Exception e) {
                lblStatusInfo.setText("● Database Error: " + e.getMessage());
                lblStatusInfo.setForeground(Theme.STATUS_ERROR);
                cardsContainer.add(createErrorCard("Failed to retrieve topologies: " + e.getMessage()));
            }
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private JPanel createTopologyCard(TopologySummary summary) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1, true),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 1: Header (Name + ID Badge on left, Node and Link Badges on right)
        JPanel rowHeader = new JPanel(new BorderLayout());
        rowHeader.setOpaque(false);

        JPanel leftTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTitlePanel.setOpaque(false);

        JLabel lblName = new JLabel(summary.getName());
        lblName.setFont(Theme.FONT_TITLE);
        lblName.setForeground(Theme.TEXT_PRIMARY);

        JPanel badgeId = createBadge("ID #" + summary.getId(), Theme.BG_INPUT, Theme.TEXT_SECONDARY);

        leftTitlePanel.add(lblName);
        leftTitlePanel.add(badgeId);

        JPanel rightStatsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightStatsPanel.setOpaque(false);

        JPanel badgeNodes = createBadge("● " + summary.getNodeCount() + " Nodes", Theme.BG_INPUT, Theme.ACCENT_CYAN);
        JPanel badgeLinks = createBadge("● " + summary.getLinkCount() + " Links", Theme.BG_INPUT, Theme.ACCENT_BLUE);

        rightStatsPanel.add(badgeNodes);
        rightStatsPanel.add(badgeLinks);

        rowHeader.add(leftTitlePanel, BorderLayout.WEST);
        rowHeader.add(rightStatsPanel, BorderLayout.EAST);
        card.add(rowHeader);

        card.add(Box.createRigidArea(new Dimension(0, 8)));

        // Row 2: Description (if present)
        if (summary.getDescription() != null && !summary.getDescription().trim().isEmpty()) {
            JTextArea txtDesc = new JTextArea(summary.getDescription().trim());
            txtDesc.setFont(Theme.FONT_BODY);
            txtDesc.setForeground(Theme.TEXT_SECONDARY);
            txtDesc.setOpaque(false);
            txtDesc.setEditable(false);
            txtDesc.setLineWrap(true);
            txtDesc.setWrapStyleWord(true);
            txtDesc.setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));
            card.add(txtDesc);
            card.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        // Row 3: Timestamps
        String createdStr = formatDateTime(summary.getCreatedAt());
        String updatedStr = formatDateTime(summary.getUpdatedAt());

        JPanel rowTimestamps = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rowTimestamps.setOpaque(false);

        JLabel lblTime = new JLabel("📅 Created: " + createdStr + "    •    🕒 Last Updated: " + updatedStr);
        lblTime.setFont(Theme.FONT_SMALL);
        lblTime.setForeground(Theme.TEXT_MUTED);
        rowTimestamps.add(lblTime);
        card.add(rowTimestamps);

        card.add(Box.createRigidArea(new Dimension(0, 12)));

        // Row 4: Subtle divider before actions
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
        card.add(div);

        card.add(Box.createRigidArea(new Dimension(0, 10)));

        // Row 5: Action Buttons (Load Topology and Delete Record)
        JPanel rowActions = new JPanel(new BorderLayout());
        rowActions.setOpaque(false);

        JLabel lblHint = new JLabel("Click 'Load' to open in simulator or 'Delete' to permanently remove record.");
        lblHint.setFont(Theme.FONT_SMALL);
        lblHint.setForeground(Theme.TEXT_MUTED);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnLoad = Theme.createButton("▶ Load Topology", Theme.ButtonStyle.PRIMARY);
        btnLoad.setFont(Theme.FONT_BODY_BOLD);
        btnLoad.setPreferredSize(new Dimension(145, 32));
        btnLoad.addActionListener(e -> {
            if (listener != null) {
                listener.onLoadTopology(summary);
            }
        });

        JButton btnDelete = Theme.createButton("🗑 Delete", Theme.ButtonStyle.DESTRUCTIVE);
        btnDelete.setFont(Theme.FONT_BODY_BOLD);
        btnDelete.setPreferredSize(new Dimension(105, 32));
        btnDelete.addActionListener(e -> {
            if (listener != null) {
                listener.onDeleteTopology(summary);
            }
        });

        btnPanel.add(btnLoad);
        btnPanel.add(btnDelete);

        rowActions.add(lblHint, BorderLayout.WEST);
        rowActions.add(btnPanel, BorderLayout.EAST);
        card.add(rowActions);

        return card;
    }

    private JPanel createEmptyStateCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1, true),
                BorderFactory.createEmptyBorder(36, 28, 36, 28)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("📂 No Saved Topologies Found");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.ACCENT_CYAN);

        JLabel lblSub = new JLabel("There are currently no network topologies saved in the MySQL database.");
        lblSub.setFont(Theme.FONT_BODY);
        lblSub.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblInfo = new JLabel("To save a topology, launch the Network Simulator, configure your network nodes and links, and click '💾 Save Topology'.");
        lblInfo.setFont(Theme.FONT_SMALL);
        lblInfo.setForeground(Theme.TEXT_SECONDARY);

        JButton btnGoSim = Theme.createButton("Launch Simulator", Theme.ButtonStyle.PRIMARY);
        btnGoSim.setFont(Theme.FONT_BODY_BOLD);
        btnGoSim.setPreferredSize(new Dimension(155, 34));
        btnGoSim.addActionListener(e -> {
            if (listener != null) {
                listener.onLaunchSimulator();
            }
        });

        card.add(lblTitle);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(lblSub);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(lblInfo);
        card.add(Box.createRigidArea(new Dimension(0, 18)));
        card.add(btnGoSim);

        return card;
    }

    private JPanel createDatabaseOfflineCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.STATUS_WARNING, 1, true),
                BorderFactory.createEmptyBorder(32, 28, 32, 28)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("⚠ Database Unavailable");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.STATUS_WARNING);

        JLabel lblSub = new JLabel("NetRoute is currently unable to communicate with MySQL database 'netroute_db'.");
        lblSub.setFont(Theme.FONT_BODY);
        lblSub.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblInfo = new JLabel("Please verify that your MySQL server is running on localhost:3306 and that db.properties contains valid credentials.");
        lblInfo.setFont(Theme.FONT_SMALL);
        lblInfo.setForeground(Theme.TEXT_SECONDARY);

        JButton btnRetry = Theme.createButton("↻ Retry Connection", Theme.ButtonStyle.SECONDARY);
        btnRetry.setFont(Theme.FONT_BODY_BOLD);
        btnRetry.setPreferredSize(new Dimension(160, 34));
        btnRetry.addActionListener(e -> refresh());

        card.add(lblTitle);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(lblSub);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(lblInfo);
        card.add(Box.createRigidArea(new Dimension(0, 18)));
        card.add(btnRetry);

        return card;
    }

    private JPanel createErrorCard(String errorMsg) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.STATUS_ERROR, 1, true),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("Error Retrieving Topologies");
        lblTitle.setFont(Theme.FONT_TITLE);
        lblTitle.setForeground(Theme.STATUS_ERROR);

        JLabel lblMsg = new JLabel(errorMsg);
        lblMsg.setFont(Theme.FONT_BODY);
        lblMsg.setForeground(Theme.TEXT_PRIMARY);

        JButton btnRetry = Theme.createButton("↻ Retry", Theme.ButtonStyle.SECONDARY);
        btnRetry.setFont(Theme.FONT_BODY_BOLD);
        btnRetry.setPreferredSize(new Dimension(110, 32));
        btnRetry.addActionListener(e -> refresh());

        card.add(lblTitle);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(lblMsg);
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(btnRetry);

        return card;
    }

    private JPanel createBadge(String text, Color bg, Color fg) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(fg);
        badge.add(lbl);
        return badge;
    }

    private static String formatDateTime(java.time.LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FORMATTER) : "N/A";
    }

    // Accessors for testing and external controllers

    public TopologyStorageService getStorageService() {
        return storageService;
    }

    public void setStorageService(TopologyStorageService storageService) {
        this.storageService = storageService;
        refresh();
    }

    public int getDisplayedCardCount() {
        return loadedSummaries.size();
    }

    public List<TopologySummary> getLoadedSummaries() {
        return Collections.unmodifiableList(loadedSummaries);
    }

    public JButton getBtnRefreshHeader() {
        return btnRefreshHeader;
    }

    public JButton getBtnHomeHeader() {
        return btnHomeHeader;
    }

    public JButton getBtnLaunchHeader() {
        return btnLaunchHeader;
    }

    public JPanel getCardsContainer() {
        return cardsContainer;
    }
}

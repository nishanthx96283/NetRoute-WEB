package com.netroute.ui;

import com.netroute.repository.SimulationRunRecord;
import com.netroute.service.TopologyStorageService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Dedicated Simulation History page for NetRoute displaying recorded simulation run experiments
 * and packet delivery analytics from MySQL {@code simulation_runs} table.
 * <p>
 * Displays newest runs first with stable secondary tie-break (ordered by run_timestamp DESC, id DESC).
 * Provides search/filtering by topology name or run ID, read-only detail inspection,
 * database refresh, and zero-mutation isolation from the active simulator engine.
 */
public class SimulationHistoryPage extends JPanel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] COLUMN_NAMES = {
            "Run ID", "Topology Name", "Topology ID", "Date & Time",
            "Ticks", "Packets", "Delivered", "Failed", "Dropped",
            "Delivery %", "Avg Delay", "Failures", "Reroutes"
    };

    public interface Listener {
        void onBackToHome();
        void onLaunchSimulator();
    }

    private TopologyStorageService storageService;
    private final Listener listener;

    private final List<SimulationRunRecord> loadedRuns = new ArrayList<>();
    private final List<SimulationRunRecord> displayedRuns = new ArrayList<>();

    private final SimulationTableModel tableModel = new SimulationTableModel();
    private final JTable table = new JTable(tableModel);
    private final JScrollPane tableScrollPane;

    private final JTextField txtSearch = new JTextField(18);
    private final JButton btnClearSearch;
    private final JButton btnViewDetails;
    private final JButton btnRefresh;

    private final JLabel lblStatusInfo = new JLabel();
    private final JPanel emptyPanel = new JPanel();
    private final JLabel lblEmptyTitle = new JLabel();
    private final JLabel lblEmptySubtitle = new JLabel();

    private final JPanel centerContainer = new JPanel(new BorderLayout());

    // Selected run quick-preview card
    private final JPanel previewCard = Theme.createCard();
    private final JLabel lblPreviewTitle = new JLabel("Select a simulation run to preview summary");
    private final JLabel lblPreviewStats = new JLabel("");
    private final JLabel lblPreviewNotes = new JLabel("");

    public SimulationHistoryPage(TopologyStorageService storageService, Listener listener) {
        this.storageService = storageService;
        this.listener = listener;

        setLayout(new BorderLayout());
        setBackground(Theme.BG_MAIN);

        // 1. Top Navigation Bar
        add(createHeaderBar(), BorderLayout.NORTH);

        // 2. Main Content
        JPanel mainContent = new JPanel(new BorderLayout(0, 12));
        mainContent.setBackground(Theme.BG_MAIN);
        mainContent.setBorder(BorderFactory.createEmptyBorder(16, 24, 20, 24));

        // Sub-header + Toolbar
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        topSection.add(createIntroSection());
        topSection.add(Box.createRigidArea(new Dimension(0, 12)));
        topSection.add(createToolbarPanel());
        mainContent.add(topSection, BorderLayout.NORTH);

        // Center: Table or Empty/Error View
        configureTable();
        tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        tableScrollPane.setBackground(Theme.BG_MAIN);
        tableScrollPane.getViewport().setBackground(Theme.BG_INPUT);

        setupEmptyPanel();

        centerContainer.setOpaque(false);
        centerContainer.add(tableScrollPane, BorderLayout.CENTER);
        mainContent.add(centerContainer, BorderLayout.CENTER);

        // Bottom: Quick Preview Card & Footer
        JPanel bottomSection = new JPanel(new BorderLayout(0, 8));
        bottomSection.setOpaque(false);
        setupPreviewCard();
        bottomSection.add(previewCard, BorderLayout.NORTH);
        bottomSection.add(createFooterPanel(), BorderLayout.SOUTH);
        mainContent.add(bottomSection, BorderLayout.SOUTH);

        add(mainContent, BorderLayout.CENTER);

        // Search listeners
        btnClearSearch = Theme.createButton("✕", Theme.ButtonStyle.OUTLINE);
        btnClearSearch.setPreferredSize(new Dimension(32, 28));
        btnClearSearch.setFont(Theme.FONT_SMALL);
        btnClearSearch.setToolTipText("Clear search filter");
        btnClearSearch.addActionListener(e -> {
            txtSearch.setText("");
            filter("");
        });

        btnViewDetails = Theme.createButton("👁 View Details", Theme.ButtonStyle.PRIMARY);
        btnViewDetails.setPreferredSize(new Dimension(125, 30));
        btnViewDetails.setFont(Theme.FONT_SMALL);
        btnViewDetails.setEnabled(false);
        btnViewDetails.addActionListener(e -> {
            SimulationRunRecord selected = getSelectedRun();
            if (selected != null) {
                showDetailsForRun(selected);
            }
        });

        btnRefresh = Theme.createButton("↻ Refresh", Theme.ButtonStyle.SECONDARY);
        btnRefresh.setPreferredSize(new Dimension(95, 30));
        btnRefresh.setFont(Theme.FONT_SMALL);
        btnRefresh.addActionListener(e -> refresh());

        // Perform initial load
        refresh();
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Left Branding
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

        JLabel lblSub = new JLabel("Simulation Run History");
        lblSub.setFont(Theme.FONT_BODY_BOLD);
        lblSub.setForeground(Theme.ACCENT_CYAN);

        left.add(logo);
        left.add(lblBrand);
        left.add(lblSep);
        left.add(lblSub);

        // Right Navigation
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton btnHome = Theme.createButton("← Home", Theme.ButtonStyle.SECONDARY);
        btnHome.setPreferredSize(new Dimension(90, 30));
        btnHome.setFont(Theme.FONT_SMALL);
        btnHome.addActionListener(e -> {
            if (listener != null) {
                listener.onBackToHome();
            }
        });

        JButton btnSim = Theme.createButton("Launch Simulator", Theme.ButtonStyle.PRIMARY);
        btnSim.setPreferredSize(new Dimension(140, 30));
        btnSim.setFont(Theme.FONT_SMALL);
        btnSim.addActionListener(e -> {
            if (listener != null) {
                listener.onLaunchSimulator();
            }
        });

        right.add(btnHome);
        right.add(btnSim);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createIntroSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("Simulation Execution & Analytics History");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSubtitle = new JLabel(
                "Historical record of past simulation experiments, packet delivery ratios, delays, and network events from MySQL netroute_db."
        );
        lblSubtitle.setFont(Theme.FONT_BODY);
        lblSubtitle.setForeground(Theme.TEXT_SECONDARY);

        panel.add(lblTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(lblSubtitle);
        return panel;
    }

    private JPanel createToolbarPanel() {
        JPanel toolbar = Theme.createCard();
        toolbar.setLayout(new BorderLayout(12, 0));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        // Left: Search Field & Clear Button
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel lblSearch = new JLabel("🔍 Filter Runs:");
        lblSearch.setFont(Theme.FONT_BODY_BOLD);
        lblSearch.setForeground(Theme.TEXT_PRIMARY);

        Theme.styleTextField(txtSearch);
        txtSearch.setPreferredSize(new Dimension(240, 30));
        txtSearch.setToolTipText("Search by topology name or run ID");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter(txtSearch.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter(txtSearch.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter(txtSearch.getText());
            }
        });

        left.add(lblSearch);
        left.add(txtSearch);

        // Center / Status
        lblStatusInfo.setFont(Theme.FONT_BODY);
        lblStatusInfo.setForeground(Theme.TEXT_SECONDARY);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        center.setOpaque(false);
        center.add(lblStatusInfo);

        // Right Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton btnHeaderRefresh = Theme.createButton("↻ Refresh", Theme.ButtonStyle.SECONDARY);
        btnHeaderRefresh.setPreferredSize(new Dimension(95, 30));
        btnHeaderRefresh.setFont(Theme.FONT_SMALL);
        btnHeaderRefresh.addActionListener(e -> refresh());

        JButton btnHeaderView = Theme.createButton("👁 View Details", Theme.ButtonStyle.PRIMARY);
        btnHeaderView.setPreferredSize(new Dimension(125, 30));
        btnHeaderView.setFont(Theme.FONT_SMALL);
        btnHeaderView.addActionListener(e -> {
            SimulationRunRecord selected = getSelectedRun();
            if (selected != null) {
                showDetailsForRun(selected);
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = table.getSelectedRow() >= 0;
            btnHeaderView.setEnabled(hasSelection);
            btnViewDetails.setEnabled(hasSelection);
            updatePreviewCard();
        });

        right.add(btnHeaderRefresh);
        right.add(btnHeaderView);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(center, BorderLayout.CENTER);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    private void configureTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(30);
        table.setFont(Theme.FONT_BODY);
        table.setBackground(Theme.BG_INPUT);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setGridColor(Theme.BORDER_SUBTLE);
        table.setShowGrid(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);

        // Custom Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(Theme.BG_PANEL);
        header.setForeground(Theme.ACCENT_CYAN);
        header.setFont(Theme.FONT_BODY_BOLD);
        header.setPreferredSize(new Dimension(header.getWidth(), 32));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE));

        // Column widths
        int[] widths = {65, 175, 110, 140, 60, 65, 75, 65, 65, 80, 75, 65, 70};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Cell Renderer
        SimulationCellRenderer cellRenderer = new SimulationCellRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // Double click & Enter listeners
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    SimulationRunRecord run = getSelectedRun();
                    if (run != null) {
                        showDetailsForRun(run);
                    }
                }
            }
        });

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SimulationRunRecord run = getSelectedRun();
                    if (run != null) {
                        e.consume();
                        showDetailsForRun(run);
                    }
                }
            }
        });
    }

    private void setupEmptyPanel() {
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setOpaque(false);
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(60, 40, 60, 40));

        lblEmptyTitle.setFont(Theme.FONT_TITLE);
        lblEmptyTitle.setForeground(Theme.TEXT_PRIMARY);
        lblEmptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblEmptySubtitle.setFont(Theme.FONT_BODY);
        lblEmptySubtitle.setForeground(Theme.TEXT_SECONDARY);
        lblEmptySubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyPanel.add(Box.createVerticalGlue());
        emptyPanel.add(lblEmptyTitle);
        emptyPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        emptyPanel.add(lblEmptySubtitle);
        emptyPanel.add(Box.createVerticalGlue());
    }

    private void setupPreviewCard() {
        previewCard.setLayout(new BorderLayout(8, 6));
        previewCard.setPreferredSize(new Dimension(Integer.MAX_VALUE, 65));
        previewCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        lblPreviewTitle.setFont(Theme.FONT_BODY_BOLD);
        lblPreviewTitle.setForeground(Theme.ACCENT_CYAN);

        lblPreviewStats.setFont(Theme.FONT_SMALL);
        lblPreviewStats.setForeground(Theme.TEXT_SECONDARY);

        lblPreviewNotes.setFont(Theme.FONT_SMALL);
        lblPreviewNotes.setForeground(Theme.TEXT_MUTED);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(lblPreviewTitle);
        textPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        textPanel.add(lblPreviewStats);
        textPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        textPanel.add(lblPreviewNotes);

        previewCard.add(textPanel, BorderLayout.CENTER);
    }

    private void updatePreviewCard() {
        SimulationRunRecord run = getSelectedRun();
        if (run == null) {
            lblPreviewTitle.setText("Select a simulation run to preview summary");
            lblPreviewTitle.setForeground(Theme.TEXT_MUTED);
            lblPreviewStats.setText("Click any row in the table above, or double-click to inspect detailed metrics.");
            lblPreviewNotes.setText("");
            return;
        }

        lblPreviewTitle.setText(String.format("Run #%d — %s (%s)",
                run.getId(),
                run.getTopologyName(),
                run.getRunTimestamp().format(DATE_FORMATTER)
        ));
        lblPreviewTitle.setForeground(Theme.ACCENT_CYAN);

        String topIdStr = run.getTopologyId() != null ? "#" + run.getTopologyId() : "Unlinked / Deleted";
        lblPreviewStats.setText(String.format(
                "Topology ID: %s  |  Delivery: %.1f%% (%d/%d packets)  |  Avg Delay: %.2f ticks  |  Failures: %d  |  Reroutes: %d",
                topIdStr, run.getDeliveryRatio(), run.getDeliveredPackets(), run.getTotalPackets(),
                run.getAvgDeliveryDelay(), run.getLinkFailures(), run.getReroutes()
        ));

        String notes = run.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            lblPreviewNotes.setText("Notes: " + notes.trim());
        } else {
            lblPreviewNotes.setText("Notes: (None)");
        }
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        JLabel lblHint = new JLabel("💡 Tip: Double-click any row or select and press Enter to view the full analytics breakdown.");
        lblHint.setFont(Theme.FONT_SMALL);
        lblHint.setForeground(Theme.TEXT_MUTED);
        footer.add(lblHint);
        return footer;
    }

    /**
     * Refreshes simulation runs list from the database.
     */
    public void refresh() {
        loadedRuns.clear();

        if (storageService == null || !storageService.isDatabaseAvailable()) {
            showErrorState("Database Unavailable",
                    "Could not connect to netroute_db. Please ensure MySQL service is running.");
            return;
        }

        try {
            List<SimulationRunRecord> runs = storageService.listAllSimulationRuns();
            if (runs != null) {
                loadedRuns.addAll(runs);
            }

            if (loadedRuns.isEmpty()) {
                showEmptyState("No Simulation Runs Found",
                        "No simulation runs have been recorded yet. Run a packet simulation in the simulator and click 💾 Save.");
            } else {
                showTable();
                filter(txtSearch.getText());
            }
        } catch (SQLException ex) {
            showErrorState("Database Query Error",
                    "Failed to query simulation history: " + sanitizeErrorMessage(ex.getMessage()));
        }
    }

    /**
     * Filters displayed runs by topology name (substring, case-insensitive) or run ID.
     */
    public void filter(String query) {
        displayedRuns.clear();

        if (query == null || query.trim().isEmpty()) {
            displayedRuns.addAll(loadedRuns);
        } else {
            String q = query.trim().toLowerCase();
            for (SimulationRunRecord run : loadedRuns) {
                boolean matchesName = run.getTopologyName() != null && run.getTopologyName().toLowerCase().contains(q);
                boolean matchesId = String.valueOf(run.getId()).contains(q);
                boolean matchesTopId = run.getTopologyId() != null && String.valueOf(run.getTopologyId()).contains(q);
                if (matchesName || matchesId || matchesTopId) {
                    displayedRuns.add(run);
                }
            }
        }

        tableModel.fireTableDataChanged();

        if (!loadedRuns.isEmpty() && displayedRuns.isEmpty()) {
            showEmptyState("No Matching Runs", "No simulation runs matched the filter query: \"" + query.trim() + "\"");
        } else if (!loadedRuns.isEmpty()) {
            showTable();
        }

        updateStatusText();
        updatePreviewCard();
    }

    private void updateStatusText() {
        if (loadedRuns.isEmpty()) {
            lblStatusInfo.setText("0 recorded simulation runs");
        } else if (displayedRuns.size() == loadedRuns.size()) {
            lblStatusInfo.setText(String.format("Showing all %d recorded simulation runs", loadedRuns.size()));
        } else {
            lblStatusInfo.setText(String.format("Showing %d of %d simulation runs", displayedRuns.size(), loadedRuns.size()));
        }
    }

    private void showTable() {
        centerContainer.removeAll();
        centerContainer.add(tableScrollPane, BorderLayout.CENTER);
        centerContainer.revalidate();
        centerContainer.repaint();
    }

    private void showEmptyState(String title, String subtitle) {
        lblEmptyTitle.setText(title);
        lblEmptyTitle.setForeground(Theme.TEXT_SECONDARY);
        lblEmptySubtitle.setText(subtitle);
        centerContainer.removeAll();
        centerContainer.add(emptyPanel, BorderLayout.CENTER);
        centerContainer.revalidate();
        centerContainer.repaint();
        lblStatusInfo.setText(title);
    }

    private void showErrorState(String title, String subtitle) {
        lblEmptyTitle.setText(title);
        lblEmptyTitle.setForeground(Theme.STATUS_ERROR);
        lblEmptySubtitle.setText(subtitle);
        centerContainer.removeAll();
        centerContainer.add(emptyPanel, BorderLayout.CENTER);
        centerContainer.revalidate();
        centerContainer.repaint();
        lblStatusInfo.setText(title);
    }

    private String sanitizeErrorMessage(String msg) {
        if (msg == null) return "Unknown error";
        // Strip out passwords or connection strings if any appear
        return msg.replaceAll("password=[^;&]*", "password=***");
    }

    /**
     * Opens read-only detailed modal dialog for the selected simulation run.
     * Guaranteed to have zero side-effects on simulator state.
     */
    public void showDetailsForRun(SimulationRunRecord record) {
        if (record == null) return;

        Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        SimulationRunDetailDialog dialog = new SimulationRunDetailDialog(parentFrame, record);
        dialog.setVisible(true);
    }

    // ==========================================
    // Table Model & Cell Renderer
    // ==========================================

    private class SimulationTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return displayedRuns.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= displayedRuns.size()) return "";
            SimulationRunRecord run = displayedRuns.get(rowIndex);

            return switch (columnIndex) {
                case 0 -> "#" + run.getId();
                case 1 -> run.getTopologyName();
                case 2 -> run.getTopologyId() != null ? "#" + run.getTopologyId() : "— (Deleted)";
                case 3 -> run.getRunTimestamp().format(DATE_FORMATTER);
                case 4 -> run.getSimulationTicks();
                case 5 -> run.getTotalPackets();
                case 6 -> run.getDeliveredPackets();
                case 7 -> run.getFailedPackets();
                case 8 -> run.getDroppedPackets();
                case 9 -> String.format("%.1f%%", run.getDeliveryRatio());
                case 10 -> String.format("%.2f", run.getAvgDeliveryDelay());
                case 11 -> run.getLinkFailures();
                case 12 -> run.getReroutes();
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private class SimulationCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (row < 0 || row >= displayedRuns.size()) return c;
            SimulationRunRecord run = displayedRuns.get(row);

            // Row striping
            if (isSelected) {
                c.setBackground(new Color(0x2D, 0x48, 0x66));
                c.setForeground(Theme.TEXT_PRIMARY);
            } else {
                c.setBackground(row % 2 == 0 ? Theme.BG_INPUT : new Color(0x1E, 0x2C, 0x3A));
                c.setForeground(Theme.TEXT_PRIMARY);
            }

            // Alignments & special formatting
            if (column == 1) {
                setHorizontalAlignment(SwingConstants.LEFT);
                if (!isSelected) c.setForeground(Theme.TEXT_PRIMARY);
            } else {
                setHorizontalAlignment(SwingConstants.CENTER);
            }

            // Highlighting Run ID
            if (column == 0 && !isSelected) {
                c.setForeground(Theme.ACCENT_CYAN);
            }

            // Topology ID handling
            if (column == 2) {
                if (run.getTopologyId() == null && !isSelected) {
                    c.setForeground(Theme.TEXT_MUTED);
                } else if (!isSelected) {
                    c.setForeground(Theme.ACCENT_BLUE);
                }
            }

            // Delivery Ratio coloring
            if (column == 9 && !isSelected) {
                double ratio = run.getDeliveryRatio();
                if (ratio >= 90.0) {
                    c.setForeground(Theme.STATUS_SUCCESS);
                } else if (ratio >= 50.0) {
                    c.setForeground(Theme.STATUS_WARNING);
                } else {
                    c.setForeground(Theme.STATUS_ERROR);
                }
            }

            // Failed & Dropped packets coloring
            if ((column == 7 || column == 8) && !isSelected) {
                int count = column == 7 ? run.getFailedPackets() : run.getDroppedPackets();
                if (count > 0) {
                    c.setForeground(Theme.STATUS_ERROR);
                } else {
                    c.setForeground(Theme.TEXT_MUTED);
                }
            }

            // Link Failures coloring
            if (column == 11 && !isSelected) {
                if (run.getLinkFailures() > 0) {
                    c.setForeground(Theme.STATUS_WARNING);
                } else {
                    c.setForeground(Theme.TEXT_MUTED);
                }
            }

            return c;
        }
    }

    // ==========================================
    // Read-only Run Detail Dialog
    // ==========================================

    public static class SimulationRunDetailDialog extends JDialog {

        public SimulationRunDetailDialog(Frame owner, SimulationRunRecord run) {
            super(owner, "Simulation Run #" + run.getId() + " Details", true);
            Objects.requireNonNull(run, "run record cannot be null");

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setResizable(false);
            getContentPane().setBackground(Theme.BG_MAIN);

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(Theme.BG_MAIN);
            content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            // Header Banner
            JPanel headerPanel = Theme.createCard();
            headerPanel.setLayout(new BorderLayout(10, 6));
            headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblHeaderTitle = new JLabel("Run #" + run.getId() + " — " + run.getTopologyName());
            lblHeaderTitle.setFont(Theme.FONT_TITLE);
            lblHeaderTitle.setForeground(Theme.ACCENT_CYAN);

            String topStatus = run.getTopologyId() != null
                    ? "Associated Topology ID: #" + run.getTopologyId()
                    : "Associated Topology: None / Unlinked (original topology removed or unsaved)";
            JLabel lblHeaderSub = new JLabel(topStatus + "  •  Timestamp: " + run.getRunTimestamp().format(DATE_FORMATTER));
            lblHeaderSub.setFont(Theme.FONT_SMALL);
            lblHeaderSub.setForeground(Theme.TEXT_SECONDARY);

            headerPanel.add(lblHeaderTitle, BorderLayout.NORTH);
            headerPanel.add(lblHeaderSub, BorderLayout.SOUTH);
            content.add(headerPanel);

            content.add(Box.createRigidArea(new Dimension(0, 14)));

            // Metrics Grid Card
            JPanel metricsCard = Theme.createCard();
            metricsCard.setLayout(new GridLayout(4, 2, 16, 10));
            metricsCard.setBorder(Theme.createTitledBorder("Execution & Performance Metrics"));
            metricsCard.setAlignmentX(Component.CENTER_ALIGNMENT);

            metricsCard.add(createMetricItem("Simulation Duration", run.getSimulationTicks() + " ticks"));
            metricsCard.add(createMetricItem("Total Transmitted Packets", String.valueOf(run.getTotalPackets())));
            metricsCard.add(createMetricItem("Successfully Delivered", String.valueOf(run.getDeliveredPackets())));
            metricsCard.add(createMetricItem("Delivery Ratio", String.format("%.2f%%", run.getDeliveryRatio())));
            metricsCard.add(createMetricItem("Failed Delivery", String.valueOf(run.getFailedPackets())));
            metricsCard.add(createMetricItem("Average Delivery Delay", String.format("%.2f ticks", run.getAvgDeliveryDelay())));
            metricsCard.add(createMetricItem("Packets Dropped", String.valueOf(run.getDroppedPackets())));
            metricsCard.add(createMetricItem("Failed Links / Reroutes", run.getLinkFailures() + " failed / " + run.getReroutes() + " reroutes"));

            content.add(metricsCard);

            content.add(Box.createRigidArea(new Dimension(0, 14)));

            // Notes Section
            JPanel notesCard = Theme.createCard();
            notesCard.setLayout(new BorderLayout(6, 6));
            notesCard.setBorder(Theme.createTitledBorder("Scenario Notes & Description"));
            notesCard.setAlignmentX(Component.CENTER_ALIGNMENT);

            JTextArea txtNotes = new JTextArea(4, 40);
            txtNotes.setEditable(false);
            txtNotes.setFont(Theme.FONT_BODY);
            txtNotes.setBackground(Theme.BG_INPUT);
            txtNotes.setForeground(Theme.TEXT_PRIMARY);
            txtNotes.setLineWrap(true);
            txtNotes.setWrapStyleWord(true);
            txtNotes.setCaretPosition(0);

            String notes = run.getNotes();
            if (notes == null || notes.trim().isEmpty()) {
                txtNotes.setText("(No scenario notes provided for this simulation run)");
                txtNotes.setForeground(Theme.TEXT_MUTED);
            } else {
                txtNotes.setText(notes.trim());
            }

            JScrollPane notesScroll = new JScrollPane(txtNotes);
            notesScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
            notesCard.add(notesScroll, BorderLayout.CENTER);
            content.add(notesCard);

            content.add(Box.createRigidArea(new Dimension(0, 18)));

            // Close Button
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            actionPanel.setOpaque(false);
            actionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton btnClose = Theme.createButton("Close", Theme.ButtonStyle.PRIMARY);
            btnClose.setPreferredSize(new Dimension(100, 32));
            btnClose.addActionListener(e -> dispose());
            actionPanel.add(btnClose);
            content.add(actionPanel);

            getContentPane().add(content);
            pack();
            setLocationRelativeTo(owner);
        }

        private JPanel createMetricItem(String label, String value) {
            JPanel p = new JPanel(new BorderLayout(6, 0));
            p.setOpaque(false);

            JLabel lbl = new JLabel(label + ":");
            lbl.setFont(Theme.FONT_BODY);
            lbl.setForeground(Theme.TEXT_SECONDARY);

            JLabel val = new JLabel(value);
            val.setFont(Theme.FONT_BODY_BOLD);
            val.setForeground(Theme.TEXT_PRIMARY);

            p.add(lbl, BorderLayout.WEST);
            p.add(val, BorderLayout.EAST);
            return p;
        }
    }

    // ==========================================
    // Getters & Programmatic API for Testing
    // ==========================================

    public int getDisplayedRowCount() {
        return displayedRuns.size();
    }

    public List<SimulationRunRecord> getLoadedRuns() {
        return Collections.unmodifiableList(loadedRuns);
    }

    public List<SimulationRunRecord> getDisplayedRuns() {
        return Collections.unmodifiableList(displayedRuns);
    }

    public SimulationRunRecord getSelectedRun() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < displayedRuns.size()) {
            return displayedRuns.get(selectedRow);
        }
        return null;
    }

    public void selectRunById(int runId) {
        for (int i = 0; i < displayedRuns.size(); i++) {
            if (displayedRuns.get(i).getId() == runId) {
                table.setRowSelectionInterval(i, i);
                updatePreviewCard();
                return;
            }
        }
    }

    public void setStorageService(TopologyStorageService storageService) {
        this.storageService = storageService;
    }

    public JTextField getSearchField() {
        return txtSearch;
    }

    public JButton getRefreshButton() {
        return btnRefresh;
    }

    public JButton getViewDetailsButton() {
        return btnViewDetails;
    }

    public JTable getTable() {
        return table;
    }
}

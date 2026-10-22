package com.netroute.ui;

import com.netroute.statistics.NetworkStatistics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Visual display panel for {@link NetworkStatistics} with modern dark metric cards.
 * <p>
 * Displays real-time packet delivery counts, delivery ratio, simulated delay,
 * topology counts, link failures, and reroutes.
 */
public class StatisticsPanel extends JPanel {

    // Metric Value Labels
    private final JLabel lblTotalPackets = createValueLabel("0", Theme.TEXT_PRIMARY);
    private final JLabel lblDelivered = createValueLabel("0", Theme.STATUS_SUCCESS);
    private final JLabel lblFailed = createValueLabel("0", Theme.STATUS_ERROR);
    private final JLabel lblDropped = createValueLabel("0", Theme.STATUS_ERROR);
    private final JLabel lblDeliveryRatio = createValueLabel("0.0%", Theme.ACCENT_CYAN);
    private final JLabel lblAvgDelay = createValueLabel("0.00 ticks", Theme.ACCENT_BLUE);

    private final JLabel lblTotalNodes = createValueLabel("0", Theme.TEXT_PRIMARY);
    private final JLabel lblTotalLinks = createValueLabel("0", Theme.TEXT_PRIMARY);
    private final JLabel lblActiveLinks = createValueLabel("0", Theme.STATUS_SUCCESS);
    private final JLabel lblInactiveLinks = createValueLabel("0", Theme.TEXT_MUTED);
    private final JLabel lblLinkFailures = createValueLabel("0", Theme.STATUS_WARNING);
    private final JLabel lblReroutes = createValueLabel("0", Theme.ACCENT_CYAN);

    public StatisticsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BG_PANEL);
        setBorder(Theme.createTitledBorder("Simulation & Network Metrics"));

        // Section 1: Traffic & Delivery
        JPanel trafficHeader = createSubHeader("TRAFFIC & PACKET DELIVERY");
        JPanel trafficGrid = new JPanel(new GridLayout(3, 2, 8, 6));
        trafficGrid.setOpaque(false);

        trafficGrid.add(createMetricTile("Total Packets", lblTotalPackets));
        trafficGrid.add(createMetricTile("Delivered", lblDelivered));
        trafficGrid.add(createMetricTile("Failed at Submission", lblFailed));
        trafficGrid.add(createMetricTile("Dropped in Transit", lblDropped));
        trafficGrid.add(createMetricTile("Delivery Ratio", lblDeliveryRatio));
        trafficGrid.add(createMetricTile("Avg Delivery Delay", lblAvgDelay));

        // Section 2: Topology & Events
        JPanel topologyHeader = createSubHeader("TOPOLOGY & RESILIENCE");
        JPanel topologyGrid = new JPanel(new GridLayout(3, 2, 8, 6));
        topologyGrid.setOpaque(false);

        topologyGrid.add(createMetricTile("Total Nodes", lblTotalNodes));
        topologyGrid.add(createMetricTile("Total Links", lblTotalLinks));
        topologyGrid.add(createMetricTile("Active Links", lblActiveLinks));
        topologyGrid.add(createMetricTile("Inactive / Failed", lblInactiveLinks));
        topologyGrid.add(createMetricTile("Link Failure Events", lblLinkFailures));
        topologyGrid.add(createMetricTile("Successful Reroutes", lblReroutes));

        add(trafficHeader);
        add(Box.createRigidArea(new Dimension(0, 4)));
        add(trafficGrid);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(topologyHeader);
        add(Box.createRigidArea(new Dimension(0, 4)));
        add(topologyGrid);
    }

    private JPanel createSubHeader(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(Theme.TEXT_MUTED);
        panel.add(lbl, BorderLayout.WEST);
        return panel;
    }

    private JPanel createMetricTile(String title, JLabel valueLabel) {
        JPanel tile = new JPanel(new BorderLayout(4, 2));
        tile.setBackground(Theme.BG_INPUT);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_SMALL);
        lblTitle.setForeground(Theme.TEXT_SECONDARY);

        tile.add(lblTitle, BorderLayout.NORTH);
        tile.add(valueLabel, BorderLayout.CENTER);
        return tile;
    }

    private static JLabel createValueLabel(String initialText, Color color) {
        JLabel label = new JLabel(initialText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(color);
        return label;
    }

    /**
     * Updates all metric labels from the provided {@link NetworkStatistics} object.
     */
    public void updateStatistics(NetworkStatistics stats) {
        if (stats == null) {
            return;
        }

        lblTotalPackets.setText(String.valueOf(stats.getTotalPackets()));

        int delivered = stats.getDeliveredCount();
        lblDelivered.setText(String.valueOf(delivered));
        lblDelivered.setForeground(delivered > 0 ? Theme.STATUS_SUCCESS : Theme.TEXT_MUTED);

        int failed = stats.getFailedCount();
        lblFailed.setText(String.valueOf(failed));
        lblFailed.setForeground(failed > 0 ? Theme.STATUS_ERROR : Theme.TEXT_MUTED);

        int dropped = stats.getDroppedCount();
        lblDropped.setText(String.valueOf(dropped));
        lblDropped.setForeground(dropped > 0 ? Theme.STATUS_ERROR : Theme.TEXT_MUTED);

        lblDeliveryRatio.setText(String.format("%.1f%%", stats.getDeliveryRatio()));
        lblAvgDelay.setText(String.format("%.2f ticks", stats.getAverageDeliveryDelay()));

        lblTotalNodes.setText(String.valueOf(stats.getTotalNodes()));
        int active = stats.getActiveLinks();
        int inactive = stats.getInactiveLinks();
        lblTotalLinks.setText(String.valueOf(active + inactive));
        lblActiveLinks.setText(String.valueOf(active));

        lblInactiveLinks.setText(String.valueOf(inactive));
        lblInactiveLinks.setForeground(inactive > 0 ? Theme.STATUS_ERROR : Theme.TEXT_MUTED);

        lblLinkFailures.setText(String.valueOf(stats.getLinkFailureCount()));
        int reroutes = stats.getTotalReroutes();
        lblReroutes.setText(String.valueOf(reroutes));
        lblReroutes.setForeground(reroutes > 0 ? Theme.ACCENT_CYAN : Theme.TEXT_MUTED);
    }
}

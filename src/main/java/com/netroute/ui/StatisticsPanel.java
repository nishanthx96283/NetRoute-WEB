package com.netroute.ui;

import com.netroute.statistics.NetworkStatistics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Visual display panel for {@link NetworkStatistics}.
 * <p>
 * Displays packet delivery counts, delivery ratio, simulated delay,
 * network topology counts, link failures, and reroutes in real-time.
 */
public class StatisticsPanel extends JPanel {

    private final JLabel lblTotalPackets = createValueLabel("0");
    private final JLabel lblDelivered = createValueLabel("0");
    private final JLabel lblFailed = createValueLabel("0");
    private final JLabel lblDropped = createValueLabel("0");
    private final JLabel lblDeliveryRatio = createValueLabel("0.0%");
    private final JLabel lblAvgDelay = createValueLabel("0.00 ticks");

    private final JLabel lblTotalNodes = createValueLabel("0");
    private final JLabel lblActiveLinks = createValueLabel("0");
    private final JLabel lblInactiveLinks = createValueLabel("0");
    private final JLabel lblLinkFailures = createValueLabel("0");
    private final JLabel lblReroutes = createValueLabel("0");

    public StatisticsPanel() {
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Simulation Statistics"),
                BorderFactory.createEmptyBorder(6, 10, 8, 10)
        ));

        // 2-column key-value grid layout
        setLayout(new GridLayout(11, 2, 8, 4));

        addStatRow("Total Packets Submitted:", lblTotalPackets);
        addStatRow("Successfully Delivered:", lblDelivered);
        addStatRow("Failed at Submission:", lblFailed);
        addStatRow("Dropped in Transit:", lblDropped);
        addStatRow("Delivery Ratio:", lblDeliveryRatio);
        addStatRow("Avg Delivery Delay:", lblAvgDelay);

        addStatRow("Total Nodes in Network:", lblTotalNodes);
        addStatRow("Active Links:", lblActiveLinks);
        addStatRow("Failed / Inactive Links:", lblInactiveLinks);
        addStatRow("Link Failure Events:", lblLinkFailures);
        addStatRow("Successful Reroutes:", lblReroutes);
    }

    private void addStatRow(String labelText, JLabel valueLabel) {
        JLabel titleLabel = new JLabel(labelText);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(60, 64, 67));
        add(titleLabel);
        add(valueLabel);
    }

    private static JLabel createValueLabel(String initialText) {
        JLabel label = new JLabel(initialText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(32, 33, 36));
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
        lblDelivered.setForeground(delivered > 0 ? new Color(40, 167, 69) : Color.DARK_GRAY);

        int failed = stats.getFailedCount();
        lblFailed.setText(String.valueOf(failed));
        lblFailed.setForeground(failed > 0 ? new Color(220, 53, 69) : Color.DARK_GRAY);

        int dropped = stats.getDroppedCount();
        lblDropped.setText(String.valueOf(dropped));
        lblDropped.setForeground(dropped > 0 ? new Color(220, 53, 69) : Color.DARK_GRAY);

        lblDeliveryRatio.setText(String.format("%.1f%%", stats.getDeliveryRatio()));
        lblAvgDelay.setText(String.format("%.2f ticks", stats.getAverageDeliveryDelay()));

        lblTotalNodes.setText(String.valueOf(stats.getTotalNodes()));
        lblActiveLinks.setText(String.valueOf(stats.getActiveLinks()));

        int inactive = stats.getInactiveLinks();
        lblInactiveLinks.setText(String.valueOf(inactive));
        lblInactiveLinks.setForeground(inactive > 0 ? new Color(220, 53, 69) : Color.DARK_GRAY);

        lblLinkFailures.setText(String.valueOf(stats.getLinkFailureCount()));

        int reroutes = stats.getTotalReroutes();
        lblReroutes.setText(String.valueOf(reroutes));
        lblReroutes.setForeground(reroutes > 0 ? new Color(13, 110, 253) : Color.DARK_GRAY);
    }
}

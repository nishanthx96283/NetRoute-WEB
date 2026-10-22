package com.netroute.ui;

import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Control panel for routing, packet simulation, and link failure management.
 */
public class ControlPanel extends JPanel {

    public interface Listener {
        void onFindRoute(String sourceId, String destinationId);
        void onSendPacket(String sourceId, String destinationId);
        void onStepSimulation();
        void onFailLink(String linkId);
        void onRestoreLink(String linkId);
        void onResetNetwork();
    }

    private final JComboBox<String> cmbSource = new JComboBox<>();
    private final JComboBox<String> cmbDestination = new JComboBox<>();
    private final JComboBox<String> cmbLinks = new JComboBox<>();

    private final JLabel lblRouteResult = new JLabel("No route calculated yet.");
    private Listener listener;

    public ControlPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        add(createRoutingPanel());
        add(createLinkControlPanel());
        add(createSimulationActionPanel());
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Populates combo boxes with current nodes and links in the network.
     */
    public void populate(Network network) {
        if (network == null) {
            return;
        }

        List<String> nodeIds = new ArrayList<>(network.getNodes().keySet());
        Collections.sort(nodeIds);

        DefaultComboBoxModel<String> srcModel = new DefaultComboBoxModel<>(nodeIds.toArray(new String[0]));
        DefaultComboBoxModel<String> dstModel = new DefaultComboBoxModel<>(nodeIds.toArray(new String[0]));

        cmbSource.setModel(srcModel);
        cmbDestination.setModel(dstModel);

        // Default: PC1 -> S1 if available
        if (nodeIds.contains("PC1")) {
            cmbSource.setSelectedItem("PC1");
        }
        if (nodeIds.contains("S1")) {
            cmbDestination.setSelectedItem("S1");
        }

        populateLinks(network);
    }

    public void populateLinks(Network network) {
        if (network == null) {
            return;
        }
        List<String> linkDescriptions = new ArrayList<>();
        List<String> linkIds = new ArrayList<>(network.getLinks().keySet());
        Collections.sort(linkIds);

        for (String id : linkIds) {
            NetworkLink link = network.getLink(id);
            String status = link.isActive() ? "ACTIVE" : "FAILED";
            linkDescriptions.add(id + " (" + link.getSourceId() + " - " + link.getDestinationId() + ") [" + status + "]");
        }

        int prevSelected = cmbLinks.getSelectedIndex();
        cmbLinks.setModel(new DefaultComboBoxModel<>(linkDescriptions.toArray(new String[0])));
        if (prevSelected >= 0 && prevSelected < linkDescriptions.size()) {
            cmbLinks.setSelectedIndex(prevSelected);
        }
    }

    public void setRouteResult(String text, boolean success) {
        lblRouteResult.setText(text);
        lblRouteResult.setForeground(success ? new Color(13, 110, 253) : new Color(220, 53, 69));
    }

    private JPanel createRoutingPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Route Calculation & Packet Sending"),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JPanel formGrid = new JPanel(new GridLayout(2, 2, 8, 6));
        formGrid.add(new JLabel("Source Node:"));
        formGrid.add(cmbSource);
        formGrid.add(new JLabel("Destination Node:"));
        formGrid.add(cmbDestination);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton btnFindRoute = new JButton("Calculate Route (Dijkstra)");
        JButton btnSendPacket = new JButton("Send Packet & Run");
        btnRow.add(btnFindRoute);
        btnRow.add(btnSendPacket);

        lblRouteResult.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblRouteResult.setForeground(Color.DARK_GRAY);

        btnFindRoute.addActionListener(e -> {
            String src = (String) cmbSource.getSelectedItem();
            String dst = (String) cmbDestination.getSelectedItem();
            if (listener != null && src != null && dst != null) {
                listener.onFindRoute(src, dst);
            }
        });

        btnSendPacket.addActionListener(e -> {
            String src = (String) cmbSource.getSelectedItem();
            String dst = (String) cmbDestination.getSelectedItem();
            if (listener != null && src != null && dst != null) {
                listener.onSendPacket(src, dst);
            }
        });

        panel.add(formGrid, BorderLayout.NORTH);
        panel.add(btnRow, BorderLayout.CENTER);
        panel.add(lblRouteResult, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLinkControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Link Failure & Reroute Simulation"),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JPanel linkSelectPanel = new JPanel(new BorderLayout(5, 5));
        linkSelectPanel.add(new JLabel("Select Link:"), BorderLayout.WEST);
        linkSelectPanel.add(cmbLinks, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton btnFail = new JButton("Simulate Link Failure");
        JButton btnRestore = new JButton("Restore Link");
        btnFail.setForeground(new Color(180, 40, 50));
        btnRestore.setForeground(new Color(30, 130, 60));

        btnRow.add(btnFail);
        btnRow.add(btnRestore);

        btnFail.addActionListener(e -> {
            String selected = (String) cmbLinks.getSelectedItem();
            if (listener != null && selected != null) {
                String linkId = extractLinkId(selected);
                listener.onFailLink(linkId);
            }
        });

        btnRestore.addActionListener(e -> {
            String selected = (String) cmbLinks.getSelectedItem();
            if (listener != null && selected != null) {
                String linkId = extractLinkId(selected);
                listener.onRestoreLink(linkId);
            }
        });

        panel.add(linkSelectPanel, BorderLayout.NORTH);
        panel.add(btnRow, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSimulationActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Simulation Controls"),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JButton btnStep = new JButton("Step 1 Tick");
        JButton btnReset = new JButton("Reset Sample Network");

        btnStep.addActionListener(e -> {
            if (listener != null) {
                listener.onStepSimulation();
            }
        });

        btnReset.addActionListener(e -> {
            if (listener != null) {
                listener.onResetNetwork();
            }
        });

        panel.add(btnStep);
        panel.add(btnReset);
        return panel;
    }

    private static String extractLinkId(String text) {
        int spaceIndex = text.indexOf(' ');
        if (spaceIndex != -1) {
            return text.substring(0, spaceIndex).trim();
        }
        return text.trim();
    }
}

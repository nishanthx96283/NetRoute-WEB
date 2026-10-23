package com.netroute.ui;

import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.NodeType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
 * Control panel for routing, packet simulation, link failure management,
 * link cost randomization, and dynamic network management with dark theme styling.
 */
public class ControlPanel extends JPanel {

    public interface Listener {
        void onFindRoute(String sourceId, String destinationId);
        void onSendPacket(String sourceId, String destinationId);
        void onStepSimulation();
        void onFailLink(String linkId);
        void onRestoreLink(String linkId);
        void onResetNetwork();
        void onRandomizeLinkCosts();
        void onAddNode(NodeType type);
        void onConnectNodes(String sourceId, String destinationId, double cost);
        void onDeleteNode(String nodeId);
        void onDeleteLink(String linkId);
        default void onSave() {}
        default void onSaveTopology() {
            onSave();
        }
        default void onFinishAndSaveRun() {
            onSave();
        }
    }

    private final JComboBox<String> cmbSource = new JComboBox<>();
    private final JComboBox<String> cmbDestination = new JComboBox<>();
    private final JComboBox<String> cmbLinks = new JComboBox<>();

    // Dynamic Network Management components
    private final JComboBox<NodeType> cmbNodeType = new JComboBox<>(NodeType.values());
    private final JComboBox<String> cmbConnectNodeA = new JComboBox<>();
    private final JComboBox<String> cmbConnectNodeB = new JComboBox<>();
    private final JTextField txtLinkCost = new JTextField("2.0", 5);
    private final JComboBox<String> cmbDeleteNode = new JComboBox<>();

    private final JLabel lblRouteResult = new JLabel("No route calculated yet.");
    private Listener listener;

    public ControlPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Apply dark styling to combo boxes and inputs
        Theme.styleComboBox(cmbSource);
        Theme.styleComboBox(cmbDestination);
        Theme.styleComboBox(cmbLinks);
        Theme.styleComboBox(cmbNodeType);
        Theme.styleComboBox(cmbConnectNodeA);
        Theme.styleComboBox(cmbConnectNodeB);
        Theme.styleComboBox(cmbDeleteNode);
        Theme.styleTextField(txtLinkCost);

        add(createRoutingPanel());
        add(Box.createRigidArea(new Dimension(0, 8)));
        add(createLinkControlPanel());
        add(Box.createRigidArea(new Dimension(0, 8)));
        add(createNetworkManagementPanel());
        add(Box.createRigidArea(new Dimension(0, 8)));
        add(createSimulationActionPanel());
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String getSelectedSource() {
        return (String) cmbSource.getSelectedItem();
    }

    public String getSelectedDestination() {
        return (String) cmbDestination.getSelectedItem();
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

        String prevSrc = (String) cmbSource.getSelectedItem();
        String prevDst = (String) cmbDestination.getSelectedItem();
        String prevNodeA = (String) cmbConnectNodeA.getSelectedItem();
        String prevNodeB = (String) cmbConnectNodeB.getSelectedItem();
        String prevDelete = (String) cmbDeleteNode.getSelectedItem();

        cmbSource.setModel(new DefaultComboBoxModel<>(nodeIds.toArray(new String[0])));
        cmbDestination.setModel(new DefaultComboBoxModel<>(nodeIds.toArray(new String[0])));
        cmbConnectNodeA.setModel(new DefaultComboBoxModel<>(nodeIds.toArray(new String[0])));
        cmbConnectNodeB.setModel(new DefaultComboBoxModel<>(nodeIds.toArray(new String[0])));
        cmbDeleteNode.setModel(new DefaultComboBoxModel<>(nodeIds.toArray(new String[0])));

        // Restore or default selections
        if (prevSrc != null && nodeIds.contains(prevSrc)) {
            cmbSource.setSelectedItem(prevSrc);
        } else if (nodeIds.contains("PC1")) {
            cmbSource.setSelectedItem("PC1");
        }

        if (prevDst != null && nodeIds.contains(prevDst)) {
            cmbDestination.setSelectedItem(prevDst);
        } else if (nodeIds.contains("S1")) {
            cmbDestination.setSelectedItem("S1");
        }

        if (prevNodeA != null && nodeIds.contains(prevNodeA)) {
            cmbConnectNodeA.setSelectedItem(prevNodeA);
        } else if (!nodeIds.isEmpty()) {
            cmbConnectNodeA.setSelectedIndex(0);
        }

        if (prevNodeB != null && nodeIds.contains(prevNodeB)) {
            cmbConnectNodeB.setSelectedItem(prevNodeB);
        } else if (nodeIds.size() > 1) {
            cmbConnectNodeB.setSelectedIndex(1);
        }

        if (prevDelete != null && nodeIds.contains(prevDelete)) {
            cmbDeleteNode.setSelectedItem(prevDelete);
        } else if (!nodeIds.isEmpty()) {
            cmbDeleteNode.setSelectedIndex(0);
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
            linkDescriptions.add(id + " (" + link.getSourceId() + " - " + link.getDestinationId()
                    + ") [cost: " + formatCost(link.getCost()) + "] [" + status + "]");
        }

        int prevSelected = cmbLinks.getSelectedIndex();
        cmbLinks.setModel(new DefaultComboBoxModel<>(linkDescriptions.toArray(new String[0])));
        if (prevSelected >= 0 && prevSelected < linkDescriptions.size()) {
            cmbLinks.setSelectedIndex(prevSelected);
        }
    }

    public void setRouteResult(String text, boolean success) {
        lblRouteResult.setText(" " + text);
        lblRouteResult.setForeground(success ? Theme.ACCENT_CYAN : Theme.STATUS_ERROR);
    }

    private JPanel createRoutingPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(Theme.createTitledBorder("Route Calculation & Packet Dispatch"));

        JPanel formGrid = new JPanel(new GridLayout(2, 2, 8, 6));
        formGrid.setOpaque(false);

        JLabel lblSrc = new JLabel("Source Node:");
        lblSrc.setFont(Theme.FONT_BODY);
        lblSrc.setForeground(Theme.TEXT_SECONDARY);

        JLabel lblDst = new JLabel("Destination Node:");
        lblDst.setFont(Theme.FONT_BODY);
        lblDst.setForeground(Theme.TEXT_SECONDARY);

        formGrid.add(lblSrc);
        formGrid.add(cmbSource);
        formGrid.add(lblDst);
        formGrid.add(cmbDestination);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        btnRow.setOpaque(false);

        JButton btnFindRoute = Theme.createButton("Calculate Route", Theme.ButtonStyle.PRIMARY);
        JButton btnSendPacket = Theme.createButton("Send Packet & Run", Theme.ButtonStyle.SUCCESS);
        btnRow.add(btnFindRoute);
        btnRow.add(btnSendPacket);

        // Result Card
        JPanel resultCard = new JPanel(new BorderLayout());
        resultCard.setBackground(Theme.BG_INPUT);
        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        lblRouteResult.setFont(Theme.FONT_SMALL);
        lblRouteResult.setForeground(Theme.TEXT_MUTED);
        resultCard.add(lblRouteResult, BorderLayout.CENTER);

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
        panel.add(resultCard, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLinkControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(Theme.createTitledBorder("Link Control & Randomization"));

        JPanel linkSelectPanel = new JPanel(new BorderLayout(6, 4));
        linkSelectPanel.setOpaque(false);
        JLabel lblLink = new JLabel("Select Link:");
        lblLink.setFont(Theme.FONT_BODY);
        lblLink.setForeground(Theme.TEXT_SECONDARY);
        linkSelectPanel.add(lblLink, BorderLayout.WEST);
        linkSelectPanel.add(cmbLinks, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        btnRow.setOpaque(false);

        JButton btnFail = Theme.createButton("Fail Link", Theme.ButtonStyle.DESTRUCTIVE);
        JButton btnRestore = Theme.createButton("Restore Link", Theme.ButtonStyle.SUCCESS);
        JButton btnDeleteLink = Theme.createButton("Delete Link", Theme.ButtonStyle.DESTRUCTIVE);
        JButton btnRandomize = Theme.createButton("Randomize Costs", Theme.ButtonStyle.SECONDARY);

        btnRow.add(btnFail);
        btnRow.add(btnRestore);
        btnRow.add(btnDeleteLink);
        btnRow.add(btnRandomize);

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

        btnDeleteLink.addActionListener(e -> {
            String selected = (String) cmbLinks.getSelectedItem();
            if (selected == null || selected.isBlank()) {
                JOptionPane.showMessageDialog(this, "No link selected for deletion.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (listener != null) {
                String linkId = extractLinkId(selected);
                listener.onDeleteLink(linkId);
            }
        });

        btnRandomize.addActionListener(e -> {
            if (listener != null) {
                listener.onRandomizeLinkCosts();
            }
        });

        panel.add(linkSelectPanel, BorderLayout.NORTH);
        panel.add(btnRow, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createNetworkManagementPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(Theme.createTitledBorder("Dynamic Network Management"));

        // Row 1: Add Node controls
        JPanel nodeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        nodeRow.setOpaque(false);
        JLabel lblType = new JLabel("New Node Type:");
        lblType.setFont(Theme.FONT_BODY);
        lblType.setForeground(Theme.TEXT_SECONDARY);
        nodeRow.add(lblType);
        nodeRow.add(cmbNodeType);

        JButton btnAddNode = Theme.createButton("Add Node", Theme.ButtonStyle.PRIMARY);
        nodeRow.add(btnAddNode);

        btnAddNode.addActionListener(e -> {
            NodeType type = (NodeType) cmbNodeType.getSelectedItem();
            if (listener != null && type != null) {
                listener.onAddNode(type);
            }
        });

        // Row 2: Connect Nodes controls
        JPanel connectGrid = new JPanel(new GridLayout(2, 2, 6, 4));
        connectGrid.setOpaque(false);
        JLabel lblA = new JLabel("Node A:");
        lblA.setFont(Theme.FONT_BODY);
        lblA.setForeground(Theme.TEXT_SECONDARY);
        JLabel lblB = new JLabel("Node B:");
        lblB.setFont(Theme.FONT_BODY);
        lblB.setForeground(Theme.TEXT_SECONDARY);

        connectGrid.add(lblA);
        connectGrid.add(cmbConnectNodeA);
        connectGrid.add(lblB);
        connectGrid.add(cmbConnectNodeB);

        JPanel costRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        costRow.setOpaque(false);
        JLabel lblCost = new JLabel("Link Cost:");
        lblCost.setFont(Theme.FONT_BODY);
        lblCost.setForeground(Theme.TEXT_SECONDARY);
        costRow.add(lblCost);
        costRow.add(txtLinkCost);

        JButton btnConnect = Theme.createButton("Connect Nodes", Theme.ButtonStyle.PRIMARY);
        costRow.add(btnConnect);

        btnConnect.addActionListener(e -> {
            String nodeA = (String) cmbConnectNodeA.getSelectedItem();
            String nodeB = (String) cmbConnectNodeB.getSelectedItem();
            if (nodeA == null || nodeB == null) {
                JOptionPane.showMessageDialog(this, "Please select both nodes to connect.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (nodeA.equals(nodeB)) {
                JOptionPane.showMessageDialog(this, "Self-loops are not allowed. Please select two different nodes.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double cost;
            try {
                cost = Double.parseDouble(txtLinkCost.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric cost.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (Double.isNaN(cost) || Double.isInfinite(cost) || cost <= 0.0) {
                JOptionPane.showMessageDialog(this, "Link cost must be a finite positive number (> 0).", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (listener != null) {
                listener.onConnectNodes(nodeA, nodeB, cost);
            }
        });

        // Row 3: Delete Node controls
        JPanel deleteNodeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        deleteNodeRow.setOpaque(false);
        JLabel lblDel = new JLabel("Delete Node:");
        lblDel.setFont(Theme.FONT_BODY);
        lblDel.setForeground(Theme.TEXT_SECONDARY);
        deleteNodeRow.add(lblDel);
        deleteNodeRow.add(cmbDeleteNode);

        JButton btnDeleteNode = Theme.createButton("Delete Node", Theme.ButtonStyle.DESTRUCTIVE);
        deleteNodeRow.add(btnDeleteNode);

        btnDeleteNode.addActionListener(e -> {
            String selected = (String) cmbDeleteNode.getSelectedItem();
            if (selected == null || selected.isBlank()) {
                JOptionPane.showMessageDialog(this, "No node selected for deletion.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (listener != null) {
                listener.onDeleteNode(selected);
            }
        });

        panel.add(nodeRow);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(connectGrid);
        panel.add(costRow);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(deleteNodeRow);
        return panel;
    }

    private JButton btnSave;

    private JPanel createSimulationActionPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 8, 6));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(Theme.createTitledBorder("Simulation & Persistence Controls"));

        JButton btnStep = Theme.createButton("Step 1 Tick", Theme.ButtonStyle.SECONDARY);
        JButton btnReset = Theme.createButton("Reset Network", Theme.ButtonStyle.SECONDARY);
        btnSave = Theme.createButton("💾 Save", Theme.ButtonStyle.PRIMARY);

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

        btnSave.addActionListener(e -> {
            if (listener != null) {
                listener.onSave();
            }
        });

        panel.add(btnStep);
        panel.add(btnReset);
        panel.add(btnSave);
        return panel;
    }

    public JButton getBtnSave() {
        return btnSave;
    }

    public void triggerSave() {
        if (listener != null) {
            listener.onSave();
        }
    }

    /**
     * Backwards-compatible alias for {@link #getBtnSave()}.
     */
    public JButton getBtnSaveTopology() {
        return btnSave;
    }

    /**
     * Backwards-compatible alias for {@link #triggerSave()}.
     */
    public void triggerSaveTopology() {
        triggerSave();
    }

    /**
     * Backwards-compatible alias for {@link #getBtnSave()}.
     */
    public JButton getBtnFinishRun() {
        return btnSave;
    }

    /**
     * Backwards-compatible alias for {@link #triggerSave()}.
     */
    public void triggerFinishAndSaveRun() {
        triggerSave();
    }

    private static String extractLinkId(String text) {
        int spaceIndex = text.indexOf(' ');
        if (spaceIndex != -1) {
            return text.substring(0, spaceIndex).trim();
        }
        return text.trim();
    }

    private static String formatCost(double cost) {
        if (cost == (long) cost) {
            return String.valueOf((long) cost);
        }
        return String.format("%.1f", cost);
    }
}

package com.netroute.ui;

import com.netroute.graph.Network;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.model.Route;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Visual canvas for the network topology.
 * <p>
 * Renders nodes as shapes according to their {@link NodeType}, renders links
 * with their weights, highlights the active calculated route, and visually
 * distinguishes failed/inactive links. Supports dragging nodes to reposition them.
 */
public class TopologyPanel extends JPanel {

    private static final int NODE_WIDTH = 64;
    private static final int NODE_HEIGHT = 44;

    private static final Color COLOR_BG = new Color(248, 249, 250);
    private static final Color COLOR_LINK_ACTIVE = new Color(120, 144, 156);
    private static final Color COLOR_LINK_FAILED = new Color(220, 53, 69);
    private static final Color COLOR_LINK_ROUTE = new Color(13, 110, 253);

    private static final Color COLOR_COMPUTER = new Color(66, 133, 244);
    private static final Color COLOR_ROUTER = new Color(52, 168, 83);
    private static final Color COLOR_SERVER = new Color(142, 68, 173);

    private Network network;
    private Route highlightedRoute;
    private String selectedSourceId;
    private String selectedDestinationId;

    // Node dragging state
    private NetworkNode draggedNode;
    private double dragOffsetX;
    private double dragOffsetY;

    public TopologyPanel(Network network) {
        this.network = network;
        setBackground(COLOR_BG);
        setPreferredSize(new Dimension(750, 480));

        // Enable dragging nodes with the mouse
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (TopologyPanel.this.network == null) {
                    return;
                }
                for (NetworkNode node : TopologyPanel.this.network.getNodes().values()) {
                    if (isClickInsideNode(e.getX(), e.getY(), node)) {
                        draggedNode = node;
                        dragOffsetX = node.getX() - e.getX();
                        dragOffsetY = node.getY() - e.getY();
                        break;
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedNode != null) {
                    // Update node position and keep within canvas bounds
                    double newX = Math.max(40, Math.min(getWidth() - 40, e.getX() + dragOffsetX));
                    double newY = Math.max(30, Math.min(getHeight() - 40, e.getY() + dragOffsetY));
                    draggedNode.setPosition(newX, newY);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggedNode = null;
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setNetwork(Network network) {
        this.network = network;
        this.highlightedRoute = null;
        repaint();
    }

    public void setHighlightedRoute(Route route) {
        this.highlightedRoute = route;
        repaint();
    }

    public void clearHighlightedRoute() {
        this.highlightedRoute = null;
        repaint();
    }

    public void setSelectedEndpoints(String sourceId, String destinationId) {
        this.selectedSourceId = sourceId;
        this.selectedDestinationId = destinationId;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (network == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Set<String> routeLinkIds = highlightedRoute == null
                ? Collections.emptySet()
                : new HashSet<>(highlightedRoute.getLinkIds());

        Set<String> routeNodeIds = highlightedRoute == null
                ? Collections.emptySet()
                : new HashSet<>(highlightedRoute.getNodeIds());

        // 1. Draw all links
        for (NetworkLink link : network.getLinks().values()) {
            NetworkNode src = network.getNode(link.getSourceId());
            NetworkNode dst = network.getNode(link.getDestinationId());
            if (src == null || dst == null) {
                continue;
            }
            boolean isRouteLink = routeLinkIds.contains(link.getId());
            drawLink(g2, link, src, dst, isRouteLink);
        }

        // 2. Draw all nodes
        for (NetworkNode node : network.getNodes().values()) {
            boolean isRouteNode = routeNodeIds.contains(node.getId());
            boolean isSource = node.getId().equals(selectedSourceId);
            boolean isDest = node.getId().equals(selectedDestinationId);
            drawNode(g2, node, isRouteNode, isSource, isDest);
        }

        // 3. Draw Legend at top-right
        drawLegend(g2);
    }

    private void drawLink(Graphics2D g2, NetworkLink link, NetworkNode src, NetworkNode dst, boolean isRouteLink) {
        int x1 = (int) src.getX();
        int y1 = (int) src.getY();
        int x2 = (int) dst.getX();
        int y2 = (int) dst.getY();

        if (!link.isActive()) {
            // Failed link: dashed red line
            g2.setColor(COLOR_LINK_FAILED);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{7.0f, 6.0f}, 0.0f));
        } else if (isRouteLink) {
            // Route link: bold blue line
            g2.setColor(COLOR_LINK_ROUTE);
            g2.setStroke(new BasicStroke(4.0f));
        } else {
            // Normal active link
            g2.setColor(COLOR_LINK_ACTIVE);
            g2.setStroke(new BasicStroke(2.0f));
        }

        g2.drawLine(x1, y1, x2, y2);

        // Draw arrow if directed
        if (link.isDirected()) {
            drawArrowHead(g2, x1, y1, x2, y2);
        }

        // Link badge / label
        int midX = (x1 + x2) / 2;
        int midY = (y1 + y2) / 2;

        String label = link.getId() + " (cost: " + formatNumber(link.getCost()) + ")";
        if (!link.isActive()) {
            label = link.getId() + " [FAILED]";
        }

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getHeight();

        int boxPad = 3;
        int boxX = midX - (textWidth / 2) - boxPad;
        int boxY = midY - (textHeight / 2) - boxPad;

        // Badge background
        g2.setColor(link.isActive() ? new Color(255, 255, 255, 220) : new Color(255, 235, 238, 230));
        g2.fillRoundRect(boxX, boxY, textWidth + (boxPad * 2), textHeight + (boxPad * 2), 6, 6);

        // Badge border
        g2.setColor(link.isActive() ? Color.LIGHT_GRAY : COLOR_LINK_FAILED);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(boxX, boxY, textWidth + (boxPad * 2), textHeight + (boxPad * 2), 6, 6);

        // Text
        g2.setColor(link.isActive() ? Color.DARK_GRAY : COLOR_LINK_FAILED);
        g2.drawString(label, boxX + boxPad, boxY + boxPad + fm.getAscent());
    }

    private void drawNode(Graphics2D g2, NetworkNode node, boolean isRouteNode, boolean isSource, boolean isDest) {
        int cx = (int) node.getX();
        int cy = (int) node.getY();
        int x = cx - (NODE_WIDTH / 2);
        int y = cy - (NODE_HEIGHT / 2);

        // Highlight ring if source, destination, or part of route
        if (isSource) {
            g2.setColor(new Color(40, 167, 69, 140)); // Green halo
            g2.setStroke(new BasicStroke(6.0f));
            g2.drawRoundRect(x - 5, y - 5, NODE_WIDTH + 10, NODE_HEIGHT + 10, 16, 16);
        } else if (isDest) {
            g2.setColor(new Color(255, 152, 0, 160)); // Orange halo
            g2.setStroke(new BasicStroke(6.0f));
            g2.drawRoundRect(x - 5, y - 5, NODE_WIDTH + 10, NODE_HEIGHT + 10, 16, 16);
        } else if (isRouteNode) {
            g2.setColor(new Color(13, 110, 253, 130)); // Blue halo
            g2.setStroke(new BasicStroke(4.0f));
            g2.drawRoundRect(x - 3, y - 3, NODE_WIDTH + 6, NODE_HEIGHT + 6, 14, 14);
        }

        // Main node shape fill
        Color nodeColor = switch (node.getType()) {
            case COMPUTER -> COLOR_COMPUTER;
            case ROUTER -> COLOR_ROUTER;
            case SERVER -> COLOR_SERVER;
        };
        g2.setColor(nodeColor);
        g2.fill(new RoundRectangle2D.Double(x, y, NODE_WIDTH, NODE_HEIGHT, 14, 14));

        // Node border
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new RoundRectangle2D.Double(x, y, NODE_WIDTH, NODE_HEIGHT, 14, 14));

        // Node ID inside the shape
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int idWidth = fm.stringWidth(node.getId());
        g2.drawString(node.getId(), cx - (idWidth / 2), cy + (fm.getAscent() / 2) - 2);

        // Node Type badge / text below
        String subLabel = node.getName();
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics subFm = g2.getFontMetrics();
        int subWidth = subFm.stringWidth(subLabel);

        // Sublabel background
        int subX = cx - (subWidth / 2) - 4;
        int subY = y + NODE_HEIGHT + 4;
        g2.setColor(new Color(255, 255, 255, 210));
        g2.fillRect(subX, subY, subWidth + 8, subFm.getHeight() + 2);

        g2.setColor(Color.DARK_GRAY);
        g2.drawString(subLabel, cx - (subWidth / 2), subY + subFm.getAscent() + 1);
    }

    private void drawLegend(Graphics2D g2) {
        int x = getWidth() - 170;
        int y = 10;
        if (x < 10) {
            return;
        }

        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(x, y, 160, 95, 8, 8);
        g2.setColor(Color.LIGHT_GRAY);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(x, y, 160, 95, 8, 8);

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(Color.BLACK);
        g2.drawString("Node Types", x + 10, y + 16);

        drawLegendItem(g2, x + 10, y + 25, COLOR_COMPUTER, "Computer");
        drawLegendItem(g2, x + 10, y + 43, COLOR_ROUTER, "Router");
        drawLegendItem(g2, x + 10, y + 61, COLOR_SERVER, "Server");
        drawLegendItem(g2, x + 10, y + 79, COLOR_LINK_FAILED, "Failed Link");
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String text) {
        g2.setColor(color);
        g2.fillRoundRect(x, y, 12, 12, 3, 3);
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(text, x + 18, y + 10);
    }

    private void drawArrowHead(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length < 1) {
            return;
        }
        // Place arrow near 70% of the line
        double arrowX = x1 + (dx * 0.7);
        double arrowY = y1 + (dy * 0.7);
        double angle = Math.atan2(dy, dx);

        int arrowSize = 8;
        int xA = (int) (arrowX - arrowSize * Math.cos(angle - Math.PI / 6));
        int yA = (int) (arrowY - arrowSize * Math.sin(angle - Math.PI / 6));
        int xB = (int) (arrowX - arrowSize * Math.cos(angle + Math.PI / 6));
        int yB = (int) (arrowY - arrowSize * Math.sin(angle + Math.PI / 6));

        g2.fillPolygon(new int[]{(int) arrowX, xA, xB}, new int[]{(int) arrowY, yA, yB}, 3);
    }

    private boolean isClickInsideNode(int clickX, int clickY, NetworkNode node) {
        double halfW = NODE_WIDTH / 2.0;
        double halfH = NODE_HEIGHT / 2.0;
        return clickX >= (node.getX() - halfW) && clickX <= (node.getX() + halfW)
                && clickY >= (node.getY() - halfH) && clickY <= (node.getY() + halfH);
    }

    private static String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%.1f", value);
    }
}

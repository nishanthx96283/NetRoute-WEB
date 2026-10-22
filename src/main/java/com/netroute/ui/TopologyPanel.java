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
 * Visual canvas for the network topology with modern dark theme styling.
 * <p>
 * Renders nodes as engineering equipment (Computers, Routers, Servers) on a dark slate
 * technical grid, renders links with weights, highlights the active Dijkstra route in glowing cyan,
 * and visually distinguishes failed/inactive links. Supports dragging nodes to reposition them.
 */
public class TopologyPanel extends JPanel {

    private static final int NODE_WIDTH = 68;
    private static final int NODE_HEIGHT = 44;

    // Link Colors
    private static final Color COLOR_LINK_NORMAL = new Color(0x4A, 0x64, 0x7D);
    private static final Color COLOR_LINK_FAILED = Theme.STATUS_ERROR;
    private static final Color COLOR_LINK_ROUTE = Theme.ACCENT_CYAN;
    private static final Color COLOR_LINK_GLOW = new Color(0x43, 0xC6, 0xD9, 60);

    // Node Type Colors
    private static final Color COLOR_COMPUTER = new Color(0x3B, 0x9A, 0xEE); // Bright tech blue
    private static final Color COLOR_ROUTER = new Color(0x2E, 0xC4, 0xB6);   // Teal / routing cyan
    private static final Color COLOR_SERVER = new Color(0x9E, 0x7A, 0xD2);   // Server purple

    // Grid Color
    private static final Color COLOR_GRID = new Color(0x1D, 0x28, 0x34);

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
        setBackground(Theme.BG_MAIN);
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
                    double newX = Math.max(45, Math.min(getWidth() - 45, e.getX() + dragOffsetX));
                    double newY = Math.max(35, Math.min(getHeight() - 45, e.getY() + dragOffsetY));
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
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Draw Subtle Technical Grid
        drawTechnicalGrid(g2);

        if (network == null) {
            return;
        }

        Set<String> routeLinkIds = highlightedRoute == null
                ? Collections.emptySet()
                : new HashSet<>(highlightedRoute.getLinkIds());

        Set<String> routeNodeIds = highlightedRoute == null
                ? Collections.emptySet()
                : new HashSet<>(highlightedRoute.getNodeIds());

        // 2. Draw all links
        for (NetworkLink link : network.getLinks().values()) {
            NetworkNode src = network.getNode(link.getSourceId());
            NetworkNode dst = network.getNode(link.getDestinationId());
            if (src == null || dst == null) {
                continue;
            }
            boolean isRouteLink = routeLinkIds.contains(link.getId());
            drawLink(g2, link, src, dst, isRouteLink);
        }

        // 3. Draw all nodes
        for (NetworkNode node : network.getNodes().values()) {
            boolean isRouteNode = routeNodeIds.contains(node.getId());
            boolean isSource = node.getId().equals(selectedSourceId);
            boolean isDest = node.getId().equals(selectedDestinationId);
            drawNode(g2, node, isRouteNode, isSource, isDest);
        }

        // 4. Draw Legend in top-right
        drawLegend(g2);
    }

    private void drawTechnicalGrid(Graphics2D g2) {
        int width = getWidth();
        int height = getHeight();

        g2.setColor(COLOR_GRID);
        g2.setStroke(new BasicStroke(1.0f));

        // 32-pixel grid spacing
        for (int x = 0; x < width; x += 32) {
            g2.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += 32) {
            g2.drawLine(0, y, width, y);
        }
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
            g2.drawLine(x1, y1, x2, y2);
        } else if (isRouteLink) {
            // Route link: glowing cyan line
            g2.setColor(COLOR_LINK_GLOW);
            g2.setStroke(new BasicStroke(8.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);

            g2.setColor(COLOR_LINK_ROUTE);
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
        } else {
            // Normal active link
            g2.setColor(COLOR_LINK_NORMAL);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(x1, y1, x2, y2);
        }

        // Draw arrow if directed
        if (link.isDirected()) {
            drawArrowHead(g2, x1, y1, x2, y2);
        }

        // Link badge / label
        int midX = (x1 + x2) / 2;
        int midY = (y1 + y2) / 2;

        String label = link.getId() + " [c:" + formatNumber(link.getCost()) + "]";
        if (!link.isActive()) {
            label = link.getId() + " [FAILED]";
        }

        g2.setFont(Theme.FONT_SMALL);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getHeight();

        int boxPadX = 5;
        int boxPadY = 2;
        int boxX = midX - (textWidth / 2) - boxPadX;
        int boxY = midY - (textHeight / 2) - boxPadY;

        // Badge pill background
        g2.setColor(link.isActive() ? Theme.BG_PANEL : Theme.STATUS_ERROR_BG);
        g2.fillRoundRect(boxX, boxY, textWidth + (boxPadX * 2), textHeight + (boxPadY * 2), 8, 8);

        // Badge border
        g2.setColor(link.isActive() ? Theme.BORDER_SUBTLE : Theme.STATUS_ERROR);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(boxX, boxY, textWidth + (boxPadX * 2), textHeight + (boxPadY * 2), 8, 8);

        // Text
        g2.setColor(link.isActive() ? Theme.TEXT_PRIMARY : Theme.STATUS_ERROR);
        g2.drawString(label, boxX + boxPadX, boxY + boxPadY + fm.getAscent());
    }

    private void drawNode(Graphics2D g2, NetworkNode node, boolean isRouteNode, boolean isSource, boolean isDest) {
        int cx = (int) node.getX();
        int cy = (int) node.getY();
        int x = cx - (NODE_WIDTH / 2);
        int y = cy - (NODE_HEIGHT / 2);

        // 1. Halo Rings for Selection and Active Route
        if (isSource) {
            g2.setColor(new Color(0x4E, 0xBA, 0x6F, 160)); // Green halo
            g2.setStroke(new BasicStroke(5.0f));
            g2.drawRoundRect(x - 5, y - 5, NODE_WIDTH + 10, NODE_HEIGHT + 10, 16, 16);
        } else if (isDest) {
            g2.setColor(new Color(0xE5, 0xA9, 0x3C, 180)); // Amber halo
            g2.setStroke(new BasicStroke(5.0f));
            g2.drawRoundRect(x - 5, y - 5, NODE_WIDTH + 10, NODE_HEIGHT + 10, 16, 16);
        } else if (isRouteNode) {
            g2.setColor(new Color(0x43, 0xC6, 0xD9, 140)); // Cyan halo
            g2.setStroke(new BasicStroke(4.0f));
            g2.drawRoundRect(x - 3, y - 3, NODE_WIDTH + 6, NODE_HEIGHT + 6, 14, 14);
        }

        // 2. Main Node Shape
        Color accentColor = switch (node.getType()) {
            case COMPUTER -> COLOR_COMPUTER;
            case ROUTER -> COLOR_ROUTER;
            case SERVER -> COLOR_SERVER;
        };

        // Dark elevated body
        g2.setColor(Theme.BG_ELEVATED);
        g2.fill(new RoundRectangle2D.Double(x, y, NODE_WIDTH, NODE_HEIGHT, 12, 12));

        // Accent top bar / bezel
        g2.setColor(accentColor);
        g2.fillRoundRect(x, y, NODE_WIDTH, 7, 10, 10);
        g2.fillRect(x, y + 4, NODE_WIDTH, 4);

        // Node border
        g2.setColor(isRouteNode ? Theme.ACCENT_CYAN : Theme.BORDER_SUBTLE);
        g2.setStroke(new BasicStroke(isRouteNode ? 2.0f : 1.2f));
        g2.draw(new RoundRectangle2D.Double(x, y, NODE_WIDTH, NODE_HEIGHT, 12, 12));

        // Node ID inside
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_BODY_BOLD);
        FontMetrics fm = g2.getFontMetrics();
        int idWidth = fm.stringWidth(node.getId());
        g2.drawString(node.getId(), cx - (idWidth / 2), cy + (fm.getAscent() / 2) + 2);

        // 3. Status Badge (SRC / DST)
        if (isSource) {
            drawEndpointTag(g2, x + NODE_WIDTH - 20, y - 7, "SRC", Theme.STATUS_SUCCESS);
        } else if (isDest) {
            drawEndpointTag(g2, x + NODE_WIDTH - 20, y - 7, "DST", Theme.STATUS_WARNING);
        }

        // 4. Sublabel pill below
        String subLabel = node.getName();
        g2.setFont(Theme.FONT_SMALL);
        FontMetrics subFm = g2.getFontMetrics();
        int subWidth = subFm.stringWidth(subLabel);

        int subPadX = 6;
        int subPadY = 1;
        int subX = cx - (subWidth / 2) - subPadX;
        int subY = y + NODE_HEIGHT + 4;

        g2.setColor(Theme.BG_PANEL);
        g2.fillRoundRect(subX, subY, subWidth + (subPadX * 2), subFm.getHeight() + (subPadY * 2), 6, 6);
        g2.setColor(Theme.BORDER_SUBTLE);
        g2.drawRoundRect(subX, subY, subWidth + (subPadX * 2), subFm.getHeight() + (subPadY * 2), 6, 6);

        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString(subLabel, cx - (subWidth / 2), subY + subFm.getAscent() + subPadY);
    }

    private void drawEndpointTag(Graphics2D g2, int x, int y, String tag, Color color) {
        g2.setColor(color);
        g2.fillRoundRect(x, y, 24, 12, 4, 4);
        g2.setColor(Theme.TEXT_DARK);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        g2.drawString(tag, x + 3, y + 9);
    }

    private void drawLegend(Graphics2D g2) {
        int width = 175;
        int height = 130;
        int x = getWidth() - width - 12;
        int y = 12;
        if (x < 10) {
            return;
        }

        // Legend Card Background
        g2.setColor(Theme.BG_PANEL);
        g2.fillRoundRect(x, y, width, height, 10, 10);
        g2.setColor(Theme.BORDER_SUBTLE);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(x, y, width, height, 10, 10);

        // Legend Title
        g2.setFont(Theme.FONT_BODY_BOLD);
        g2.setColor(Theme.ACCENT_CYAN);
        g2.drawString("Topology Legend", x + 12, y + 18);

        // Items
        drawLegendItem(g2, x + 12, y + 28, COLOR_COMPUTER, "Computer (PC)");
        drawLegendItem(g2, x + 12, y + 46, COLOR_ROUTER, "Router (R)");
        drawLegendItem(g2, x + 12, y + 64, COLOR_SERVER, "Server (S)");
        drawLegendItem(g2, x + 12, y + 82, COLOR_LINK_NORMAL, "Active Link");
        drawLegendItem(g2, x + 12, y + 100, COLOR_LINK_FAILED, "Failed Link");
        drawLegendItem(g2, x + 12, y + 118, COLOR_LINK_ROUTE, "Shortest Route");
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String text) {
        g2.setColor(color);
        g2.fillRoundRect(x, y, 10, 10, 3, 3);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.setFont(Theme.FONT_SMALL);
        g2.drawString(text, x + 18, y + 9);
    }

    private void drawArrowHead(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        if (length < 1) {
            return;
        }
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

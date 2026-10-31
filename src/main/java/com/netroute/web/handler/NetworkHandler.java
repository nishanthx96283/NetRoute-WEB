package com.netroute.web.handler;

import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.web.HttpUtils;
import com.netroute.web.WebApplicationState;
import com.netroute.web.dto.LinkDto;
import com.netroute.web.dto.NodeDto;
import com.netroute.web.dto.Requests;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class NetworkHandler implements HttpHandler {

    private final WebApplicationState state;

    public NetworkHandler(WebApplicationState state) {
        this.state = Objects.requireNonNull(state, "state is required");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("/api/network".equals(path)) {
                if ("GET".equalsIgnoreCase(method)) {
                    HttpUtils.sendSuccess(exchange, state.getNetworkSnapshot());
                } else {
                    HttpUtils.sendError(exchange, 405, "Method Not Allowed");
                }
            } else if ("/api/network/reset".equals(path) && "POST".equalsIgnoreCase(method)) {
                state.resetToSampleNetwork();
                HttpUtils.sendSuccess(exchange, "Network reset to sample campus network", state.getNetworkSnapshot());
            } else if ("/api/network/new-project".equals(path) && "POST".equalsIgnoreCase(method)) {
                state.newProject();
                HttpUtils.sendSuccess(exchange, "Created new blank network project", state.getNetworkSnapshot());
            } else if ("/api/network/randomize-costs".equals(path) && "POST".equalsIgnoreCase(method)) {
                state.randomizeLinkCosts();
                HttpUtils.sendSuccess(exchange, "Link costs randomized", state.getNetworkSnapshot());
            } else if ("/api/network/node".equals(path)) {
                if ("POST".equalsIgnoreCase(method)) {
                    handleAddNode(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteNode(exchange);
                } else {
                    HttpUtils.sendError(exchange, 405, "Method Not Allowed");
                }
            } else if ("/api/network/node/position".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleUpdatePosition(exchange);
            } else if ("/api/network/link".equals(path)) {
                if ("POST".equalsIgnoreCase(method)) {
                    handleAddLink(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteLink(exchange);
                } else {
                    HttpUtils.sendError(exchange, 405, "Method Not Allowed");
                }
            } else {
                HttpUtils.sendError(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleAddNode(HttpExchange exchange) throws IOException {
        Requests.CreateNodeRequest req = HttpUtils.readJson(exchange, Requests.CreateNodeRequest.class);
        if (req == null || req.type() == null) {
            HttpUtils.sendError(exchange, 400, "Node type is required (COMPUTER, ROUTER, SERVER)");
            return;
        }
        NodeType type;
        try {
            type = NodeType.valueOf(req.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, "Invalid node type: " + req.type());
            return;
        }

        NetworkNode node = state.addNode(type, req.name(), req.x(), req.y());
        HttpUtils.sendSuccess(exchange, "Node added", NodeDto.from(node));
    }

    private void handleDeleteNode(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtils.parseQueryParams(exchange);
        String nodeId = query.get("id");
        if (nodeId == null || nodeId.isBlank()) {
            Requests.LinkActionRequest req = HttpUtils.readJson(exchange, Requests.LinkActionRequest.class);
            if (req != null) {
                nodeId = req.linkId(); // Reused identifier field if sent via body
            }
        }
        if (nodeId == null || nodeId.isBlank()) {
            HttpUtils.sendError(exchange, 400, "Missing node ID to delete");
            return;
        }

        state.deleteNode(nodeId.trim());
        HttpUtils.sendSuccess(exchange, "Node deleted", state.getNetworkSnapshot());
    }

    private void handleUpdatePosition(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtils.parseQueryParams(exchange);
        String nodeId = query.get("id");
        Requests.UpdatePositionRequest req = HttpUtils.readJson(exchange, Requests.UpdatePositionRequest.class);
        if (nodeId == null || nodeId.isBlank() || req == null) {
            HttpUtils.sendError(exchange, 400, "Node ID and position coordinates (x, y) are required");
            return;
        }
        state.updateNodePosition(nodeId.trim(), req.x(), req.y());
        HttpUtils.sendSuccess(exchange, "Position updated", null);
    }

    private void handleAddLink(HttpExchange exchange) throws IOException {
        Requests.ConnectNodesRequest req = HttpUtils.readJson(exchange, Requests.ConnectNodesRequest.class);
        if (req == null) {
            HttpUtils.sendError(exchange, 400, "Missing link creation payload");
            return;
        }
        NetworkLink link = state.connectNodes(
                req.sourceId(),
                req.destinationId(),
                req.cost(),
                req.delay(),
                req.capacity(),
                req.directed()
        );
        HttpUtils.sendSuccess(exchange, "Link created", LinkDto.from(link));
    }

    private void handleDeleteLink(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtils.parseQueryParams(exchange);
        String linkId = query.get("id");
        if (linkId == null || linkId.isBlank()) {
            Requests.LinkActionRequest req = HttpUtils.readJson(exchange, Requests.LinkActionRequest.class);
            if (req != null) {
                linkId = req.linkId();
            }
        }
        if (linkId == null || linkId.isBlank()) {
            HttpUtils.sendError(exchange, 400, "Missing link ID to delete");
            return;
        }
        state.deleteLink(linkId.trim());
        HttpUtils.sendSuccess(exchange, "Link deleted", state.getNetworkSnapshot());
    }
}

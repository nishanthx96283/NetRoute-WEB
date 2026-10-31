package com.netroute.web.handler;

import com.netroute.graph.Network;
import com.netroute.repository.DatabaseConnection;
import com.netroute.repository.DuplicateTopologyNameException;
import com.netroute.repository.SavedTopology;
import com.netroute.repository.SimulationRunRecord;
import com.netroute.repository.TopologySummary;
import com.netroute.repository.UnifiedSaveResult;
import com.netroute.service.TopologyStorageService;
import com.netroute.statistics.NetworkStatistics;
import com.netroute.web.HttpUtils;
import com.netroute.web.WebApplicationState;
import com.netroute.web.dto.Requests;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class HistoryHandler implements HttpHandler {

    private final WebApplicationState state;

    public HistoryHandler(WebApplicationState state) {
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
            if ("/api/history/topologies".equals(path)) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleListTopologies(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteTopology(exchange);
                } else {
                    HttpUtils.sendError(exchange, 405, "Method Not Allowed");
                }
            } else if ("/api/history/topologies/save".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleSaveTopology(exchange);
            } else if ("/api/history/topologies/load".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleLoadTopology(exchange);
            } else if ("/api/history/topologies/delete".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleDeleteTopology(exchange);
            } else if ("/api/history/runs".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleListSimulationRuns(exchange);
            } else if ("/api/db/status".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleDatabaseStatus(exchange);
            } else {
                HttpUtils.sendError(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (DuplicateTopologyNameException e) {
            Map<String, Object> errData = new HashMap<>();
            errData.put("duplicateName", e.getTopologyName());
            errData.put("existingId", e.getExistingTopologyId());
            errData.put("isDuplicate", true);
            HttpUtils.sendJson(exchange, 409, com.netroute.web.dto.ApiResponse.failure("A topology named '" + e.getTopologyName() + "' already exists in the database."));
        } catch (SQLException e) {
            HttpUtils.sendError(exchange, 503, "Database Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Persistence Error: " + e.getMessage());
        }
    }

    private void handleListTopologies(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> query = HttpUtils.parseQueryParams(exchange);
        boolean includeArchived = "true".equalsIgnoreCase(query.get("includeArchived"));

        TopologyStorageService service = state.getStorageService();
        List<TopologySummary> topologies = service.listAllTopologies(includeArchived);
        HttpUtils.sendSuccess(exchange, topologies);
    }

    private void handleSaveTopology(HttpExchange exchange) throws IOException, SQLException, DuplicateTopologyNameException {
        Requests.SaveTopologyRequest req = HttpUtils.readJson(exchange, Requests.SaveTopologyRequest.class);
        if (req == null || req.name() == null || req.name().isBlank()) {
            HttpUtils.sendError(exchange, 400, "Topology name cannot be empty.");
            return;
        }

        String name = req.name().trim();
        String description = req.description() != null ? req.description().trim() : "";
        boolean overwrite = req.overwrite() != null && req.overwrite();
        boolean recordRun = req.recordRun() != null && req.recordRun();
        String runNotes = req.runNotes() != null ? req.runNotes().trim() : "";

        Integer targetId = req.topologyId() != null ? req.topologyId() : state.getActiveTopologyId();
        Network network = state.getNetwork();

        if (network.nodeCount() == 0) {
            HttpUtils.sendError(exchange, 400, "Cannot save an empty network.");
            return;
        }

        NetworkStatistics stats = recordRun ? state.getStatistics() : null;
        long ticks = state.getEngine().getCurrentTime();

        TopologyStorageService service = state.getStorageService();
        UnifiedSaveResult result = service.saveUnified(
                targetId,
                name,
                description,
                network,
                overwrite,
                stats,
                ticks,
                runNotes
        );

        state.setActiveTopologyId(result.getTopologyId());
        state.setActiveTopologyName(result.getTopologyName());
        state.setActiveTopologyDescription(description);
        state.setTopologyModified(false);
        if (result.hasSimulationRun()) {
            state.setCurrentRunSaved(true);
            state.setLastSavedRunId(result.getSimulationRunId());
        }

        state.logEvent("Saved topology '" + name + "' (ID: " + result.getTopologyId() + ")" +
                (result.hasSimulationRun() ? " with simulation run #" + result.getSimulationRunId() : "") + ".");

        Map<String, Object> resp = new HashMap<>();
        resp.put("topologyId", result.getTopologyId());
        resp.put("topologyName", result.getTopologyName());
        resp.put("created", result.isCreated());
        resp.put("simulationRunId", result.getSimulationRunId());
        resp.put("network", state.getNetworkSnapshot());

        HttpUtils.sendSuccess(exchange, "Topology saved successfully", resp);
    }

    private void handleLoadTopology(HttpExchange exchange) throws IOException, SQLException {
        Requests.LoadTopologyRequest req = HttpUtils.readJson(exchange, Requests.LoadTopologyRequest.class);
        if (req == null || req.topologyId() <= 0) {
            HttpUtils.sendError(exchange, 400, "Valid topologyId is required");
            return;
        }

        TopologyStorageService service = state.getStorageService();
        Optional<SavedTopology> savedOpt = service.loadTopology(req.topologyId());
        if (savedOpt.isEmpty()) {
            HttpUtils.sendError(exchange, 404, "Topology not found with ID: " + req.topologyId());
            return;
        }

        SavedTopology saved = savedOpt.get();
        state.loadTopology(saved.getId(), saved.getName(), saved.getDescription(), saved.getNetwork());
        HttpUtils.sendSuccess(exchange, "Topology loaded successfully", state.getNetworkSnapshot());
    }

    private void handleDeleteTopology(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> query = HttpUtils.parseQueryParams(exchange);
        String idStr = query.get("id");
        if (idStr == null || idStr.isBlank()) {
            Requests.LoadTopologyRequest req = HttpUtils.readJson(exchange, Requests.LoadTopologyRequest.class);
            if (req != null && req.topologyId() > 0) {
                idStr = String.valueOf(req.topologyId());
            }
        }
        if (idStr == null || idStr.isBlank()) {
            HttpUtils.sendError(exchange, 400, "Topology ID is required");
            return;
        }

        int id = Integer.parseInt(idStr.trim());
        boolean permanent = "true".equalsIgnoreCase(query.get("permanent"));

        TopologyStorageService service = state.getStorageService();
        if (permanent) {
            service.deleteTopologyPermanently(id);
            state.logEvent("Permanently deleted topology ID: " + id);
        } else {
            service.archiveTopology(id);
            state.logEvent("Archived topology ID: " + id);
        }

        // If deleted topology was the currently active one, disassociate it
        if (state.getActiveTopologyId() != null && state.getActiveTopologyId() == id) {
            state.setActiveTopologyId(null);
        }

        HttpUtils.sendSuccess(exchange, "Topology deleted successfully", null);
    }

    private void handleListSimulationRuns(HttpExchange exchange) throws IOException, SQLException {
        TopologyStorageService service = state.getStorageService();
        List<SimulationRunRecord> runs = service.listAllSimulationRuns();
        HttpUtils.sendSuccess(exchange, runs);
    }

    private void handleDatabaseStatus(HttpExchange exchange) throws IOException {
        DatabaseConnection.ConnectionResult diag = state.getStorageService().testConnection();
        Map<String, Object> status = new HashMap<>();
        status.put("available", diag.isSuccess());
        status.put("message", diag.getMessage());
        status.put("databaseProduct", diag.getDatabaseProductName());
        status.put("databaseVersion", diag.getDatabaseProductVersion());
        status.put("responseTimeMs", diag.getResponseTimeMs());
        HttpUtils.sendSuccess(exchange, status);
    }
}

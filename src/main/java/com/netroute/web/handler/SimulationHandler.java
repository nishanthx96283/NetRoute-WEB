package com.netroute.web.handler;

import com.netroute.model.NetworkLink;
import com.netroute.model.Packet;
import com.netroute.model.PacketStatus;
import com.netroute.simulation.SimulationEngine;
import com.netroute.web.HttpUtils;
import com.netroute.web.WebApplicationState;
import com.netroute.web.dto.PacketDto;
import com.netroute.web.dto.Requests;
import com.netroute.web.dto.SimulationStateDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimulationHandler implements HttpHandler {

    private final WebApplicationState state;

    public SimulationHandler(WebApplicationState state) {
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
            if ("/api/simulation/state".equals(path) && "GET".equalsIgnoreCase(method)) {
                HttpUtils.sendSuccess(exchange, buildStateDto());
            } else if ("/api/simulation/packet".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleSubmitPacket(exchange);
            } else if ("/api/simulation/step".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleStep(exchange);
            } else if ("/api/simulation/run".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleRun(exchange);
            } else if ("/api/simulation/reset".equals(path) && "POST".equalsIgnoreCase(method)) {
                state.resetSimulationEngine();
                HttpUtils.sendSuccess(exchange, "Simulation engine reset", buildStateDto());
            } else if ("/api/simulation/fail-link".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleFailLink(exchange);
            } else if ("/api/simulation/restore-link".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleRestoreLink(exchange);
            } else if ("/api/simulation/events".equals(path) && "GET".equalsIgnoreCase(method)) {
                HttpUtils.sendSuccess(exchange, state.getRecentEvents());
            } else {
                HttpUtils.sendError(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Simulation error: " + e.getMessage());
        }
    }

    private void handleSubmitPacket(HttpExchange exchange) throws IOException {
        Requests.SubmitPacketRequest req = HttpUtils.readJson(exchange, Requests.SubmitPacketRequest.class);
        if (req == null || req.sourceId() == null || req.destinationId() == null) {
            HttpUtils.sendError(exchange, 400, "sourceId and destinationId are required");
            return;
        }
        int payloadSize = (req.payloadSize() != null && req.payloadSize() >= 1) ? req.payloadSize() : 64;
        String src = req.sourceId().trim();
        String dst = req.destinationId().trim();

        Packet packet = state.getEngine().submitPacket(src, dst, payloadSize);
        String eventMsg = "Submitted packet " + packet.getId() + " (" + src + " -> " + dst + ", payload=" + payloadSize + "B). Status: " + packet.getStatus();
        if (packet.getStatus() == PacketStatus.FAILED) {
            eventMsg += " Reason: " + packet.getReason().orElse("Unknown");
        }
        state.logEvent(eventMsg);

        HttpUtils.sendSuccess(exchange, "Packet submitted", buildStateDto());
    }

    private void handleStep(HttpExchange exchange) throws IOException {
        SimulationEngine engine = state.getEngine();
        long prevTime = engine.getCurrentTime();
        engine.step();
        long newTime = engine.getCurrentTime();

        state.logEvent("Simulated tick: " + prevTime + " -> " + newTime + ". (Idle=" + engine.isIdle() + ")");
        HttpUtils.sendSuccess(exchange, "Advanced 1 tick", buildStateDto());
    }

    private void handleRun(HttpExchange exchange) throws IOException {
        SimulationEngine engine = state.getEngine();
        long startTick = engine.getCurrentTime();
        engine.run();
        long endTick = engine.getCurrentTime();

        state.logEvent("Simulation completed to idle. Ticks: " + startTick + " -> " + endTick + ". Delivered: " + engine.getDeliveredCount() + ", Dropped: " + engine.getDroppedCount());
        HttpUtils.sendSuccess(exchange, "Simulation completed", buildStateDto());
    }

    private void handleFailLink(HttpExchange exchange) throws IOException {
        Requests.LinkActionRequest req = HttpUtils.readJson(exchange, Requests.LinkActionRequest.class);
        if (req == null || req.linkId() == null || req.linkId().isBlank()) {
            HttpUtils.sendError(exchange, 400, "linkId is required");
            return;
        }
        state.failLink(req.linkId().trim());
        HttpUtils.sendSuccess(exchange, "Link marked as failed", buildStateDto());
    }

    private void handleRestoreLink(HttpExchange exchange) throws IOException {
        Requests.LinkActionRequest req = HttpUtils.readJson(exchange, Requests.LinkActionRequest.class);
        if (req == null || req.linkId() == null || req.linkId().isBlank()) {
            HttpUtils.sendError(exchange, 400, "linkId is required");
            return;
        }
        state.restoreLink(req.linkId().trim());
        HttpUtils.sendSuccess(exchange, "Link restored", buildStateDto());
    }

    private SimulationStateDto buildStateDto() {
        SimulationEngine engine = state.getEngine();
        List<PacketDto> packetDtos = engine.getPackets().stream().map(PacketDto::from).toList();

        Map<String, Integer> queueSizes = new HashMap<>();
        for (NetworkLink link : state.getNetwork().getLinks().values()) {
            int qSize = engine.hasQueueFor(link.getId()) ? engine.queueFor(link.getId()).size() : 0;
            queueSizes.put(link.getId(), qSize);
        }

        int inFlightCount = 0;
        for (Packet p : engine.getPackets()) {
            if (p.getStatus() == PacketStatus.IN_TRANSIT) {
                inFlightCount++;
            }
        }

        return new SimulationStateDto(
                engine.getCurrentTime(),
                engine.isIdle(),
                packetDtos,
                queueSizes,
                inFlightCount,
                engine.getSentCount(),
                engine.getDeliveredCount(),
                engine.getFailedCount(),
                engine.getDroppedCount(),
                engine.getLinkFailureCount(),
                state.getRecentEvents()
        );
    }
}

package com.netroute.web.handler;

import com.netroute.web.HttpUtils;
import com.netroute.web.WebApplicationState;
import com.netroute.web.dto.StatisticsDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Objects;

public class StatisticsHandler implements HttpHandler {

    private final WebApplicationState state;

    public StatisticsHandler(WebApplicationState state) {
        this.state = Objects.requireNonNull(state, "state is required");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed. Use GET.");
            return;
        }

        try {
            StatisticsDto dto = StatisticsDto.from(state.getStatistics());
            HttpUtils.sendSuccess(exchange, dto);
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Error generating statistics: " + e.getMessage());
        }
    }
}

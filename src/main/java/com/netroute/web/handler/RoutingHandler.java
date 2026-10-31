package com.netroute.web.handler;

import com.netroute.model.Route;
import com.netroute.web.HttpUtils;
import com.netroute.web.WebApplicationState;
import com.netroute.web.dto.Requests;
import com.netroute.web.dto.RouteDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class RoutingHandler implements HttpHandler {

    private final WebApplicationState state;

    public RoutingHandler(WebApplicationState state) {
        this.state = Objects.requireNonNull(state, "state is required");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed. Use POST.");
            return;
        }

        try {
            Requests.CalculateRouteRequest req = HttpUtils.readJson(exchange, Requests.CalculateRouteRequest.class);
            if (req == null || req.sourceId() == null || req.destinationId() == null) {
                HttpUtils.sendError(exchange, 400, "sourceId and destinationId are required");
                return;
            }

            String src = req.sourceId().trim();
            String dst = req.destinationId().trim();

            if (!state.getNetwork().containsNode(src)) {
                HttpUtils.sendError(exchange, 400, "Source node does not exist: " + src);
                return;
            }
            if (!state.getNetwork().containsNode(dst)) {
                HttpUtils.sendError(exchange, 400, "Destination node does not exist: " + dst);
                return;
            }

            Optional<Route> routeOpt = state.getRouter().findRoute(state.getNetwork(), src, dst);
            if (routeOpt.isPresent()) {
                Route route = routeOpt.get();
                state.logEvent("Calculated shortest route from " + src + " to " + dst + " (cost: " + route.getTotalCost() + "): " + String.join(" -> ", route.getNodeIds()));
                HttpUtils.sendSuccess(exchange, RouteDto.from(route));
            } else {
                state.logEvent("No reachable route found from " + src + " to " + dst + ".");
                HttpUtils.sendSuccess(exchange, RouteDto.notFound());
            }
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Routing error: " + e.getMessage());
        }
    }
}

package com.netroute.web;

import com.netroute.web.handler.HistoryHandler;
import com.netroute.web.handler.NetworkHandler;
import com.netroute.web.handler.RoutingHandler;
import com.netroute.web.handler.SimulationHandler;
import com.netroute.web.handler.StatisticsHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Lightweight, modular HTTP server hosting the NetRoute Web application.
 * Powered by JDK 21 built-in {@link HttpServer} and virtual threads.
 */
public class WebServer {

    public static final int DEFAULT_PORT = 8080;

    private final int port;
    private final WebApplicationState state;
    private HttpServer server;

    public WebServer() {
        this(DEFAULT_PORT, new WebApplicationState());
    }

    public WebServer(int port) {
        this(port, new WebApplicationState());
    }

    public WebServer(int port, WebApplicationState state) {
        this.port = port;
        this.state = state != null ? state : new WebApplicationState();
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        // Leverage Java 21 Virtual Threads for high-concurrency request execution
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        // Attach REST API handlers
        server.createContext("/api/network", new NetworkHandler(state));
        server.createContext("/api/route", new RoutingHandler(state));
        server.createContext("/api/simulation", new SimulationHandler(state));
        server.createContext("/api/statistics", new StatisticsHandler(state));
        server.createContext("/api/history", new HistoryHandler(state));
        server.createContext("/api/db", new HistoryHandler(state));

        // Attach static frontend resource handler as root
        server.createContext("/", new StaticFileHandler());

        server.start();
    }

    public synchronized void stop(int delaySeconds) {
        if (server != null) {
            server.stop(Math.max(0, delaySeconds));
            server = null;
        }
    }

    public int getPort() {
        return server != null ? server.getAddress().getPort() : port;
    }

    public WebApplicationState getState() {
        return state;
    }
}

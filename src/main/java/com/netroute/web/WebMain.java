package com.netroute.web;

import com.netroute.repository.DatabaseConnection;

/**
 * Entry point for NetRoute Web application.
 * Launches the embedded HTTP server and serves the web interface.
 */
public final class WebMain {

    private WebMain() {}

    public static void main(String[] args) {
        int port = WebServer.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        } else {
            String envPort = System.getenv("PORT");
            if (envPort != null && !envPort.isBlank()) {
                try {
                    port = Integer.parseInt(envPort.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        try {
            WebApplicationState state = new WebApplicationState();
            WebServer server = new WebServer(port, state);
            server.start();

            int actualPort = server.getPort();
            String localUrl = "http://localhost:" + actualPort;

            System.out.println("================================================================================");
            System.out.println("   _  _     _   ___            _           __      __    _     ");
            System.out.println("  | \\| |___| |_| _ \\___ _  _ _| |_ ___   __\\ \\    / /___| |__  ");
            System.out.println("  | .` / -_)  _|   / _ \\ || |_   _/ -_) |___\\ \\/\\/ / -_) '_ \\ ");
            System.out.println("  |_|\\_\\___|\\__|_|_\\___/\\_,_| |_| \\___|      \\_/\\_/\\___|_.__/ ");
            System.out.println("================================================================================");
            System.out.println(" Network Traffic & Routing Simulator - Web Edition");
            System.out.println(" Tagline: Every Node Has a Path. Every Packet Has a Purpose.");
            System.out.println(" Architecture: Java 21 Virtual Threads + JDK HttpServer + HTML5 Canvas");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(" Application running at : " + localUrl);
            System.out.println(" API Base URL           : " + localUrl + "/api");

            // Diagnostic database check
            DatabaseConnection.ConnectionResult dbTest = state.getStorageService().testConnection();
            if (dbTest.isSuccess()) {
                System.out.println(" Database Connection   : CONNECTED (" + dbTest.getDatabaseProductName() + " " + dbTest.getDatabaseProductVersion() + " in " + dbTest.getResponseTimeMs() + "ms)");
            } else {
                System.out.println(" Database Connection   : OFFLINE (" + dbTest.getMessage() + ")");
                System.out.println(" Note: Local in-memory simulation is fully operational. To persist topologies, start MySQL.");
            }
            System.out.println("================================================================================");
            System.out.println(" Press Ctrl+C to stop the server.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down NetRoute Web Server...");
                server.stop(1);
                System.out.println("NetRoute Web stopped gracefully.");
            }));

        } catch (Exception e) {
            System.err.println("Failed to start NetRoute Web Server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

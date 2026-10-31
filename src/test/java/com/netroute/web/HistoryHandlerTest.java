package com.netroute.web;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netroute.repository.SimulationRunRecord;
import com.netroute.repository.TopologySummary;
import com.netroute.service.TopologyStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryHandlerTest {

    private WebServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new WebServer(0);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + server.getPort();
    }

    @Test
    void testDatabaseStatusDiagnostic() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/db/status"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());

        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertTrue(json.get("success").getAsBoolean());
        JsonObject data = json.getAsJsonObject("data");
        assertTrue(data.has("available"));
        assertTrue(data.has("message"));
    }

    @Test
    void testSaveTopologyValidationRejectsEmptyName() throws Exception {
        String invalidPayload = """
                {
                    "name": "   ",
                    "description": "empty name"
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/history/topologies/save"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidPayload))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Topology name cannot be empty"));
    }

    @Test
    void testListTopologiesWithLocalDateTimeSerializesCorrectly() throws Exception {
        TopologyStorageService mockService = new TopologyStorageService() {
            @Override
            public List<TopologySummary> listAllTopologies(boolean includeArchived) {
                return List.of(new TopologySummary(
                        10, "Test Topo", "Desc", 4, 3, false,
                        LocalDateTime.of(2026, 10, 31, 12, 0, 0),
                        LocalDateTime.of(2026, 10, 31, 12, 30, 0)
                ));
            }
        };

        WebApplicationState state = new WebApplicationState(mockService);
        WebServer customServer = new WebServer(0, state);
        customServer.start();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + customServer.getPort() + "/api/history/topologies"))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resp.statusCode());
            assertTrue(resp.body().contains("\"createdAt\": \"2026-10-31T12:00:00\""));
            assertTrue(resp.body().contains("\"updatedAt\": \"2026-10-31T12:30:00\""));
        } finally {
            customServer.stop(0);
        }
    }

    @Test
    void testListRunsWithLocalDateTimeSerializesCorrectly() throws Exception {
        TopologyStorageService mockService = new TopologyStorageService() {
            @Override
            public List<SimulationRunRecord> listAllSimulationRuns() {
                return List.of(new SimulationRunRecord(
                        99, 10, "Test Topo",
                        LocalDateTime.of(2026, 10, 31, 13, 15, 0),
                        500L, 20, 18, 1, 1, 90.0, 4.2, 0, 1, "Notes"
                ));
            }
        };

        WebApplicationState state = new WebApplicationState(mockService);
        WebServer customServer = new WebServer(0, state);
        customServer.start();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + customServer.getPort() + "/api/history/runs"))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resp.statusCode());
            assertTrue(resp.body().contains("\"runTimestamp\": \"2026-10-31T13:15:00\""));
        } finally {
            customServer.stop(0);
        }
    }
}

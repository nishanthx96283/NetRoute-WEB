package com.netroute.web;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
}

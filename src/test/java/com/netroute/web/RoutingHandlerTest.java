package com.netroute.web;

import com.google.gson.JsonArray;
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

class RoutingHandlerTest {

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
    void testCalculateOptimalRouteDijkstra() throws Exception {
        String payload = """
                {
                    "sourceId": "PC1",
                    "destinationId": "S1"
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/route"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());

        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertTrue(json.get("success").getAsBoolean());

        JsonObject data = json.getAsJsonObject("data");
        assertTrue(data.get("found").getAsBoolean());
        assertEquals(5.0, data.get("totalCost").getAsDouble(), 0.001);

        JsonArray nodeIds = data.getAsJsonArray("nodeIds");
        assertEquals(4, nodeIds.size());
        assertEquals("PC1", nodeIds.get(0).getAsString());
        assertEquals("R1", nodeIds.get(1).getAsString());
        assertEquals("R2", nodeIds.get(2).getAsString());
        assertEquals("S1", nodeIds.get(3).getAsString());
    }

    @Test
    void testRoutingWithInvalidNodesFailsGracefully() throws Exception {
        String payload = """
                {
                    "sourceId": "NONEXISTENT",
                    "destinationId": "S1"
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/route"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Source node does not exist"));
    }
}

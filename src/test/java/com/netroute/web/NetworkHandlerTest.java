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

class NetworkHandlerTest {

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
    void testGetNetworkReturnsInitialSampleNetwork() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/network"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        assertTrue(json.get("success").getAsBoolean());

        JsonObject data = json.getAsJsonObject("data");
        assertEquals(6, data.getAsJsonArray("nodes").size());
        assertEquals(6, data.getAsJsonArray("links").size());
    }

    @Test
    void testAddNodeAndPositionUpdate() throws Exception {
        String addNodePayload = """
                {
                    "type": "ROUTER",
                    "name": "Branch Router",
                    "x": 350.0,
                    "y": 250.0
                }
                """;

        HttpRequest addReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/network/node"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(addNodePayload))
                .build();

        HttpResponse<String> addResp = client.send(addReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, addResp.statusCode());

        JsonObject addJson = JsonParser.parseString(addResp.body()).getAsJsonObject();
        JsonObject nodeData = addJson.getAsJsonObject("data");
        String newNodeId = nodeData.get("id").getAsString();
        assertEquals("ROUTER", nodeData.get("type").getAsString());

        // Update position
        String posPayload = """
                { "x": 400.0, "y": 300.0 }
                """;
        HttpRequest posReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/network/node/position?id=" + newNodeId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(posPayload))
                .build();

        HttpResponse<String> posResp = client.send(posReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, posResp.statusCode());
    }

    @Test
    void testConnectAndDeleteNodes() throws Exception {
        // Connect PC1 and PC3 directly
        String connPayload = """
                {
                    "sourceId": "PC1",
                    "destinationId": "PC3",
                    "cost": 4.5,
                    "delay": 3.0,
                    "capacity": 5
                }
                """;

        HttpRequest connReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/network/link"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(connPayload))
                .build();

        HttpResponse<String> connResp = client.send(connReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, connResp.statusCode());

        JsonObject connJson = JsonParser.parseString(connResp.body()).getAsJsonObject();
        String linkId = connJson.getAsJsonObject("data").get("id").getAsString();

        // Delete the new link
        HttpRequest delLinkReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/network/link?id=" + linkId))
                .DELETE()
                .build();

        HttpResponse<String> delLinkResp = client.send(delLinkReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, delLinkResp.statusCode());

        // Delete PC3
        HttpRequest delNodeReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/network/node?id=PC3"))
                .DELETE()
                .build();

        HttpResponse<String> delNodeResp = client.send(delNodeReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, delNodeResp.statusCode());
    }

    @Test
    void testNewProjectAndReset() throws Exception {
        // New project
        HttpRequest newReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/network/new-project"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> newResp = client.send(newReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, newResp.statusCode());
        JsonObject newJson = JsonParser.parseString(newResp.body()).getAsJsonObject();
        assertEquals(0, newJson.getAsJsonObject("data").getAsJsonArray("nodes").size());

        // Reset
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/network/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resetResp = client.send(resetReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resetResp.statusCode());
        JsonObject resetJson = JsonParser.parseString(resetResp.body()).getAsJsonObject();
        assertEquals(6, resetJson.getAsJsonObject("data").getAsJsonArray("nodes").size());
    }
}

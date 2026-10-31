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

class StatisticsHandlerTest {

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
    void testStatisticsInitialValuesAndFailureSemantics() throws Exception {
        // Initial state
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/statistics"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());

        JsonObject data = JsonParser.parseString(resp.body()).getAsJsonObject().getAsJsonObject("data");
        assertEquals(6, data.get("totalNodes").getAsInt());
        assertEquals(6, data.get("totalLinks").getAsInt());
        assertEquals(6, data.get("activeLinks").getAsInt());
        assertEquals(0, data.get("inactiveLinks").getAsInt());
        assertEquals(0, data.get("currentlyFailedLinks").getAsInt());

        // 1. Fail two links (L1 and L2)
        postFailLink("L1");
        postFailLink("L2");

        JsonObject stats1 = getStats();
        assertEquals(2, stats1.get("currentlyFailedLinks").getAsInt());
        assertEquals(2, stats1.get("inactiveLinks").getAsInt());
        assertEquals(4, stats1.get("activeLinks").getAsInt());

        // 2. Restore one link (L1)
        postRestoreLink("L1");
        JsonObject stats2 = getStats();
        assertEquals(1, stats2.get("currentlyFailedLinks").getAsInt());
        assertEquals(1, stats2.get("inactiveLinks").getAsInt());
        assertEquals(5, stats2.get("activeLinks").getAsInt());

        // 3. Fail another link (L3) -> count becomes 2
        postFailLink("L3");
        JsonObject stats3 = getStats();
        assertEquals(2, stats3.get("currentlyFailedLinks").getAsInt());

        // 4. Restore both failed links (L2 and L3) -> count becomes 0
        postRestoreLink("L2");
        postRestoreLink("L3");
        JsonObject stats4 = getStats();
        assertEquals(0, stats4.get("currentlyFailedLinks").getAsInt());
        assertEquals(0, stats4.get("inactiveLinks").getAsInt());
        assertEquals(6, stats4.get("activeLinks").getAsInt());
    }

    private void postFailLink(String linkId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/simulation/fail-link"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"linkId\":\"" + linkId + "\"}"))
                .build();
        assertEquals(200, client.send(req, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    private void postRestoreLink(String linkId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/simulation/restore-link"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"linkId\":\"" + linkId + "\"}"))
                .build();
        assertEquals(200, client.send(req, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    private JsonObject getStats() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/statistics"))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(resp.body()).getAsJsonObject().getAsJsonObject("data");
    }
}

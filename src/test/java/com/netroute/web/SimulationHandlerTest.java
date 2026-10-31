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

class SimulationHandlerTest {

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
    void testPacketSubmissionAndDeliveryToIdle() throws Exception {
        String submitPayload = """
                {
                    "sourceId": "PC1",
                    "destinationId": "S1",
                    "payloadSize": 64
                }
                """;

        HttpRequest submitReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/simulation/packet"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(submitPayload))
                .build();

        HttpResponse<String> submitResp = client.send(submitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, submitResp.statusCode());

        // Run simulation until idle
        HttpRequest runReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/simulation/run"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> runResp = client.send(runReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, runResp.statusCode());

        JsonObject json = JsonParser.parseString(runResp.body()).getAsJsonObject();
        JsonObject state = json.getAsJsonObject("data");
        assertTrue(state.get("isIdle").getAsBoolean());
        assertEquals(1, state.get("deliveredCount").getAsInt());
        assertEquals(0, state.get("droppedCount").getAsInt());
    }

    @Test
    void testLinkFailureAndRestoration() throws Exception {
        // Fail link L4
        String failPayload = """
                { "linkId": "L4" }
                """;
        HttpRequest failReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/simulation/fail-link"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(failPayload))
                .build();

        HttpResponse<String> failResp = client.send(failReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, failResp.statusCode());

        // Now calculate route from PC1 to S1 - L4 is failed, so route should bypass L4 and take L6 (R1 -> S1)
        String routePayload = """
                { "sourceId": "PC1", "destinationId": "S1" }
                """;
        HttpRequest routeReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/route"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(routePayload))
                .build();

        HttpResponse<String> routeResp = client.send(routeReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, routeResp.statusCode());
        JsonObject routeData = JsonParser.parseString(routeResp.body()).getAsJsonObject().getAsJsonObject("data");
        // Path should now be PC1 -> R1 -> S1 (cost: 2 + 5 = 7.0)
        assertEquals(7.0, routeData.get("totalCost").getAsDouble(), 0.001);

        // Restore link L4
        String restorePayload = """
                { "linkId": "L4" }
                """;
        HttpRequest restoreReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/simulation/restore-link"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(restorePayload))
                .build();

        HttpResponse<String> restoreResp = client.send(restoreReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, restoreResp.statusCode());

        // Recalculate route - L4 is active again, so cost should be 5.0 again
        HttpResponse<String> routeResp2 = client.send(routeReq, HttpResponse.BodyHandlers.ofString());
        JsonObject routeData2 = JsonParser.parseString(routeResp2.body()).getAsJsonObject().getAsJsonObject("data");
        assertEquals(5.0, routeData2.get("totalCost").getAsDouble(), 0.001);
    }
}

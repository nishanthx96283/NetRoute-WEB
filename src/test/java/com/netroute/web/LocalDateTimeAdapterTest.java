package com.netroute.web;

import com.google.gson.Gson;
import com.netroute.repository.SimulationRunRecord;
import com.netroute.repository.TopologySummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LocalDateTimeAdapterTest {

    private final Gson gson = HttpUtils.GSON;

    @Test
    void testDirectSerializationAndDeserialization() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 31, 15, 10, 30);
        String json = gson.toJson(now);

        assertEquals("\"2026-10-31T15:10:30\"", json);

        LocalDateTime parsed = gson.fromJson(json, LocalDateTime.class);
        assertEquals(now, parsed);
    }

    @Test
    void testNullHandling() {
        String json = gson.toJson(null, LocalDateTime.class);
        assertEquals("null", json);

        LocalDateTime parsed = gson.fromJson("null", LocalDateTime.class);
        assertNull(parsed);
    }

    @Test
    void testDeserializationWithSpaceFormat() {
        String json = "\"2026-10-31 15:10:30\"";
        LocalDateTime parsed = gson.fromJson(json, LocalDateTime.class);

        assertEquals(LocalDateTime.of(2026, 10, 31, 15, 10, 30), parsed);
    }

    @Test
    void testTopologySummarySerializationDoesNotFailModuleAccess() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 31, 14, 0, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 10, 31, 14, 30, 0);

        TopologySummary summary = new TopologySummary(
                1,
                "Campus Network",
                "Description test",
                6,
                6,
                false,
                created,
                updated
        );

        // This would throw InaccessibleObjectException: Failed making field 'java.time.LocalDateTime#date' accessible without the adapter
        String json = assertDoesNotThrow(() -> gson.toJson(summary));

        assertTrue(json.contains("\"createdAt\": \"2026-10-31T14:00:00\""));
        assertTrue(json.contains("\"updatedAt\": \"2026-10-31T14:30:00\""));

        TopologySummary deserialized = assertDoesNotThrow(() -> gson.fromJson(json, TopologySummary.class));
        assertNotNull(deserialized);
        assertEquals("Campus Network", deserialized.getName());
        assertEquals(created, deserialized.getCreatedAt());
        assertEquals(updated, deserialized.getUpdatedAt());
    }

    @Test
    void testSimulationRunRecordSerializationDoesNotFailModuleAccess() {
        LocalDateTime runTime = LocalDateTime.of(2026, 10, 31, 14, 45, 12);

        SimulationRunRecord record = new SimulationRunRecord(
                42,
                1,
                "Campus Network",
                runTime,
                150L,
                10,
                8,
                1,
                1,
                80.0,
                3.5,
                1,
                2,
                "Link L4 failure test"
        );

        // Verify serialization works cleanly without reflection errors into java.time.LocalDateTime
        String json = assertDoesNotThrow(() -> gson.toJson(record));

        assertTrue(json.contains("\"runTimestamp\": \"2026-10-31T14:45:12\""));

        SimulationRunRecord deserialized = assertDoesNotThrow(() -> gson.fromJson(json, SimulationRunRecord.class));
        assertNotNull(deserialized);
        assertEquals(42, deserialized.getId());
        assertEquals("Campus Network", deserialized.getTopologyName());
        assertEquals(runTime, deserialized.getRunTimestamp());
        assertEquals(80.0, deserialized.getDeliveryRatio(), 0.001);
    }
}

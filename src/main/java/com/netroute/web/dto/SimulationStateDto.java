package com.netroute.web.dto;

import java.util.List;
import java.util.Map;

public record SimulationStateDto(
        long tick,
        boolean isIdle,
        List<PacketDto> packets,
        Map<String, Integer> queueSizes,
        int inFlightCount,
        int sentCount,
        int deliveredCount,
        int failedCount,
        int droppedCount,
        int linkFailureCount,
        List<String> recentEvents
) {}

package com.netroute.web.dto;

import com.netroute.model.Packet;

import java.util.List;

public record PacketDto(
        String id,
        String sourceId,
        String destinationId,
        int payloadSize,
        long createdAt,
        String status,
        String currentNodeId,
        Long sentAt,
        Long deliveredAt,
        String reason,
        int rerouteCount,
        List<String> visitedNodeIds,
        int hopIndex,
        List<String> routeNodeIds,
        String nextLinkId
) {
    public static PacketDto from(Packet packet) {
        List<String> routeNodeIds = packet.getRoute().map(com.netroute.model.Route::getNodeIds).orElse(List.of());
        String nextLinkId = packet.nextLink().map(com.netroute.model.NetworkLink::getId).orElse(null);

        return new PacketDto(
                packet.getId(),
                packet.getSourceId(),
                packet.getDestinationId(),
                packet.getPayloadSize(),
                packet.getCreatedAt(),
                packet.getStatus().name(),
                packet.getCurrentNodeId(),
                packet.getSentAt().orElse(null),
                packet.getDeliveredAt().orElse(null),
                packet.getReason().orElse(null),
                packet.getRerouteCount(),
                packet.getVisitedNodeIds(),
                packet.getHopIndex(),
                routeNodeIds,
                nextLinkId
        );
    }
}

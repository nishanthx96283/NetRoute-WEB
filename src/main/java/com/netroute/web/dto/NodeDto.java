package com.netroute.web.dto;

import com.netroute.model.NetworkNode;

public record NodeDto(
        String id,
        String name,
        String type,
        double x,
        double y
) {
    public static NodeDto from(NetworkNode node) {
        return new NodeDto(
                node.getId(),
                node.getName(),
                node.getType().name(),
                node.getX(),
                node.getY()
        );
    }
}

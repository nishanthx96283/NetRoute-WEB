package com.netroute.web.dto;

import com.netroute.model.NetworkLink;

public record LinkDto(
        String id,
        String sourceId,
        String destinationId,
        double cost,
        double delay,
        int capacity,
        boolean directed,
        boolean active
) {
    public static LinkDto from(NetworkLink link) {
        return new LinkDto(
                link.getId(),
                link.getSourceId(),
                link.getDestinationId(),
                link.getCost(),
                link.getDelay(),
                link.getCapacity(),
                link.isDirected(),
                link.isActive()
        );
    }
}

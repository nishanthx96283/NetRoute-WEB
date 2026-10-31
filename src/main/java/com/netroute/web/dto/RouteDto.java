package com.netroute.web.dto;

import com.netroute.model.Route;

import java.util.List;

public record RouteDto(
        boolean found,
        List<NodeDto> nodes,
        List<String> nodeIds,
        List<LinkDto> links,
        List<String> linkIds,
        double totalCost
) {
    public static RouteDto from(Route route) {
        if (route == null) {
            return new RouteDto(false, List.of(), List.of(), List.of(), List.of(), 0.0);
        }
        List<NodeDto> nodes = route.getNodes().stream().map(NodeDto::from).toList();
        List<LinkDto> links = route.getLinks().stream().map(LinkDto::from).toList();
        return new RouteDto(
                true,
                nodes,
                route.getNodeIds(),
                links,
                route.getLinkIds(),
                route.getTotalCost()
        );
    }

    public static RouteDto notFound() {
        return new RouteDto(false, List.of(), List.of(), List.of(), List.of(), 0.0);
    }
}

package com.netroute.web.dto;

public final class Requests {

    private Requests() {}

    public record CreateNodeRequest(
            String type,
            String name,
            Double x,
            Double y
    ) {}

    public record ConnectNodesRequest(
            String sourceId,
            String destinationId,
            double cost,
            Double delay,
            Integer capacity,
            Boolean directed
    ) {}

    public record UpdatePositionRequest(
            double x,
            double y
    ) {}

    public record CalculateRouteRequest(
            String sourceId,
            String destinationId
    ) {}

    public record SubmitPacketRequest(
            String sourceId,
            String destinationId,
            Integer payloadSize
    ) {}

    public record LinkActionRequest(
            String linkId
    ) {}

    public record SaveTopologyRequest(
            Integer topologyId,
            String name,
            String description,
            Boolean overwrite,
            Boolean recordRun,
            String runNotes
    ) {}

    public record LoadTopologyRequest(
            int topologyId
    ) {}
}

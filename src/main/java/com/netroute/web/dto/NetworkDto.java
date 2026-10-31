package com.netroute.web.dto;

import java.util.List;

public record NetworkDto(
        List<NodeDto> nodes,
        List<LinkDto> links,
        Integer activeTopologyId,
        String activeTopologyName,
        String activeTopologyDescription,
        boolean topologyModified,
        boolean currentRunSaved,
        Integer lastSavedRunId
) {}

package com.netroute.web.dto;

import com.netroute.statistics.NetworkStatistics;

public record StatisticsDto(
        int totalPackets,
        int deliveredCount,
        int failedCount,
        int droppedCount,
        double deliveryRatio,
        double averageDeliveryDelay,
        int totalNodes,
        int totalLinks,
        int activeLinks,
        int inactiveLinks,
        int currentlyFailedLinks,
        int linkFailureCount,
        int totalReroutes,
        String summaryText
) {
    public static StatisticsDto from(NetworkStatistics stats) {
        return new StatisticsDto(
                stats.getTotalPackets(),
                stats.getDeliveredCount(),
                stats.getFailedCount(),
                stats.getDroppedCount(),
                stats.getDeliveryRatio(),
                stats.getAverageDeliveryDelay(),
                stats.getTotalNodes(),
                stats.getTotalLinks(),
                stats.getActiveLinks(),
                stats.getInactiveLinks(),
                stats.getCurrentlyFailedLinks(),
                stats.getLinkFailureCount(),
                stats.getTotalReroutes(),
                stats.toString()
        );
    }
}

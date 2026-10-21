package com.netroute.model;

/**
 * Lifecycle of a simulated packet. Terminal states are {@link #DELIVERED},
 * {@link #FAILED} and {@link #DROPPED}.
 */
public enum PacketStatus {
    CREATED,
    QUEUED,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    DROPPED
}

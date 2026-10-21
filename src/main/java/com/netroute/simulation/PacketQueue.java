package com.netroute.simulation;

import com.netroute.model.Packet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * FIFO waiting line for packets that have not yet started traversing a link.
 */
public final class PacketQueue {

    private final String linkId;
    private final ArrayDeque<Packet> waiting = new ArrayDeque<>();

    public PacketQueue(String linkId) {
        if (linkId == null || linkId.isBlank()) {
            throw new IllegalArgumentException("Link ID must be a non-blank string");
        }
        this.linkId = linkId.trim();
    }

    public String getLinkId() {
        return linkId;
    }

    public void enqueue(Packet packet) {
        Objects.requireNonNull(packet, "packet is required");
        waiting.addLast(packet);
    }

    public Packet poll() {
        return waiting.pollFirst();
    }

    public boolean remove(Packet packet) {
        return waiting.remove(packet);
    }

    public List<Packet> drain() {
        List<Packet> drained = new ArrayList<>(waiting);
        waiting.clear();
        return drained;
    }

    public List<Packet> snapshot() {
        return List.copyOf(waiting);
    }

    public int size() {
        return waiting.size();
    }

    public boolean isEmpty() {
        return waiting.isEmpty();
    }
}

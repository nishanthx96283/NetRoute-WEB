package com.netroute.model;

import java.util.Objects;

/**
 * A vertex in the NetRoute graph. Identity is the node ID.
 * Position is stored for later Swing topology drawing and is unused by graph logic.
 */
public abstract class NetworkNode {

    private final String id;
    private String name;
    private final NodeType type;
    private double x;
    private double y;

    protected NetworkNode(String id, String name, NodeType type, double x, double y) {
        this.id = requireId(id);
        this.name = requireName(name);
        this.type = Objects.requireNonNull(type, "type is required");
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireName(name);
    }

    public NodeType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NetworkNode other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return type + "(" + id + ", " + name + ")";
    }

    static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Node ID must be a non-blank string");
        }
        return id.trim();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Node name must be a non-blank string");
        }
        return name.trim();
    }
}

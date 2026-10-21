package com.netroute.model;

/**
 * Intermediate forwarding node.
 */
public final class Router extends NetworkNode {

    public Router(String id, String name, double x, double y) {
        super(id, name, NodeType.ROUTER, x, y);
    }
}

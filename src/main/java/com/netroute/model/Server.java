package com.netroute.model;

/**
 * Service endpoint in the simulated network.
 */
public final class Server extends NetworkNode {

    public Server(String id, String name, double x, double y) {
        super(id, name, NodeType.SERVER, x, y);
    }
}

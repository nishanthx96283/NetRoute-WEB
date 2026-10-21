package com.netroute.model;

/**
 * End-host computer. Routing treats all node types equally; the type is for modelling and display.
 */
public final class Computer extends NetworkNode {

    public Computer(String id, String name, double x, double y) {
        super(id, name, NodeType.COMPUTER, x, y);
    }
}

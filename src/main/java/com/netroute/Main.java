package com.netroute;

import com.netroute.ui.NetRouteFrame;

import javax.swing.SwingUtilities;

/**
 * Entry point for the NetRoute desktop application.
 * Launches the full Swing desktop interface.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowWindow);
    }

    static void createAndShowWindow() {
        NetRouteFrame frame = new NetRouteFrame();
        frame.setVisible(true);
    }
}

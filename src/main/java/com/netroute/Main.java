package com.netroute;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Entry point for the NetRoute desktop application.
 * This class currently launches a placeholder window so the Maven project
 * can compile and run. Graph, routing, simulation, and persistence are not
 * implemented in this skeleton.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowWindow);
    }

    static void createAndShowWindow() {
        JFrame frame = new JFrame("NetRoute");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JLabel title = new JLabel("NetRoute", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));

        JLabel status = new JLabel(
                "Skeleton build. Database not connected. In-memory mode only.",
                SwingConstants.CENTER);

        frame.add(title, BorderLayout.CENTER);
        frame.add(status, BorderLayout.SOUTH);
        frame.setSize(640, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

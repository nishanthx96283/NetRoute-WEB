package com.netroute.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Central styling constants and UI factory methods for the NetRoute dark theme.
 * Uses a refined deep slate and cyan color palette tailored for network engineering dashboards.
 */
public final class Theme {

    private Theme() {
    }

    // Core Color Palette
    public static final Color BG_MAIN = new Color(0x17, 0x21, 0x2B);          // #17212B Deep slate / navy
    public static final Color BG_PANEL = new Color(0x24, 0x33, 0x42);         // #243342 Blue-grey cards & panels
    public static final Color BG_ELEVATED = new Color(0x2D, 0x40, 0x52);      // #2D4052 Elevated surfaces / hovers
    public static final Color BG_INPUT = new Color(0x1A, 0x26, 0x33);         // #1A2633 Dark inputs & textfields

    // Accents
    public static final Color ACCENT_CYAN = new Color(0x43, 0xC6, 0xD9);      // #43C6D9 Primary refined cyan
    public static final Color ACCENT_CYAN_HOVER = new Color(0x56, 0xD5, 0xE7);// Lighter cyan on hover
    public static final Color ACCENT_BLUE = new Color(0x70, 0x97, 0xD3);      // #7097D3 Secondary muted blue
    public static final Color ACCENT_PURPLE = new Color(0x9E, 0x7A, 0xD2);    // Muted purple for servers

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(0xE8, 0xEE, 0xF4);     // #E8EEF4 Soft white
    public static final Color TEXT_SECONDARY = new Color(0xA7, 0xB5, 0xC4);   // #A7B5C4 Cool grey
    public static final Color TEXT_MUTED = new Color(0x71, 0x82, 0x94);       // Muted text
    public static final Color TEXT_DARK = new Color(0x10, 0x1A, 0x24);        // Dark text for bright cyan buttons

    // Status Colors
    public static final Color STATUS_SUCCESS = new Color(0x4E, 0xBA, 0x6F);   // Muted green
    public static final Color STATUS_WARNING = new Color(0xE5, 0xA9, 0x3C);   // Warm amber
    public static final Color STATUS_ERROR = new Color(0xE0, 0x5D, 0x5D);     // Restrained coral red
    public static final Color STATUS_ERROR_BG = new Color(0x3B, 0x22, 0x28);  // Subtle red background for destructive actions

    // Borders & Dividers
    public static final Color BORDER_SUBTLE = new Color(0x35, 0x49, 0x5E);    // #35495E
    public static final Color BORDER_FOCUS = new Color(0x43, 0xC6, 0xD9);     // Focus border

    // Typography
    public static final Font FONT_HERO = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_PAGE_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    public enum ButtonStyle {
        PRIMARY,    // Cyan accent background, dark bold text
        SECONDARY,  // Elevated surface, light text, subtle border
        SUCCESS,    // Greenish accent
        DESTRUCTIVE,// Coral red border/text
        OUTLINE     // Transparent with border
    }

    /**
     * Creates a styled JButton with custom colors and hover feedback.
     */
    public static JButton createButton(String text, ButtonStyle style) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getBackground();
                if (getModel().isPressed()) {
                    bg = bg.darker();
                } else if (getModel().isRollover()) {
                    bg = getHoverColor(style);
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Border
                Color borderColor = getBorderColor(style);
                if (borderColor != null) {
                    g2.setColor(borderColor);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        button.setFont(FONT_BODY_BOLD);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        applyButtonColors(button, style);
        return button;
    }

    private static void applyButtonColors(JButton button, ButtonStyle style) {
        switch (style) {
            case PRIMARY -> {
                button.setBackground(ACCENT_CYAN);
                button.setForeground(TEXT_DARK);
            }
            case SECONDARY -> {
                button.setBackground(BG_ELEVATED);
                button.setForeground(TEXT_PRIMARY);
            }
            case SUCCESS -> {
                button.setBackground(STATUS_SUCCESS);
                button.setForeground(TEXT_DARK);
            }
            case DESTRUCTIVE -> {
                button.setBackground(STATUS_ERROR_BG);
                button.setForeground(STATUS_ERROR);
            }
            case OUTLINE -> {
                button.setBackground(BG_PANEL);
                button.setForeground(TEXT_PRIMARY);
            }
        }
    }

    private static Color getHoverColor(ButtonStyle style) {
        return switch (style) {
            case PRIMARY -> ACCENT_CYAN_HOVER;
            case SECONDARY -> new Color(0x38, 0x4E, 0x63);
            case SUCCESS -> new Color(0x5E, 0xC8, 0x7E);
            case DESTRUCTIVE -> new Color(0x4C, 0x2A, 0x32);
            case OUTLINE -> BG_ELEVATED;
        };
    }

    private static Color getBorderColor(ButtonStyle style) {
        return switch (style) {
            case PRIMARY -> null;
            case SECONDARY -> BORDER_SUBTLE;
            case SUCCESS -> null;
            case DESTRUCTIVE -> STATUS_ERROR;
            case OUTLINE -> BORDER_SUBTLE;
        };
    }

    /**
     * Creates a card panel with standard background, border, and rounded feel.
     */
    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        return card;
    }

    /**
     * Styles a standard JTextField to match dark theme.
     */
    public static void styleTextField(JTextField textField) {
        textField.setBackground(BG_INPUT);
        textField.setForeground(TEXT_PRIMARY);
        textField.setCaretColor(ACCENT_CYAN);
        textField.setFont(FONT_BODY);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    /**
     * Styles a JComboBox for the dark theme.
     */
    public static <T> void styleComboBox(JComboBox<T> comboBox) {
        comboBox.setBackground(BG_INPUT);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setFont(FONT_BODY);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE, 1));
    }

    /**
     * Creates a section header label with accent color.
     */
    public static JLabel createSectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SUBTITLE);
        label.setForeground(ACCENT_CYAN);
        label.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        return label;
    }

    /**
     * Creates a standard border with title in the dark theme.
     */
    public static Border createTitledBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER_SUBTLE, 1, true),
                        title,
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        FONT_SUBTITLE,
                        ACCENT_CYAN
                ),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        );
    }
}

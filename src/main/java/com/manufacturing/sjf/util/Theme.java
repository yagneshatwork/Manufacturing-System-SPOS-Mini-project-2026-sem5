package com.manufacturing.sjf.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Centralised design-token registry for the Smart Manufacturing System.
 *
 * <p>All colours, fonts, and shared UI builders come from here so that
 * changing a single constant updates every panel consistently.</p>
 */
public final class Theme {

    // ─── Colour Palette ───────────────────────────────────────────────────────

    /** Midnight Navy — main application background. */
    public static final Color BG_MAIN       = hex("#111827");
    /** Dark navy — panel background. */
    public static final Color BG_PANEL      = hex("#1F2937");
    /** Medium navy — secondary surfaces, input fields. */
    public static final Color BG_SECONDARY  = hex("#374151");
    /** Slightly lighter surface for hover/selection. */
    public static final Color BG_HOVER      = hex("#4B5563");

    /** Industrial Teal — primary accent. */
    public static final Color ACCENT_TEAL   = hex("#14B8A6");
    /** Darker teal for pressed/active states. */
    public static final Color ACCENT_TEAL_D = hex("#0D9488");
    /** Light teal for tints. */
    public static final Color ACCENT_TEAL_L = hex("#5EEAD4");

    /** Safety Amber — selected / next task. */
    public static final Color AMBER         = hex("#F59E0B");
    /** Machine Green — completed tasks. */
    public static final Color GREEN         = hex("#22C55E");
    /** Alert Red — error / failed. */
    public static final Color RED           = hex("#F87171");
    /** Sky Blue — idle / informational. */
    public static final Color BLUE          = hex("#60A5FA");

    /** Primary text colour. */
    public static final Color TEXT_PRIMARY   = hex("#F3F4F6");
    /** Secondary / muted text. */
    public static final Color TEXT_SECONDARY = hex("#9CA3AF");
    /** Disabled text. */
    public static final Color TEXT_DISABLED  = hex("#6B7280");

    /** Subtle separator border colour. */
    public static final Color BORDER         = hex("#374151");
    /** Brighter border for focused/highlighted panels. */
    public static final Color BORDER_ACCENT  = hex("#14B8A6");

    // ─── Fonts ────────────────────────────────────────────────────────────────

    public static final Font FONT_TITLE     = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_SUBTITLE  = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_HEADING   = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_BODY      = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL     = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO      = new Font("Consolas",  Font.PLAIN, 12);
    public static final Font FONT_CARD_VAL  = new Font("Segoe UI", Font.BOLD,  24);
    public static final Font FONT_CARD_LBL  = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BTN       = new Font("Segoe UI", Font.BOLD,  12);

    // ─── Dimensions ───────────────────────────────────────────────────────────

    public static final int  CORNER_RADIUS  = 10;
    public static final int  CARD_PAD       = 12;

    // ─── Colour Utility ───────────────────────────────────────────────────────

    /** Converts a CSS hex string (#RRGGBB) to a {@link Color}. */
    public static Color hex(String hexCode) {
        String h = hexCode.startsWith("#") ? hexCode.substring(1) : hexCode;
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new Color(r, g, b);
    }

    /** Returns a colour with modified alpha. */
    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // ─── Component Factories ──────────────────────────────────────────────────

    /** Creates a themed, rounded panel background. */
    public static JPanel panel() {
        JPanel p = new JPanel();
        p.setBackground(BG_PANEL);
        p.setOpaque(true);
        return p;
    }

    /** Creates a labelled section panel with a teal-bordered title. */
    public static JPanel sectionPanel(String title) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG_PANEL);
        p.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)));
        if (title != null && !title.isBlank()) {
            JLabel lbl = new JLabel(title);
            lbl.setFont(FONT_HEADING);
            lbl.setForeground(ACCENT_TEAL);
            lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
            p.add(lbl, BorderLayout.NORTH);
        }
        return p;
    }

    /** Creates a styled primary button (teal background). */
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, ACCENT_TEAL, BG_MAIN, ACCENT_TEAL_D);
        return b;
    }

    /** Creates a styled secondary button (secondary bg). */
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, BG_SECONDARY, TEXT_PRIMARY, BG_HOVER);
        return b;
    }

    /** Creates a styled danger button (red background). */
    public static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, RED, BG_MAIN, hex("#EF4444"));
        return b;
    }

    /** Creates an amber-coloured button. */
    public static JButton warningButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, AMBER, BG_MAIN, hex("#D97706"));
        return b;
    }

    private static void styleButton(JButton b, Color bg, Color fg, Color hoverBg) {
        b.setFont(FONT_BTN);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(hoverBg); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(bg); }
        });
    }

    /** Creates a themed text field. */
    public static JTextField textField() {
        JTextField f = new JTextField();
        f.setBackground(BG_SECONDARY);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setFont(FONT_BODY);
        f.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(6, 10, 6, 10)));
        return f;
    }

    /** Creates a themed spinner. */
    public static JSpinner intSpinner(int value, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        s.setBackground(BG_SECONDARY);
        s.setForeground(TEXT_PRIMARY);
        s.setFont(FONT_BODY);
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(BG_SECONDARY);
            de.getTextField().setForeground(TEXT_PRIMARY);
            de.getTextField().setCaretColor(TEXT_PRIMARY);
            de.getTextField().setBorder(new EmptyBorder(6, 8, 6, 8));
        }
        s.setBorder(new LineBorder(BORDER, 1, true));
        return s;
    }

    /** Creates a themed form label. */
    public static JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_SECONDARY);
        l.setFont(FONT_SMALL);
        return l;
    }

    /** Sets up global Swing LAF defaults to dark theme colours. */
    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background",               BG_MAIN);
        UIManager.put("Table.background",               BG_PANEL);
        UIManager.put("Table.foreground",               TEXT_PRIMARY);
        UIManager.put("Table.gridColor",                BORDER);
        UIManager.put("Table.selectionBackground",      ACCENT_TEAL);
        UIManager.put("Table.selectionForeground",      BG_MAIN);
        UIManager.put("TableHeader.background",         BG_SECONDARY);
        UIManager.put("TableHeader.foreground",         TEXT_PRIMARY);
        UIManager.put("TableHeader.font",               FONT_BTN);
        UIManager.put("ScrollPane.background",          BG_PANEL);
        UIManager.put("ScrollBar.background",           BG_SECONDARY);
        UIManager.put("ScrollBar.thumb",                BORDER_ACCENT);
        UIManager.put("ScrollBar.thumbDarkShadow",      BG_MAIN);
        UIManager.put("ScrollBar.thumbShadow",          BG_SECONDARY);
        UIManager.put("ScrollBar.track",                BG_PANEL);
        UIManager.put("ScrollBar.width",                8);
        UIManager.put("ToolTip.background",             BG_SECONDARY);
        UIManager.put("ToolTip.foreground",             TEXT_PRIMARY);
        UIManager.put("ToolTip.border",                 new LineBorder(BORDER_ACCENT, 1));
        UIManager.put("OptionPane.background",          BG_PANEL);
        UIManager.put("OptionPane.messageForeground",   TEXT_PRIMARY);
        UIManager.put("Button.background",              BG_SECONDARY);
        UIManager.put("Button.foreground",              TEXT_PRIMARY);
        UIManager.put("Label.foreground",               TEXT_PRIMARY);
        UIManager.put("Label.font",                     FONT_BODY);
        UIManager.put("Separator.foreground",           BORDER);
        UIManager.put("Viewport.background",            BG_PANEL);
        UIManager.put("SplitPane.background",           BG_MAIN);
        UIManager.put("SplitPane.dividerSize",          5);
    }

    private Theme() {} // non-instantiable
}

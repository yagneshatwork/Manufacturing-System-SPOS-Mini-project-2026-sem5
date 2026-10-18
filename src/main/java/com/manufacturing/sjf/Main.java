package com.manufacturing.sjf;

import com.manufacturing.sjf.ui.MainFrame;
import com.manufacturing.sjf.util.Theme;

import javax.swing.*;

/**
 * Application entry point.
 *
 * <p>Sets the system Look and Feel, applies dark-theme global defaults,
 * then creates and shows the {@link MainFrame} on the Event Dispatch Thread.</p>
 */
public class Main {

    public static void main(String[] args) {
        // Apply system LAF (cross-platform dark adjustments applied in Theme)
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default LAF
        }

        // Apply global dark theme defaults
        Theme.applyGlobalDefaults();

        // Launch UI on EDT
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

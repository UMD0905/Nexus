package com.nexus.service;

import com.nexus.config.AppContext;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.function.Supplier;

/**
 * Manages the AWT system-tray icon for Nexus.
 *
 * <p>AWT SystemTray works alongside JavaFX — we launch it on the AWT event thread.
 * The tray icon:
 * <ul>
 *   <li>Shows unread notification count as a badge text (tooltip).</li>
 *   <li>Right-click menu: Quick Add, Show Nexus, Quit.</li>
 *   <li>Double-click: restores the window.</li>
 * </ul>
 *
 * <p>Minimise-to-tray is opt-in (setting {@code minimize_to_tray}, default {@code false}).
 */
public class SystemTrayService {

    private static final Logger log = LoggerFactory.getLogger(SystemTrayService.class);

    private final Stage           stage;
    private final AppContext      ctx;
    private final Supplier<Void>  quickAddAction;

    private TrayIcon  trayIcon;
    private boolean   installed = false;

    public SystemTrayService(Stage stage, AppContext ctx, Supplier<Void> quickAddAction) {
        this.stage          = stage;
        this.ctx            = ctx;
        this.quickAddAction = quickAddAction;
    }

    public void install() {
        if (!SystemTray.isSupported()) {
            log.warn("System tray not supported on this platform — skipping");
            return;
        }
        EventQueue.invokeLater(() -> {
            try {
                SystemTray tray = SystemTray.getSystemTray();

                // Load the icon — fall back to a generated 16×16 placeholder if resource missing
                Image img = loadIcon();

                PopupMenu menu = new PopupMenu();

                MenuItem quickAdd = new MenuItem("Quick Add");
                quickAdd.addActionListener((ActionEvent e) -> {
                    Platform.runLater(() -> quickAddAction.get());
                    showWindow();
                });

                MenuItem show = new MenuItem("Show Nexus");
                show.addActionListener((ActionEvent e) -> showWindow());

                MenuItem quit = new MenuItem("Quit");
                quit.addActionListener((ActionEvent e) -> {
                    Platform.runLater(() -> {
                        stage.close();
                        AppContext.getInstance().shutdown();
                        Platform.exit();
                    });
                    tray.remove(trayIcon);
                });

                menu.add(quickAdd);
                menu.addSeparator();
                menu.add(show);
                menu.addSeparator();
                menu.add(quit);

                trayIcon = new TrayIcon(img, "Nexus", menu);
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("Nexus — Productivity Hub");
                trayIcon.addActionListener((ActionEvent e) -> showWindow()); // double-click

                tray.add(trayIcon);
                installed = true;
                log.info("System tray icon installed");
            } catch (Exception e) {
                log.warn("Could not install system tray icon: {}", e.getMessage());
            }
        });
    }

    /**
     * Updates the tray icon tooltip to show unread notification count.
     * Call after any notification state change.
     */
    public void updateBadge(int unreadCount) {
        if (!installed || trayIcon == null) return;
        EventQueue.invokeLater(() -> {
            String tooltip = unreadCount > 0
                ? "Nexus — " + unreadCount + " unread notification" + (unreadCount == 1 ? "" : "s")
                : "Nexus — Productivity Hub";
            trayIcon.setToolTip(tooltip);
        });
    }

    /** Hide the window to tray if the minimize_to_tray setting is enabled. */
    public void handleMinimize() {
        if (!installed) return;
        String setting = ctx.getSettingsService().getString("minimize_to_tray", "false");
        if ("true".equalsIgnoreCase(setting)) {
            Platform.runLater(() -> stage.hide());
        }
    }

    public void remove() {
        if (!installed || trayIcon == null) return;
        EventQueue.invokeLater(() -> {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
                installed = false;
            } catch (Exception e) {
                log.warn("Could not remove tray icon: {}", e.getMessage());
            }
        });
    }

    private void showWindow() {
        Platform.runLater(() -> {
            stage.show();
            stage.setIconified(false);
            stage.toFront();
        });
    }

    private Image loadIcon() {
        try {
            URL url = getClass().getResource("/icons/nexus-tray.png");
            if (url != null) return Toolkit.getDefaultToolkit().getImage(url);
        } catch (Exception ignored) {}
        // Fallback: generate a simple coloured square
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x6366f1));
        g.fillRoundRect(0, 0, 16, 16, 4, 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("N", 3, 12);
        g.dispose();
        return img;
    }
}

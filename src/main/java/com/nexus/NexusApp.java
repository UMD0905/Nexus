package com.nexus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.config.AppContext;
import com.nexus.service.SystemTrayService;
import com.nexus.ui.MainWindow;
import com.nexus.ui.NexusBridge;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class NexusApp extends Application {

    private static final Logger log    = LoggerFactory.getLogger(NexusApp.class);
    private static final Path   STATE  = Path.of(System.getProperty("user.home"), ".nexus", "data", "window-state.json");
    /** Lock port — only one Nexus instance may hold this at a time. */
    private static final int    LOCK_PORT = 47291;

    private Stage             primaryStage;
    private ServerSocket      instanceLock;
    private SystemTrayService trayService;

    @Override
    public void start(Stage stage) {
        log.info("Starting Nexus UI...");

        // Single-instance check: if another instance is running, focus it and exit
        if (!acquireInstanceLock()) {
            log.warn("Another Nexus instance is already running — exiting");
            Platform.exit();
            return;
        }

        this.primaryStage = stage;

        AppContext ctx = AppContext.getInstance();
        NexusBridge bridge = new NexusBridge(ctx, stage);

        MainWindow mainWindow = new MainWindow(ctx, bridge);
        Scene scene = new Scene(mainWindow, 1280, 800);
        scene.setFill(Color.web("#090d18"));

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Nexus");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        restoreWindowState(stage);

        stage.show();

        // Install system tray
        // quickAddAction triggers the Quick Add modal — we push a bridge event
        trayService = new SystemTrayService(stage, ctx, () -> {
            bridge.pushEvent("QUICK_ADD_OPEN", null);
            return null;
        });
        trayService.install();

        // Minimize-to-tray hook
        stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (isIconified) trayService.handleMinimize();
        });

        log.info("Nexus started.");
    }

    @Override
    public void stop() {
        log.info("Nexus shutting down...");
        saveWindowState(primaryStage);
        if (trayService != null) trayService.remove();
        releaseInstanceLock();
        AppContext.getInstance().shutdown();
    }

    // ── Single-instance lock ──────────────────────────────────────────────────

    private boolean acquireInstanceLock() {
        try {
            instanceLock = new ServerSocket(LOCK_PORT);
            return true;
        } catch (IOException e) {
            return false;  // port already in use → another instance is running
        }
    }

    private void releaseInstanceLock() {
        if (instanceLock != null) {
            try { instanceLock.close(); } catch (IOException ignored) {}
        }
    }

    // ── Window state ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void restoreWindowState(Stage stage) {
        try {
            if (!Files.exists(STATE)) {
                stage.setMaximized(true);
                return;
            }
            Map<String, Object> s = new ObjectMapper().readValue(STATE.toFile(), Map.class);

            boolean maximized = Boolean.TRUE.equals(s.get("maximized"));

            // Restore normal bounds before maximizing so un-maximize snaps to the saved size
            double x = s.get("x") instanceof Number n ? n.doubleValue() : 100;
            double y = s.get("y") instanceof Number n ? n.doubleValue() : 100;
            double w = s.get("width")  instanceof Number n ? n.doubleValue() : 1280;
            double h = s.get("height") instanceof Number n ? n.doubleValue() : 800;

            // Clamp to screen bounds so the window never starts off-screen
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            w = Math.min(w, screen.getWidth());
            h = Math.min(h, screen.getHeight());
            x = Math.max(screen.getMinX(), Math.min(x, screen.getMaxX() - w));
            y = Math.max(screen.getMinY(), Math.min(y, screen.getMaxY() - h));

            stage.setX(x);
            stage.setY(y);
            stage.setWidth(w);
            stage.setHeight(h);
            stage.setMaximized(maximized);
        } catch (Exception e) {
            log.warn("Could not restore window state: {}", e.getMessage());
            stage.setMaximized(true);
        }
    }

    private void saveWindowState(Stage stage) {
        try {
            Files.createDirectories(STATE.getParent());
            Map<String, Object> s = Map.of(
                "x",         stage.isMaximized() ? stage.getX() : stage.getX(),
                "y",         stage.isMaximized() ? stage.getY() : stage.getY(),
                "width",     stage.isMaximized() ? stage.getWidth()  : stage.getWidth(),
                "height",    stage.isMaximized() ? stage.getHeight() : stage.getHeight(),
                "maximized", stage.isMaximized()
            );
            new ObjectMapper().writeValue(STATE.toFile(), s);
        } catch (Exception e) {
            log.warn("Could not save window state: {}", e.getMessage());
        }
    }
}

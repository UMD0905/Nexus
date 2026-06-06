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
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
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
    private static final int    LOCK_PORT = 47291;

    private Stage             primaryStage;
    private ServerSocket      instanceLock;
    private SystemTrayService trayService;

    private double savedX = 100, savedY = 100, savedW = 1280, savedH = 800;

    @Override
    public void start(Stage stage) {
        log.info("Starting Nexus UI...");

        if (!acquireInstanceLock()) {
            log.warn("Another Nexus instance is already running — exiting");
            Platform.exit();
            return;
        }

        this.primaryStage = stage;

        // ── 1. Create MainWindow immediately and use it as the scene root ──────
        // The WebView lives inside MainWindow. By making it the scene root before
        // stage.show() is called, the JavaFX renderer creates the GPU surface for
        // WebKit in the same pass as the initial window paint — avoiding the
        // black-screen artefact that occurs when the WebView is added to an
        // already-showing scene (which resets the D3D swap chain).
        MainWindow mainWindow = new MainWindow();

        Scene scene = new Scene(mainWindow, 1280, 800);
        scene.setFill(Color.web("#090d18"));

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Nexus");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        restoreWindowState(stage);
        stage.show();
        stage.toFront();
        stage.requestFocus();

        // ── 2. Wire hotkeys and window listeners (no bridge needed yet) ────────
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!event.isControlDown()) return;
            // Bridge may not be ready yet; mainWindow.onAppReady sets it up.
        });

        stage.maximizedProperty().addListener((obs, wasMax, isMax) -> {
            if (!isMax) {
                // Window just un-maximized: capture the restored geometry so
                // saveWindowState() can persist the correct non-maximized size/position
                // on exit.  Saving while maximized would store the full-screen dimensions
                // which restores to wrong bounds after the next launch unmaximize.
                savedX = stage.getX();
                savedY = stage.getY();
                savedW = stage.getWidth();
                savedH = stage.getHeight();
            }
        });

        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) Platform.runLater(() -> {
                mainWindow.focusWebView();
                mainWindow.repaintWebView();
            });
        });

        stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (isIconified) {
                if (trayService != null) trayService.handleMinimize();
            } else {
                Platform.runLater(mainWindow::repaintWebView);
            }
        });

        // ── 3. Initialise AppContext off the FX thread ────────────────────────
        Thread initThread = new Thread(() -> {
            try {
                AppContext ctx = AppContext.getInstance();

                Platform.runLater(() -> {
                    try {
                        NexusBridge bridge = new NexusBridge(ctx, stage);

                        // Hotkeys now that bridge is available.
                        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                            if (!event.isControlDown()) return;
                            switch (event.getCode()) {
                                case N -> { bridge.pushEvent("QUICK_ADD_OPEN", null); event.consume(); }
                                case K -> { bridge.pushEvent("SEARCH_OPEN",     null); event.consume(); }
                                case D -> { bridge.pushEvent("MARK_DONE",       null); event.consume(); }
                                default -> { }
                            }
                        });

                        // System tray
                        trayService = new SystemTrayService(stage, ctx, () -> {
                            bridge.pushEvent("QUICK_ADD_OPEN", null);
                            return null;
                        });
                        trayService.install();

                        // Hand off to MainWindow — injects bridge once page is loaded.
                        mainWindow.onAppReady(ctx, bridge);
                        log.info("Nexus started.");

                    } catch (Exception e) {
                        log.error("Failed to initialise app context on FX thread", e);
                    }
                });

            } catch (Exception e) {
                log.error("AppContext initialisation failed", e);
                Platform.runLater(() -> {
                    Label err = new Label("Failed to initialise: " + e.getMessage());
                    err.setStyle("-fx-text-fill: #f87171; -fx-font-size: 14px;");
                    mainWindow.getChildren().setAll(err);
                });
            }
        }, "nexus-init");
        initThread.setDaemon(true);
        initThread.start();
    }

    @Override
    public void stop() {
        log.info("Nexus shutting down...");
        if (primaryStage != null) saveWindowState(primaryStage);
        if (trayService != null) trayService.remove();
        releaseInstanceLock();
        try { AppContext.getInstance().shutdown(); } catch (Exception ignored) {}
    }

    // ── Single-instance lock ──────────────────────────────────────────────────

    private boolean acquireInstanceLock() {
        try {
            instanceLock = new ServerSocket(LOCK_PORT);
            return true;
        } catch (IOException e) {
            return false;
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

            double x = s.get("x")      instanceof Number n ? n.doubleValue() : 100;
            double y = s.get("y")      instanceof Number n ? n.doubleValue() : 100;
            double w = s.get("width")  instanceof Number n ? n.doubleValue() : 1280;
            double h = s.get("height") instanceof Number n ? n.doubleValue() : 800;

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
            double x = stage.isMaximized() ? savedX : stage.getX();
            double y = stage.isMaximized() ? savedY : stage.getY();
            double w = stage.isMaximized() ? savedW : stage.getWidth();
            double h = stage.isMaximized() ? savedH : stage.getHeight();
            Map<String, Object> s = Map.of(
                "x", x, "y", y, "width", w, "height", h,
                "maximized", stage.isMaximized()
            );
            new ObjectMapper().writeValue(STATE.toFile(), s);
        } catch (Exception e) {
            log.warn("Could not save window state: {}", e.getMessage());
        }
    }
}

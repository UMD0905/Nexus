package com.nexus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.config.AppContext;
import com.nexus.service.SystemTrayService;
import com.nexus.ui.MainWindow;
import com.nexus.ui.NexusBridge;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

        // ── 1. Show the window immediately with a loading splash ───────────────
        // This prevents the black-window gap that occurs when AppContext (DB init,
        // Flyway migrations, recurrence generation) blocks the FX thread before
        // stage.show() is ever called.
        StackPane splash = buildSplash();
        Scene scene = new Scene(splash, 1280, 800);
        scene.setFill(Color.web("#090d18"));

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Nexus");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        restoreWindowState(stage);
        stage.show();

        // ── 2. Initialise AppContext off the FX thread ────────────────────────
        // DB creation, Flyway migrations, recurrence generation, and service
        // startup are all pure Java / JOOQ — safe to run on a background thread.
        Thread initThread = new Thread(() -> {
            try {
                AppContext ctx = AppContext.getInstance();

                // ── 3. Back on FX thread: build the real UI and swap it in ────
                Platform.runLater(() -> {
                    try {
                        NexusBridge bridge = new NexusBridge(ctx, stage);
                        MainWindow mainWindow = new MainWindow(ctx, bridge);

                        // Replace the loading splash with the real root node.
                        scene.setRoot(mainWindow);

                        // Wire hotkeys at the scene level (most reliable intercept point).
                        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                            if (!event.isControlDown()) return;
                            switch (event.getCode()) {
                                case N -> { bridge.pushEvent("QUICK_ADD_OPEN", null); event.consume(); }
                                case K -> { bridge.pushEvent("SEARCH_OPEN",     null); event.consume(); }
                                case D -> { bridge.pushEvent("MARK_DONE",       null); event.consume(); }
                                default -> { }
                            }
                        });

                        // Capture pre-maximized bounds for correct window-state save.
                        stage.maximizedProperty().addListener((obs, wasMax, isMax) -> {
                            if (isMax) {
                                savedX = stage.getX();
                                savedY = stage.getY();
                                savedW = stage.getWidth();
                                savedH = stage.getHeight();
                            }
                        });

                        // Restore WebView focus when the window comes back to the foreground.
                        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                            if (isFocused) Platform.runLater(mainWindow::focusWebView);
                        });

                        // System tray
                        trayService = new SystemTrayService(stage, ctx, () -> {
                            bridge.pushEvent("QUICK_ADD_OPEN", null);
                            return null;
                        });
                        trayService.install();

                        stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
                            if (isIconified) trayService.handleMinimize();
                        });

                        Platform.runLater(mainWindow::focusWebView);
                        log.info("Nexus started.");

                    } catch (Exception e) {
                        log.error("Failed to build main window", e);
                    }
                });

            } catch (Exception e) {
                log.error("AppContext initialisation failed", e);
                Platform.runLater(() -> {
                    Label err = new Label("Failed to initialise: " + e.getMessage());
                    err.setStyle("-fx-text-fill: #f87171; -fx-font-size: 14px;");
                    ((StackPane) scene.getRoot()).getChildren().setAll(err);
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

    // ── Loading splash ────────────────────────────────────────────────────────

    private StackPane buildSplash() {
        Label title = new Label("Nexus");
        title.setStyle("""
            -fx-text-fill: #6366f1;
            -fx-font-size: 32px;
            -fx-font-weight: bold;
            """);

        Label sub = new Label("Starting…");
        sub.setStyle("""
            -fx-text-fill: #475569;
            -fx-font-size: 13px;
            """);

        VBox box = new VBox(10, title, sub);
        box.setAlignment(Pos.CENTER);

        StackPane pane = new StackPane(box);
        pane.setBackground(new Background(new BackgroundFill(Color.web("#090d18"), null, null)));
        return pane;
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

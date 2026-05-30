package com.nexus.ui.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Bridge methods for window controls and notifications.
 */
public class WindowBridge {

    private static final Logger log = LoggerFactory.getLogger(WindowBridge.class);

    private final AppContext ctx;
    private final Stage stage;
    private final ObjectMapper json;

    // Drag state: captured on mousedown, applied on mousemove
    private double dragStartScreenX, dragStartScreenY;
    private double dragStartStageX,  dragStartStageY;

    public WindowBridge(AppContext ctx, Stage stage) {
        this.ctx   = ctx;
        this.stage = stage;
        this.json  = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── Window controls ───────────────────────────────────────────────────────

    /**
     * Called on mousedown over the drag region.
     * Captures both the cursor's screen position and the window's current position.
     */
    public void startDrag(double screenX, double screenY) {
        if (stage.isMaximized()) return;  // can't drag a maximized window
        this.dragStartScreenX = screenX;
        this.dragStartScreenY = screenY;
        this.dragStartStageX  = stage.getX();
        this.dragStartStageY  = stage.getY();
    }

    /**
     * Called on mousemove while dragging.
     * Moves the window by the delta from the captured start position.
     */
    public void dragWindow(double screenX, double screenY) {
        if (stage.isMaximized()) return;
        double newX = dragStartStageX + (screenX - dragStartScreenX);
        double newY = dragStartStageY + (screenY - dragStartScreenY);
        Platform.runLater(() -> {
            stage.setX(newX);
            stage.setY(newY);
        });
    }

    /** Toggle maximize from the title bar double-click. */
    public void toggleMaximize() {
        Platform.runLater(() -> stage.setMaximized(!stage.isMaximized()));
    }

    public void minimizeWindow() {
        Platform.runLater(() -> stage.setIconified(true));
    }

    public void maximizeWindow() {
        Platform.runLater(() -> stage.setMaximized(!stage.isMaximized()));
    }

    public void closeWindow() {
        Platform.runLater(() -> {
            stage.close();
            AppContext.getInstance().shutdown();
            Platform.exit();
        });
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public String getNotifications() {
        try {
            return toJson(ctx.getNotificationService().getAll().stream()
                .map(BridgeDtos::notifDto).toList());
        } catch (Exception e) { return error(e); }
    }

    public void markNotificationRead(int id) {
        try { ctx.getNotificationService().markRead(id); }
        catch (Exception e) { log.error("markNotificationRead failed", e); }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    public String getSettings() {
        try { return toJson(ctx.getSettingsService().getAll()); }
        catch (Exception e) { return error(e); }
    }

    public void setSetting(String key, String value) {
        try { ctx.getSettingsService().set(key, value); }
        catch (Exception e) { log.error("setSetting {}={} failed", key, value, e); }
    }

    // ── Native file/folder pickers ────────────────────────────────────────────

    public String chooseFolder(String title) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle(title);
            java.io.File dir = chooser.showDialog(stage);
            future.complete(dir != null ? dir.getAbsolutePath() : "");
        });
        try {
            String result = future.get(30, TimeUnit.SECONDS);
            return result.isEmpty() ? "null" : "\"" + result.replace("\\", "\\\\") + "\"";
        } catch (Exception e) {
            log.warn("chooseFolder timed out or was cancelled");
            return "null";
        }
    }

    public String chooseFile(String title, String ext) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle(title);
            if (ext != null && !ext.isBlank()) {
                chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter(
                        ext.toUpperCase() + " files", "*." + ext));
            }
            java.io.File file = chooser.showOpenDialog(stage);
            future.complete(file != null ? file.getAbsolutePath() : "");
        });
        try {
            String result = future.get(30, TimeUnit.SECONDS);
            return result.isEmpty() ? "null" : "\"" + result.replace("\\", "\\\\") + "\"";
        } catch (Exception e) {
            log.warn("chooseFile timed out or was cancelled");
            return "null";
        }
    }

    // ── Category ordering ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void reorderCategories(String orderedIdsJson) {
        try {
            List<Number> ids = new ObjectMapper().readValue(orderedIdsJson, List.class);
            for (int i = 0; i < ids.size(); i++) {
                long catId = ids.get(i).longValue();
                ctx.getDsl().execute(
                    "UPDATE CATEGORIES SET POSITION = ? WHERE ID = ?", i + 1, catId);
            }
        } catch (Exception e) { log.error("reorderCategories failed", e); }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"serialisation failed\"}"; }
    }

    private String error(Exception e) {
        log.error("Bridge error: {}", e.getMessage(), e);
        return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
    }
}

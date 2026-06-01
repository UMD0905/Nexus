package com.nexus.ui.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
            // Batch all UPDATEs inside one transaction — N round-trips → 1
            ctx.getDsl().transaction(cfg -> {
                var txDsl = org.jooq.impl.DSL.using(cfg);
                for (int i = 0; i < ids.size(); i++) {
                    long catId = ids.get(i).longValue();
                    txDsl.execute("UPDATE CATEGORIES SET POSITION = ? WHERE ID = ?", i + 1, catId);
                }
            });
        } catch (Exception e) { log.error("reorderCategories failed", e); }
    }

    // ── About / Diagnostics ───────────────────────────────────────────────────

    public String getAppInfo() {
        try {
            // App version from build-time resource
            Properties appProps = new Properties();
            try (InputStream is = getClass().getResourceAsStream("/app.properties")) {
                if (is != null) appProps.load(is);
            }
            String version = appProps.getProperty("app.version", "dev");

            // DB file stats
            Path dbFile = Path.of(System.getProperty("user.home"), ".nexus", "data", "nexus.mv.db");
            long dbBytes = Files.exists(dbFile) ? Files.size(dbFile) : 0L;
            String dbSize = dbBytes >= 1_048_576
                ? String.format("%.1f MB", dbBytes / 1_048_576.0)
                : String.format("%.1f KB", dbBytes / 1024.0);

            // Entity counts
            int taskCount     = ctx.getDsl().selectCount().from("TASKS").fetchOne(0, Integer.class);
            int goalCount     = ctx.getDsl().selectCount().from("GOALS").fetchOne(0, Integer.class);
            int categoryCount = ctx.getDsl().selectCount().from("CATEGORIES").fetchOne(0, Integer.class);

            // Latest Flyway schema version
            String schemaVersion = "unknown";
            try {
                schemaVersion = ctx.getDsl()
                    .select(DSL.field("version", String.class))
                    .from("flyway_schema_history")
                    .orderBy(DSL.field("installed_rank", Integer.class).desc())
                    .limit(1)
                    .fetchOne(0, String.class);
            } catch (Exception ignored) { /* table absent in some test contexts */ }

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("version",       version);
            info.put("java",          System.getProperty("java.version"));
            info.put("os",            System.getProperty("os.name") + " " + System.getProperty("os.version"));
            info.put("dbPath",        dbFile.toString());
            info.put("dbSize",        dbSize);
            info.put("taskCount",     taskCount);
            info.put("goalCount",     goalCount);
            info.put("categoryCount", categoryCount);
            info.put("schemaVersion", schemaVersion);
            return toJson(info);
        } catch (Exception e) { return error(e); }
    }

    public String exportDiagnostics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            String ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(now);
            Path zipPath = Path.of(System.getProperty("user.home"), "Downloads",
                "nexus-diagnostics-" + ts + ".zip");
            Files.createDirectories(zipPath.getParent());

            Path logDir = Path.of(System.getProperty("user.home"), ".nexus", "logs");
            LocalDate cutoff = now.toLocalDate().minusDays(7);

            try (ZipOutputStream zos = new ZipOutputStream(
                    Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {

                // 1. Log files modified in the last 7 days
                if (Files.isDirectory(logDir)) {
                    try (Stream<Path> listing = Files.list(logDir)) {
                        List<Path> recentLogs = listing
                            .filter(p -> p.toString().endsWith(".log"))
                            .filter(p -> {
                                try {
                                    return !Files.getLastModifiedTime(p).toInstant()
                                        .isBefore(cutoff.atStartOfDay(ZoneId.systemDefault()).toInstant());
                                } catch (Exception ex) { return true; }
                            })
                            .toList();
                        for (Path lf : recentLogs) {
                            zos.putNextEntry(new ZipEntry("logs/" + lf.getFileName()));
                            Files.copy(lf, zos);
                            zos.closeEntry();
                        }
                    }
                }

                // 2. Settings keys only — values are redacted for privacy
                Map<String, String> allSettings = ctx.getSettingsService().getAll();
                StringBuilder keysOnly = new StringBuilder("# Settings keys (values redacted)\n");
                allSettings.keySet().stream().sorted().forEach(k -> keysOnly.append(k).append('\n'));
                zos.putNextEntry(new ZipEntry("settings-keys.txt"));
                zos.write(keysOnly.toString().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                // 3. Current schema version
                String schemaVersion = "unknown";
                try {
                    schemaVersion = ctx.getDsl()
                        .select(DSL.field("version", String.class))
                        .from("flyway_schema_history")
                        .orderBy(DSL.field("installed_rank", Integer.class).desc())
                        .limit(1)
                        .fetchOne(0, String.class);
                } catch (Exception ignored) {}
                zos.putNextEntry(new ZipEntry("schema-version.txt"));
                zos.write(schemaVersion.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            return "\"" + zipPath.toString().replace("\\", "\\\\") + "\"";
        } catch (Exception e) { return error(e); }
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

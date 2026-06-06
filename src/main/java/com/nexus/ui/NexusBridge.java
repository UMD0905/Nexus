package com.nexus.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import com.nexus.ui.bridge.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Top-level JS bridge — injected as {@code window.nexusBridge} by the WebView.
 *
 * <p>Actual method implementations live in the five focused sub-bridge classes
 * under {@code com.nexus.ui.bridge}:
 * <ul>
 *   <li>{@code nexusBridge.tasks}    — task CRUD, subtasks, tags, recurrence, snooze</li>
 *   <li>{@code nexusBridge.goals}    — goal CRUD and status</li>
 *   <li>{@code nexusBridge.dashboard} — stats, import/export</li>
 *   <li>{@code nexusBridge.planning}  — time blocks, categories, tags, Pomodoro</li>
 *   <li>{@code nexusBridge.window}    — window controls, notifications, settings, file pickers</li>
 * </ul>
 *
 * <p>From JavaScript, call e.g.
 * {@code window.nexusBridge.tasks.getTasks("{}") }.
 */
public class NexusBridge {

    private static final Logger log = LoggerFactory.getLogger(NexusBridge.class);

    // ── Sub-bridges (accessible as nexusBridge.tasks, nexusBridge.goals, …) ──

    public final TaskBridge      tasks;
    public final GoalBridge      goals;
    public final DashboardBridge dashboard;
    public final PlanningBridge  planning;
    public final WindowBridge    win;      // NOT "window" — that name is shadowed by the DOM global
    public final ProjectBridge   projects;
    public final FinanceBridge   finance;

    /** JS window handle — kept alive by the WebView engine. */
    private JSObject jsWindow;

    private final ObjectMapper json;

    public NexusBridge(AppContext ctx, Stage stage) {
        this.json      = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.tasks     = new TaskBridge(ctx);
        this.goals     = new GoalBridge(ctx);
        this.dashboard = new DashboardBridge(ctx);
        this.planning  = new PlanningBridge(ctx);
        this.win       = new WindowBridge(ctx, stage);
        this.projects  = new ProjectBridge(ctx);
        this.finance   = new FinanceBridge(ctx);
    }

    /** Called once after the page loads to give us a handle to push events to JS. */
    public void init(JSObject jsWindow) {
        this.jsWindow = jsWindow;
        log.info("NexusBridge initialised — JS window handle acquired");
    }

    // ── Window control proxies ────────────────────────────────────────────────
    // These are duplicated here so JavaScript can call them directly on the
    // top-level nexusBridge object without needing to traverse a sub-bridge
    // field (JSObject field access is unreliable in JavaFX WebKit).

    public void minimizeWindow()                        { win.minimizeWindow(); }
    public void maximizeWindow()                        { win.maximizeWindow(); }
    public void toggleMaximize()                        { win.toggleMaximize(); }
    public void closeWindow()                           { win.closeWindow();    }
    public void startDrag(double screenX, double screenY) { win.startDrag(screenX, screenY); }
    public void dragWindow(double screenX, double screenY) { win.dragWindow(screenX, screenY); }

    // ── Push event to JS ──────────────────────────────────────────────────────

    /** Fires {@code window.onBridgeEvent(eventJson)} in the WebView. */
    public void pushEvent(String type, Object payload) {
        if (jsWindow == null) return;
        try {
            Map<String, Object> evt = new HashMap<>();
            evt.put("type", type);
            evt.put("payload", payload);
            String eventJson = json.writeValueAsString(evt);
            Platform.runLater(() -> {
                try { jsWindow.call("onBridgeEvent", eventJson); }
                catch (Exception e) { log.warn("pushEvent failed: {}", e.getMessage()); }
            });
        } catch (Exception e) { log.warn("pushEvent serialisation failed", e); }
    }
}

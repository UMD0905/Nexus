package com.nexus.ui;

import com.nexus.config.AppContext;
import javafx.animation.AnimationTimer;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Root window — hosts the React/TypeScript UI in a JavaFX WebView.
 *
 * <p>The WebView is created immediately when this node is constructed so it
 * is part of the live scene graph from the very first frame. This gives the
 * JavaFX D3D/OpenGL renderer a chance to create a proper GPU surface for
 * WebKit before any content is painted, preventing the black-screen artefact
 * that occurs when the WebView is added to an already-showing scene.
 *
 * <p>Bridge injection is driven by a polling AnimationTimer started when
 * AppContext is ready, NOT by the WebView load state.  JavaFX WebKit fires a
 * second RUNNING→never-SUCCEEDED cycle when processing ES-module scripts,
 * which makes the Worker.State SUCCEEDED signal unreliable as a "page ready"
 * gate.  Polling `executeScript("window")` directly is the reliable approach.
 */
public class MainWindow extends StackPane {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private WebView     webView;
    private VBox        splash;
    private boolean     appReady   = false;
    private boolean     bridgeDone = false;   // true after first successful injection
    private AppContext  ctx;
    private NexusBridge bridge;

    public MainWindow() {
        build();
    }

    /** Call this to give the embedded browser keyboard focus. */
    public void focusWebView() {
        if (webView != null) webView.requestFocus();
    }

    /**
     * Forces WebKit to repaint its surface. Call this when the window is
     * restored from minimized or regains focus.
     */
    public void repaintWebView() {
        if (webView == null || !bridgeDone) return;
        try {
            webView.getEngine().executeScript(
                "(function(){" +
                "  var d=document.body;" +
                "  if(!d)return;" +
                "  d.style.display='none';" +
                "  d.offsetHeight;" +
                "  d.style.display='';" +
                "})();"
            );
        } catch (Exception ignored) {}
    }

    /**
     * Called once AppContext and NexusBridge are fully initialised.
     * Starts the bridge injection retry loop.
     * Must be called on the JavaFX Application Thread.
     */
    public void onAppReady(AppContext ctx, NexusBridge bridge) {
        this.ctx    = ctx;
        this.bridge = bridge;
        this.appReady = true;
        log.info("onAppReady: starting bridge injection loop");
        // Try immediately, then schedule a retry loop.
        tryInjectBridge();
        scheduleBridgeRetry();
    }

    private void build() {
        setBackground(new Background(new BackgroundFill(Color.web("#090d18"), null, null)));

        // ── Loading splash ────────────────────────────────────────────────────
        Label titleLabel = new Label("Nexus");
        titleLabel.setStyle(
            "-fx-text-fill: #6366f1;" +
            "-fx-font-size: 32px;" +
            "-fx-font-weight: bold;"
        );
        Label subLabel = new Label("Loading…");
        subLabel.setStyle(
            "-fx-text-fill: #64748b;" +
            "-fx-font-size: 13px;"
        );
        splash = new VBox(10, titleLabel, subLabel);
        splash.setAlignment(Pos.CENTER);
        splash.setMouseTransparent(true);

        // ── WebView ────────────────────────────────────────────────────────────
        webView = new WebView();
        WebEngine engine = webView.getEngine();

        webView.setStyle("-fx-background-color: #090d18;");
        webView.setPageFill(Color.web("#090d18"));
        engine.setJavaScriptEnabled(true);

        engine.setConfirmHandler(message -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm");
            alert.setHeaderText(null);
            alert.setContentText(message);
            return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
        });

        engine.setPromptHandler(promptData -> {
            TextInputDialog dialog = new TextInputDialog(
                promptData.getDefaultValue() != null ? promptData.getDefaultValue() : "");
            dialog.setTitle("Input");
            dialog.setHeaderText(null);
            dialog.setContentText(promptData.getMessage());
            return dialog.showAndWait().orElse(null);
        });

        // Track page reload — reset bridgeDone so bridge re-injects after reload.
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            log.debug("WebView state: {} -> {}", oldState, newState);
            if (newState == Worker.State.RUNNING) {
                // New page load started — allow re-injection after reload.
                boolean wasInjected = bridgeDone;
                bridgeDone = false;
                // If appReady was already set (reload after startup), start a fresh retry loop.
                if (wasInjected && appReady) {
                    log.info("Page reloading — starting bridge re-injection loop");
                    scheduleBridgeRetry();
                }
            } else if (newState == Worker.State.FAILED) {
                log.error("WebView load FAILED: {}", engine.getLoadWorker().getException());
            }
            // NOTE: We do NOT rely on SUCCEEDED to inject the bridge because JavaFX WebKit
            // fires a phantom RUNNING state (processing ES-module scripts) that never
            // reaches SUCCEEDED, leaving the bridge permanently un-injected.
            // Bridge injection is driven exclusively by the AnimationTimer retry loop.
        });

        // WebView behind, splash on top.
        getChildren().addAll(webView, splash);
        webView.setOnMouseClicked(e -> webView.requestFocus());

        // Start loading immediately.
        URL url = prepareWebUI();
        if (url != null) {
            log.info("Loading WebUI from: {}", url);
            engine.load(url.toExternalForm());
        } else {
            log.error("webui/index.html not found — run 'npm run build' first");
            engine.loadContent(
                "<html><body style=\"background:#090d18;color:#94a3b8;font-family:sans-serif;" +
                "  display:flex;align-items:center;justify-content:center;height:100vh;margin:0\">" +
                "  <div style=\"text-align:center\">" +
                "    <h2 style=\"color:#6366f1\">Nexus</h2>" +
                "    <p>UI not found. Run <code>npm run build</code> inside" +
                "       <code>src/main/webui/</code> then restart.</p>" +
                "  </div></body></html>");
        }
    }

    /**
     * Attempts to inject the Java bridge objects into the live JS context.
     * Returns true if injection succeeded, false if the page isn't ready yet
     * (caller should retry).  Must be called on the FX Application Thread.
     */
    private boolean tryInjectBridge() {
        if (!appReady || bridgeDone) return bridgeDone;

        WebEngine engine = webView.getEngine();

        // Ask WebKit for the current window object.  Returns null or throws when
        // the JS context doesn't exist yet (page still loading) — both are safe.
        JSObject win;
        try {
            win = (JSObject) engine.executeScript("window");
        } catch (Exception e) {
            log.debug("tryInjectBridge: JS context not ready ({})", e.getMessage());
            return false;
        }

        if (win == null) {
            log.debug("tryInjectBridge: window is null — page not ready yet");
            return false;
        }

        // Verify document is truly interactive before injecting.
        try {
            Object readyState = engine.executeScript("document.readyState");
            if (!"complete".equals(readyState) && !"interactive".equals(readyState)) {
                log.debug("tryInjectBridge: document.readyState={} — waiting", readyState);
                return false;
            }
        } catch (Exception e) {
            log.debug("tryInjectBridge: readyState check failed ({})", e.getMessage());
            return false;
        }

        boolean firstInjection = !bridgeDone;
        bridgeDone = true;

        // ── 1. Inject bridge sub-objects ──────────────────────────────────────
        try {
            win.setMember("nexusBridge",         bridge);
            win.setMember("nexusBridgeTasks",     bridge.tasks);
            win.setMember("nexusBridgeGoals",     bridge.goals);
            win.setMember("nexusBridgeDashboard", bridge.dashboard);
            win.setMember("nexusBridgePlanning",  bridge.planning);
            win.setMember("nexusBridgeWin",       bridge.win);
            win.setMember("nexusBridgeProjects",  bridge.projects);
            win.setMember("nexusBridgeFinance",   bridge.finance);
            bridge.init(win);
            log.info("Bridge injected (first={})", firstInjection);
        } catch (Exception e) {
            log.error("tryInjectBridge: setMember failed — will retry", e);
            bridgeDone = false;   // allow retry
            return false;
        }

        // ── 2. Wire notification callback ─────────────────────────────────────
        try {
            ctx.getReminderService().setOnNotificationCreated(
                () -> bridge.pushEvent("NOTIFICATION", null));
        } catch (Exception e) {
            log.error("tryInjectBridge: setOnNotificationCreated failed", e);
        }

        // ── 3. Fire DATA_READY after React's useEffect has registered onBridgeEvent ──
        try {
            engine.executeScript(
                "(function(){" +
                "  var ev='{\"type\":\"DATA_READY\",\"payload\":null}';" +
                "  var fired=false;" +
                "  function fire(){" +
                "    if(fired)return;" +
                "    if(typeof window.onBridgeEvent==='function'){" +
                "      fired=true;" +
                "      window.onBridgeEvent(ev);" +
                "    } else {" +
                "      setTimeout(fire,100);" +
                "    }" +
                "  }" +
                "  setTimeout(fire,0);" +
                "})();"
            );
        } catch (Exception e) {
            log.error("tryInjectBridge: DATA_READY script failed", e);
        }

        // ── 4. Remove splash (no-op if already removed) ───────────────────────
        getChildren().remove(splash);
        webView.requestFocus();

        return true;
    }

    /**
     * AnimationTimer that retries bridge injection every 200 ms until it
     * succeeds (or 15 seconds elapse, at which point we force-remove the splash
     * and give up — the React polling effect will still detect the bridge if it
     * ever becomes available later).
     */
    private void scheduleBridgeRetry() {
        final int[] attempts = {0};
        AnimationTimer timer = new AnimationTimer() {
            private long lastFire = 0;
            @Override public void handle(long now) {
                if (now - lastFire < 200_000_000L) return; // 200 ms between attempts
                lastFire = now;
                attempts[0]++;

                if (bridgeDone) {
                    stop();
                    return;
                }

                boolean ok = tryInjectBridge();
                if (ok) {
                    stop();
                    return;
                }

                if (attempts[0] >= 75) { // ~15 seconds
                    log.error("Bridge injection failed after 15 s — forcing splash removal");
                    getChildren().remove(splash);
                    webView.requestFocus();
                    stop();
                }
            }
        };
        timer.start();
    }

    private URL prepareWebUI() {
        URL indexUrl = getClass().getResource("/webui/index.html");
        if (indexUrl == null) return null;

        if ("file".equals(indexUrl.getProtocol())) {
            return indexUrl;
        }

        try {
            Path tempDir = Files.createTempDirectory("nexus-webui-");
            log.info("Extracting WebUI to temp dir: {}", tempDir);

            URLConnection conn = indexUrl.openConnection();
            if (conn instanceof JarURLConnection jarConn) {
                try (JarFile jar = jarConn.getJarFile()) {
                    jar.stream()
                       .filter(e -> e.getName().startsWith("webui/") && !e.isDirectory())
                       .forEach(e -> extractEntry(jar, e, tempDir));
                }
            }

            return tempDir.resolve("index.html").toUri().toURL();
        } catch (Exception e) {
            log.error("Failed to extract WebUI to temp dir", e);
            return indexUrl;
        }
    }

    private void extractEntry(JarFile jar, JarEntry entry, Path targetDir) {
        try {
            String relative = entry.getName().substring("webui/".length());
            Path target = targetDir.resolve(relative);
            Files.createDirectories(target.getParent());
            try (InputStream is = jar.getInputStream(entry)) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.warn("Failed to extract {}", entry.getName(), e);
        }
    }
}

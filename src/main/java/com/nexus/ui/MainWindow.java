package com.nexus.ui;

import com.nexus.config.AppContext;
import javafx.application.Platform;
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
 * <p>When packaged as an app-image the webui resources live inside the JAR.
 * JavaFX WebKit cannot load subresources (JS/CSS) from {@code jar:} URLs, so
 * we extract the entire {@code /webui/} tree to a temp directory on first run
 * and load from there via a plain {@code file://} URL instead.
 *
 * <p>A branded loading screen covers the window while the WebView is
 * initialising so the user never sees a black or blank window.
 */
public class MainWindow extends StackPane {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private final AppContext  ctx;
    private final NexusBridge bridge;
    private WebView webView;

    public MainWindow(AppContext ctx, NexusBridge bridge) {
        this.ctx    = ctx;
        this.bridge = bridge;
        build();
    }

    /** Call this to give the embedded browser keyboard focus. */
    public void focusWebView() {
        if (webView != null) webView.requestFocus();
    }

    private void build() {
        // Dark canvas — visible during the loading phase so there is never a
        // black flash before the React app paints.
        setBackground(new Background(new BackgroundFill(Color.web("#090d18"), null, null)));

        // ── Loading splash ─────────────────────────────────────────────────────
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
        VBox splash = new VBox(10, titleLabel, subLabel);
        splash.setAlignment(Pos.CENTER);
        splash.setMouseTransparent(true);

        // ── WebView ────────────────────────────────────────────────────────────
        webView = new WebView();
        WebEngine engine = webView.getEngine();

        // Start the WebView hidden — revealed only when the page finishes loading.
        // Using setVisible(false) rather than setOpacity(0) is more reliable in
        // the packaged build because it fully removes the WebKit surface from
        // painting, avoiding any black-flash artefacts.
        webView.setVisible(false);
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

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("nexusBridge", bridge);
                bridge.init(window);

                ctx.getReminderService().setOnNotificationCreated(() ->
                    bridge.pushEvent("NOTIFICATION", null));

                log.info("React app loaded — bridge injected");

                // Swap: show the WebView, remove the loading splash.
                Platform.runLater(() -> {
                    getChildren().remove(splash);
                    webView.setVisible(true);
                    webView.requestFocus();
                });
            } else if (newState == Worker.State.FAILED) {
                log.error("WebView failed to load: {}", engine.getLoadWorker().getException());
            }
        });

        URL url = prepareWebUI();
        if (url != null) {
            engine.load(url.toExternalForm());
            log.info("Loading WebUI from: {}", url);
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

        // WebView behind, splash on top — splash is removed once the page loads.
        getChildren().addAll(webView, splash);

        webView.setOnMouseClicked(e -> webView.requestFocus());
    }

    /**
     * Returns the URL to load in the WebView.
     *
     * <p>Dev mode (classpath = filesystem): load directly.
     * Packaged mode (classpath = JAR): extract the whole {@code /webui/}
     * tree to a temp dir so WebKit can resolve JS/CSS via {@code file://}.
     */
    private URL prepareWebUI() {
        URL indexUrl = getClass().getResource("/webui/index.html");
        if (indexUrl == null) return null;

        if ("file".equals(indexUrl.getProtocol())) {
            return indexUrl; // development — filesystem path works fine
        }

        // Packaged inside a JAR — extract to temp dir
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
            return indexUrl; // last-resort fallback
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

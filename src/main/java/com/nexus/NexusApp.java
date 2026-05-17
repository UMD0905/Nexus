package com.nexus;

import atlantafx.base.theme.PrimerDark;
import com.nexus.config.AppContext;
import com.nexus.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX {@link Application} entry point.
 *
 * <p>Deliberately kept thin: wires the AppContext and the root scene,
 * nothing else.  Business logic stays in the service layer.
 */
public class NexusApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(NexusApp.class);

    @Override
    public void start(Stage stage) {
        log.info("Starting Nexus UI...");

        // Apply AtlantaFX dark theme (PrimerDark) before any nodes are created
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Bootstrap the DI container (creates pool, runs migrations, wires services)
        AppContext ctx = AppContext.getInstance();

        // Root window
        MainWindow mainWindow = new MainWindow(ctx);
        Scene scene = new Scene(mainWindow, 1280, 780);

        stage.setTitle("Nexus");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        log.info("Nexus started.");
    }

    @Override
    public void stop() {
        log.info("Nexus shutting down...");
        AppContext.getInstance().shutdown();
    }
}

package com.nexus;

import com.nexus.config.AppContext;
import com.nexus.ui.MainWindow;
import com.nexus.ui.NexusBridge;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(NexusApp.class);

    @Override
    public void start(Stage stage) {
        log.info("Starting Nexus UI...");

        AppContext ctx = AppContext.getInstance();

        // Bridge needs stage reference so JS can minimize/close
        NexusBridge bridge = new NexusBridge(ctx, stage);

        MainWindow mainWindow = new MainWindow(ctx, bridge);
        Scene scene = new Scene(mainWindow, 1920, 1200);
        scene.setFill(Color.web("#090d18"));

        // Remove native title bar — no white line, clean full-screen look
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Nexus");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        log.info("Nexus started.");
    }

    @Override
    public void stop() {
        log.info("Nexus shutting down...");
        AppContext.getInstance().shutdown();
    }
}

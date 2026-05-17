package com.nexus.ui.views;

import atlantafx.base.theme.Styles;
import com.nexus.model.PomodoroSession;
import com.nexus.model.Task;
import com.nexus.viewmodel.PomodoroViewModel;
import com.nexus.viewmodel.PomodoroViewModel.TimerState;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

import java.time.format.DateTimeFormatter;

/**
 * Pomodoro timer view.
 *
 * <p>Features a circular progress ring drawn on a Canvas,
 * a countdown label, task selector, Start / Pause / Stop controls,
 * and a session history list for today.
 */
public class PomodoroView extends BorderPane {

    private static final int    RING_SIZE = 220;
    private static final double RING_WIDTH = 16;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final PomodoroViewModel vm;

    private Canvas ringCanvas;
    private Label  countdownLabel;
    private Label  statusLabel;
    private Button startBtn;
    private Button pauseBtn;
    private Button stopBtn;

    public PomodoroView(PomodoroViewModel vm) {
        this.vm = vm;
        build();
        vm.initialize();

        // Redraw ring whenever remaining seconds change
        vm.remainingSecondsProperty().addListener((obs, old, nv) -> drawRing());
        vm.timerStateProperty().addListener((obs, old, nv) -> {
            drawRing();
            updateButtonState(nv);
        });
        drawRing();
        updateButtonState(vm.timerStateProperty().get());
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("pomodoro-view");
        setPadding(new Insets(24));

        // Title
        Label title = new Label("Pomodoro Timer");
        title.getStyleClass().add("view-title");
        title.setPadding(new Insets(0, 0, 16, 0));

        // Timer ring + countdown
        VBox timerBox = buildTimerBox();

        // Task selector + settings
        VBox controlBox = buildControlBox();

        // Session history
        VBox historyBox = buildHistoryBox();

        // Center layout: timer ring | controls + history
        HBox content = new HBox(32, timerBox, new VBox(16, controlBox, historyBox));
        content.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(timerBox, Priority.NEVER);
        HBox.setHgrow(content.getChildren().get(1), Priority.ALWAYS);

        VBox main = new VBox(0, title, content);
        main.setAlignment(Pos.TOP_LEFT);
        setCenter(main);
    }

    // ── Timer ring ────────────────────────────────────────────────────────────

    private VBox buildTimerBox() {
        ringCanvas = new Canvas(RING_SIZE, RING_SIZE);

        countdownLabel = new Label(vm.getFormattedTime());
        countdownLabel.getStyleClass().add("pomodoro-countdown");
        countdownLabel.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; " +
                                "-fx-text-fill: -color-fg-default;");

        vm.remainingSecondsProperty().addListener((obs, old, nv) ->
            countdownLabel.setText(vm.getFormattedTime()));

        statusLabel = new Label();
        statusLabel.getStyleClass().add("task-card-meta");
        statusLabel.textProperty().bind(vm.statusMessageProperty());
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(RING_SIZE - 20);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setTextAlignment(TextAlignment.CENTER);

        // Session counter
        Label sessionsLabel = new Label();
        sessionsLabel.getStyleClass().add("sidebar-section-label");
        sessionsLabel.textProperty().bind(
            Bindings.createStringBinding(
                () -> vm.completedTodayProperty().get() + " sessions today",
                vm.completedTodayProperty()
            )
        );

        StackPane ringStack = new StackPane(ringCanvas, countdownLabel);
        ringStack.setAlignment(Pos.CENTER);

        // Buttons
        startBtn = new Button("Start");
        startBtn.setGraphic(new FontIcon(MaterialDesignP.PLAY));
        startBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        startBtn.setMaxWidth(120);
        startBtn.setOnAction(e -> vm.startWork());

        pauseBtn = new Button("Pause");
        pauseBtn.setGraphic(new FontIcon(MaterialDesignP.PAUSE));
        pauseBtn.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        pauseBtn.setMaxWidth(120);
        pauseBtn.setOnAction(e -> vm.pauseResume());

        stopBtn = new Button("Stop");
        stopBtn.setGraphic(new FontIcon(MaterialDesignS.STOP));
        stopBtn.getStyleClass().addAll(Styles.DANGER, Styles.LARGE);
        stopBtn.setMaxWidth(120);
        stopBtn.setOnAction(e -> vm.stop());

        HBox btnRow = new HBox(12, startBtn, pauseBtn, stopBtn);
        btnRow.setAlignment(Pos.CENTER);

        VBox box = new VBox(12, ringStack, statusLabel, sessionsLabel, btnRow);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(RING_SIZE + 40);
        return box;
    }

    // ── Controls ──────────────────────────────────────────────────────────────

    private VBox buildControlBox() {
        // Task picker
        Label taskLabel = new Label("TASK");
        taskLabel.getStyleClass().add("sidebar-section-label");

        ComboBox<Task> taskCombo = new ComboBox<>(vm.getActiveTasks());
        taskCombo.valueProperty().bindBidirectional(vm.selectedTaskProperty());
        taskCombo.setMaxWidth(Double.MAX_VALUE);
        taskCombo.setPromptText("Pick a task…");
        taskCombo.setCellFactory(lv -> new TaskComboCell());
        taskCombo.setButtonCell(new TaskComboCell());

        // Duration settings
        Label settingsLabel = new Label("DURATION");
        settingsLabel.getStyleClass().add("sidebar-section-label");

        Spinner<Integer> workSpinner = new Spinner<>(1, 90, vm.workMinutesProperty().get());
        workSpinner.setEditable(true);
        workSpinner.setPrefWidth(80);
        workSpinner.valueProperty().addListener((obs, old, nv) ->
            vm.workMinutesProperty().set(nv));

        Spinner<Integer> breakSpinner = new Spinner<>(1, 30, vm.breakMinutesProperty().get());
        breakSpinner.setEditable(true);
        breakSpinner.setPrefWidth(80);
        breakSpinner.valueProperty().addListener((obs, old, nv) ->
            vm.breakMinutesProperty().set(nv));

        GridPane settings = new GridPane();
        settings.setHgap(10);
        settings.setVgap(6);
        settings.addRow(0, new Label("Work (min):"),  workSpinner);
        settings.addRow(1, new Label("Break (min):"), breakSpinner);

        VBox box = new VBox(8, taskLabel, taskCombo, settingsLabel, settings);
        box.getStyleClass().add("pomodoro-controls");
        return box;
    }

    // ── History ───────────────────────────────────────────────────────────────

    private VBox buildHistoryBox() {
        Label heading = new Label("TODAY'S SESSIONS");
        heading.getStyleClass().add("sidebar-section-label");

        ListView<PomodoroSession> historyList = new ListView<>(vm.getTodaySessions());
        historyList.setCellFactory(lv -> new SessionCell());
        historyList.setPrefHeight(180);
        historyList.getStyleClass().add("task-list");
        historyList.setPlaceholder(new Label("No sessions yet today."));

        VBox box = new VBox(8, heading, historyList);
        return box;
    }

    // ── Ring drawing ──────────────────────────────────────────────────────────

    private void drawRing() {
        GraphicsContext gc = ringCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, RING_SIZE, RING_SIZE);

        double cx     = RING_SIZE / 2.0;
        double cy     = RING_SIZE / 2.0;
        double r      = (RING_SIZE - RING_WIDTH * 2) / 2.0;
        double startX = cx - r;
        double startY = cy - r;
        double diam   = r * 2;

        // Background track
        gc.setStroke(Color.web("#374151", 0.5));
        gc.setLineWidth(RING_WIDTH);
        gc.strokeOval(startX, startY, diam, diam);

        // Progress arc
        double progress = vm.getProgress();
        double arcExtent = -360.0 * progress;

        TimerState state = vm.timerStateProperty().get();
        Color ringColor = switch (state) {
            case WORKING -> Color.web("#EF4444");
            case BREAK   -> Color.web("#10B981");
            case IDLE    -> Color.web("#3B82F6");
        };

        gc.setStroke(ringColor);
        gc.setLineWidth(RING_WIDTH);
        gc.strokeArc(startX, startY, diam, diam, 90, arcExtent, javafx.scene.shape.ArcType.OPEN);
    }

    private void updateButtonState(TimerState state) {
        startBtn.setDisable(state != TimerState.IDLE);
        pauseBtn.setDisable(state == TimerState.IDLE);
        stopBtn.setDisable(state == TimerState.IDLE);
    }

    // ── Inner cells ───────────────────────────────────────────────────────────

    private static class TaskComboCell extends ListCell<Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            setText(empty || task == null ? null : task.getTitle());
        }
    }

    private static class SessionCell extends ListCell<PomodoroSession> {
        @Override
        protected void updateItem(PomodoroSession s, boolean empty) {
            super.updateItem(s, empty);
            if (empty || s == null) { setGraphic(null); return; }

            String status = s.isCompleted() ? "✓" : "✗";
            String time   = s.getStartedAt() != null
                ? s.getStartedAt().format(TIME_FMT) : "?";

            Label statusLbl = new Label(status);
            statusLbl.setStyle(s.isCompleted()
                ? "-fx-text-fill: -color-success-fg; -fx-font-weight: bold;"
                : "-fx-text-fill: -color-danger-fg;");
            statusLbl.setMinWidth(20);

            Label timeLbl = new Label(time);
            timeLbl.getStyleClass().add("task-card-meta");

            Label durLbl = new Label(s.getDurationMinutes() + " min");
            durLbl.getStyleClass().add("task-card-meta");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(8, statusLbl, timeLbl, spacer, durLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 8, 4, 8));
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
        }
    }
}

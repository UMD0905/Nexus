package com.nexus.ui.views;

import atlantafx.base.theme.Styles;
import com.nexus.model.Task;
import com.nexus.model.TimeBlock;
import com.nexus.viewmodel.TodayViewModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Day-planner view.
 *
 * <p>Layout:
 * <pre>
 * ┌─ toolbar ──────────────────────────────────────────────┐
 * │  ◀ [date] ▶   Today                  [+ Block]         │
 * ├─────────────────────────────┬──────────────────────────┤
 * │  Timeline (scrollable)      │  Backlog tasks due today  │
 * │  06:00 ─────────────────── │  (no time block assigned) │
 * │  [time block cards]         │                           │
 * │  23:00 ─────────────────── │                           │
 * └─────────────────────────────┴──────────────────────────┘
 * </pre>
 */
public class TodayView extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int  HOUR_START  = 6;
    private static final int  HOUR_END    = 23;
    private static final int  HOUR_HEIGHT = 60; // px per hour

    private final TodayViewModel vm;

    private Label     dateLabel;
    private VBox      timelinePane;
    private ListView<Task> backlogList;

    public TodayView(TodayViewModel vm) {
        this.vm = vm;
        build();
        vm.initialize();
        vm.getTimeBlocks().addListener((javafx.collections.ListChangeListener<TimeBlock>) c -> refreshTimeline());
        vm.getBacklogTasks().addListener((javafx.collections.ListChangeListener<Task>) c -> backlogList.refresh());
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("today-view");

        setTop(buildToolbar());

        // Left: timeline
        timelinePane = new VBox(0);
        timelinePane.getStyleClass().add("timeline-pane");
        ScrollPane timelineScroll = new ScrollPane(timelinePane);
        timelineScroll.setFitToWidth(true);
        timelineScroll.getStyleClass().add("edge-to-edge");
        timelineScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Right: backlog
        backlogList = new ListView<>();
        backlogList.setItems(vm.getBacklogTasks());
        backlogList.setCellFactory(lv -> new TaskMiniCell());
        backlogList.getStyleClass().addAll("task-list", "backlog-list");
        backlogList.setPlaceholder(new Label("All tasks for today are scheduled!"));

        Label backlogLabel = new Label("UNSCHEDULED TODAY");
        backlogLabel.getStyleClass().add("sidebar-section-label");
        backlogLabel.setPadding(new Insets(12, 12, 6, 12));

        VBox rightPane = new VBox(0, backlogLabel, backlogList);
        rightPane.getStyleClass().add("backlog-pane");
        VBox.setVgrow(backlogList, Priority.ALWAYS);

        SplitPane split = new SplitPane(timelineScroll, rightPane);
        split.setDividerPositions(0.62);
        SplitPane.setResizableWithParent(rightPane, false);

        setCenter(split);
        refreshTimeline();
    }

    private HBox buildToolbar() {
        Button prevBtn = new Button();
        prevBtn.setGraphic(new FontIcon(MaterialDesignC.CHEVRON_LEFT));
        prevBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        prevBtn.setOnAction(e -> vm.navigatePrevDay());

        Button nextBtn = new Button();
        nextBtn.setGraphic(new FontIcon(MaterialDesignC.CHEVRON_RIGHT));
        nextBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        nextBtn.setOnAction(e -> vm.navigateNextDay());

        dateLabel = new Label();
        dateLabel.getStyleClass().add("view-title");
        dateLabel.textProperty().bind(
            Bindings.createStringBinding(
                () -> vm.currentDateProperty().get().format(DATE_FMT),
                vm.currentDateProperty()
            )
        );

        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().addAll(Styles.SMALL, Styles.ACCENT);
        todayBtn.setOnAction(e -> vm.goToToday());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBlockBtn = new Button("Add Block");
        addBlockBtn.setGraphic(new FontIcon(MaterialDesignP.PLUS));
        addBlockBtn.getStyleClass().addAll(Styles.ACCENT);
        addBlockBtn.setOnAction(e -> showAddBlockDialog());

        HBox toolbar = new HBox(8, prevBtn, dateLabel, nextBtn, todayBtn, spacer, addBlockBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("list-toolbar");
        toolbar.setPadding(new Insets(8, 16, 8, 16));
        return toolbar;
    }

    // ── Timeline ──────────────────────────────────────────────────────────────

    private void refreshTimeline() {
        timelinePane.getChildren().clear();
        timelinePane.setPadding(new Insets(0, 0, 24, 0));

        for (int hour = HOUR_START; hour <= HOUR_END; hour++) {
            timelinePane.getChildren().add(buildHourRow(hour));
        }

        // Overlay time blocks on top of the hour rows
        for (TimeBlock block : vm.getTimeBlocks()) {
            if (block.getStartTime() == null || block.getEndTime() == null) continue;
            timelinePane.getChildren().add(buildBlockCard(block));
        }
    }

    private HBox buildHourRow(int hour) {
        Label hourLabel = new Label(String.format("%02d:00", hour));
        hourLabel.getStyleClass().add("timeline-hour-label");
        hourLabel.setMinWidth(50);

        Separator line = new Separator();
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox row = new HBox(8, hourLabel, line);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("timeline-hour-row");
        row.setMinHeight(HOUR_HEIGHT);
        row.setPrefHeight(HOUR_HEIGHT);
        row.setPadding(new Insets(0, 12, 0, 8));
        return row;
    }

    private VBox buildBlockCard(TimeBlock block) {
        String color = block.getColor() != null ? block.getColor() : "#3B82F6";

        Rectangle colorBar = new Rectangle(4, 40);
        colorBar.setFill(Color.web(color));
        colorBar.setArcWidth(4);
        colorBar.setArcHeight(4);

        String timeText = block.getStartTime().format(TIME_FMT) + " – " + block.getEndTime().format(TIME_FMT);
        Label timeLabel = new Label(timeText);
        timeLabel.getStyleClass().add("task-card-meta");

        Label titleLabel = new Label(block.getTitle() != null ? block.getTitle() : "Untitled");
        titleLabel.getStyleClass().add("task-card-title");
        titleLabel.setStyle("-fx-font-size: 13px;");

        VBox content = new VBox(2, titleLabel, timeLabel);
        content.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(8, colorBar, content);
        card.getStyleClass().add("time-block-card");
        card.setPadding(new Insets(6, 10, 6, 8));
        card.setAlignment(Pos.CENTER_LEFT);

        // Vertical offset: pixels from the top of the timeline pane
        int offsetMinutes = (block.getStartTime().getHour() - HOUR_START) * 60
                          + block.getStartTime().getMinute();
        int durationMinutes = (int) java.time.Duration.between(
            block.getStartTime(), block.getEndTime()).toMinutes();

        double topPx    = offsetMinutes * (HOUR_HEIGHT / 60.0);
        double heightPx = Math.max(durationMinutes * (HOUR_HEIGHT / 60.0), 32);

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(topPx, 60, 0, 60));
        wrapper.setPickOnBounds(false);

        // Delete context menu
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Remove block");
        deleteItem.setOnAction(e -> vm.deleteTimeBlock(block));
        menu.getItems().add(deleteItem);
        card.setOnContextMenuRequested(e -> menu.show(card, e.getScreenX(), e.getScreenY()));
        card.setStyle("-fx-cursor: hand; -fx-min-height: " + heightPx + ";");

        return wrapper;
    }

    // ── Add block dialog ──────────────────────────────────────────────────────

    private void showAddBlockDialog() {
        Dialog<TimeBlock> dialog = new Dialog<>();
        dialog.setTitle("Add Time Block");
        dialog.setHeaderText("Schedule a time block for " +
            vm.currentDateProperty().get().format(DateTimeFormatter.ofPattern("d MMMM")));

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Deep work, Review emails…");

        TextField startField = new TextField(LocalTime.now().withMinute(0).format(TIME_FMT));
        startField.setPromptText("HH:mm");
        startField.setPrefWidth(80);

        TextField endField = new TextField(LocalTime.now().plusHours(1).withMinute(0).format(TIME_FMT));
        endField.setPromptText("HH:mm");
        endField.setPrefWidth(80);

        ColorPicker colorPicker = new ColorPicker(Color.web("#3B82F6"));
        colorPicker.setPrefWidth(120);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Title:"), titleField);
        grid.addRow(1, new Label("Start:"), startField);
        grid.addRow(2, new Label("End:"),   endField);
        grid.addRow(3, new Label("Color:"), colorPicker);
        GridPane.setHgrow(titleField, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.disableProperty().bind(titleField.textProperty().map(String::isBlank));

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                LocalTime start = LocalTime.parse(startField.getText().trim());
                LocalTime end   = LocalTime.parse(endField.getText().trim());
                javafx.scene.paint.Color c = colorPicker.getValue();
                String hex = String.format("#%02X%02X%02X",
                    (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
                return TimeBlock.builder()
                    .title(titleField.getText().trim())
                    .startTime(start)
                    .endTime(end)
                    .color(hex)
                    .build();
            } catch (Exception e) {
                return null;
            }
        });

        dialog.showAndWait().ifPresent(vm::addTimeBlock);
    }

    // ── Mini task cell for backlog ─────────────────────────────────────────────

    private static class TaskMiniCell extends ListCell<Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
                return;
            }
            Label title = new Label(task.getTitle());
            title.getStyleClass().add("sidebar-row-label");
            title.setWrapText(true);

            String priorityColor = switch (task.getPriority()) {
                case CRITICAL -> "#EF4444";
                case HIGH     -> "#F97316";
                case MEDIUM   -> "#3B82F6";
                case LOW      -> "#6B7280";
            };
            Rectangle dot = new Rectangle(8, 8);
            dot.setFill(Color.web(priorityColor));
            dot.setArcWidth(8);
            dot.setArcHeight(8);

            HBox row = new HBox(8, dot, title);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 12, 6, 12));
            setGraphic(row);
            setStyle("-fx-background-color: transparent;");
        }
    }
}

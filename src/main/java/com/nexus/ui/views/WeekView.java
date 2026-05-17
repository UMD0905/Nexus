package com.nexus.ui.views;

import atlantafx.base.theme.Styles;
import com.nexus.model.Task;
import com.nexus.viewmodel.WeekViewModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Seven-column week view (Mon → Sun).
 *
 * <p>Each column shows the day name + date, then all tasks due that day
 * as compact cards. Navigating with prev/next week reloads automatically.
 */
public class WeekView extends BorderPane {

    private static final DateTimeFormatter HEADER_FMT = DateTimeFormatter.ofPattern("EEE\nd MMM");
    private static final DateTimeFormatter RANGE_FMT  = DateTimeFormatter.ofPattern("d MMM");

    private final WeekViewModel vm;

    private Label   rangeLabel;
    private HBox    columnsBox;

    public WeekView(WeekViewModel vm) {
        this.vm = vm;
        build();
        vm.initialize();
        vm.getWeekTasks().addListener((javafx.collections.ListChangeListener<Task>) c -> refreshColumns());
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("week-view");
        setTop(buildToolbar());

        columnsBox = new HBox(0);
        columnsBox.getStyleClass().add("week-columns");

        ScrollPane scroll = new ScrollPane(columnsBox);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("edge-to-edge");

        setCenter(scroll);
        refreshColumns();
    }

    private HBox buildToolbar() {
        Button prevBtn = new Button();
        prevBtn.setGraphic(new FontIcon(MaterialDesignC.CHEVRON_LEFT));
        prevBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        prevBtn.setOnAction(e -> vm.prevWeek());

        Button nextBtn = new Button();
        nextBtn.setGraphic(new FontIcon(MaterialDesignC.CHEVRON_RIGHT));
        nextBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        nextBtn.setOnAction(e -> vm.nextWeek());

        rangeLabel = new Label();
        rangeLabel.getStyleClass().add("view-title");
        rangeLabel.textProperty().bind(
            Bindings.createStringBinding(this::weekRangeText, vm.weekStartProperty())
        );

        Button nowBtn = new Button("This Week");
        nowBtn.getStyleClass().addAll(Styles.SMALL, Styles.ACCENT);
        nowBtn.setOnAction(e -> vm.goToCurrentWeek());

        HBox toolbar = new HBox(8, prevBtn, rangeLabel, nextBtn, nowBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("list-toolbar");
        toolbar.setPadding(new Insets(8, 16, 8, 16));
        return toolbar;
    }

    // ── Columns ───────────────────────────────────────────────────────────────

    private void refreshColumns() {
        columnsBox.getChildren().clear();
        List<LocalDate> days = vm.getWeekDays();
        LocalDate today = LocalDate.now();

        for (LocalDate day : days) {
            columnsBox.getChildren().add(buildDayColumn(day, today.equals(day)));
        }
    }

    private VBox buildDayColumn(LocalDate day, boolean isToday) {
        Label header = new Label(day.format(HEADER_FMT));
        header.getStyleClass().add("week-day-header");
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setPadding(new Insets(10, 4, 10, 4));
        if (isToday) {
            header.getStyleClass().add("week-day-header-today");
        }

        VBox column = new VBox(6);
        column.getStyleClass().add("week-day-column");
        if (isToday) column.getStyleClass().add("week-day-column-today");
        column.setPadding(new Insets(0, 4, 12, 4));
        HBox.setHgrow(column, Priority.ALWAYS);

        column.getChildren().add(header);

        List<Task> tasks = vm.getTasksForDay(day);
        if (tasks.isEmpty()) {
            Label empty = new Label("·");
            empty.setStyle("-fx-text-fill: -color-fg-subtle; -fx-font-size: 18px;");
            empty.setPadding(new Insets(8, 0, 0, 0));
            empty.setAlignment(Pos.CENTER);
            empty.setMaxWidth(Double.MAX_VALUE);
            column.getChildren().add(empty);
        } else {
            for (Task task : tasks) {
                column.getChildren().add(buildWeekTaskCard(task));
            }
        }

        return column;
    }

    private VBox buildWeekTaskCard(Task task) {
        String categoryColor = task.getCategory() != null
            ? task.getCategory().getColor() : "#6B7280";

        Rectangle colorStrip = new Rectangle(3, 36);
        colorStrip.setFill(Color.web(categoryColor));
        colorStrip.setArcWidth(3);
        colorStrip.setArcHeight(3);

        Label title = new Label(task.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        title.getStyleClass().add("task-card-title");

        String timeStr = task.getDueDate() != null
            ? task.getDueDate().toLocalTime().toString().substring(0, 5) : "";
        Label meta = new Label(timeStr);
        meta.getStyleClass().add("task-card-meta");
        meta.setStyle("-fx-font-size: 10px;");

        VBox text = new VBox(2, title, meta);
        text.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(6, colorStrip, text);
        card.getStyleClass().add("week-task-card");
        card.setPadding(new Insets(4, 6, 4, 4));
        card.setAlignment(Pos.CENTER_LEFT);

        VBox wrapper = new VBox(card);
        wrapper.setStyle("-fx-cursor: hand;");
        return wrapper;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String weekRangeText() {
        LocalDate monday = vm.weekStartProperty().get();
        LocalDate sunday = monday.plusDays(6);
        return monday.format(RANGE_FMT) + " – " + sunday.format(RANGE_FMT);
    }
}

package com.nexus.ui.views;

import atlantafx.base.theme.Styles;
import com.nexus.model.Streak;
import com.nexus.service.ExportService;
import com.nexus.viewmodel.DashboardViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * Dashboard view — the first thing you see when you want a quick status read.
 *
 * <p>Sections:
 * <ol>
 *   <li>Stat cards row (active tasks, due today, completed this week, pomodoro sessions)</li>
 *   <li>Weekly completions bar chart (Mon–Sun)</li>
 *   <li>Category breakdown pie chart</li>
 *   <li>Streaks panel (current + longest)</li>
 *   <li>Export button</li>
 * </ol>
 */
public class DashboardView extends BorderPane {

    private final DashboardViewModel vm;
    private final ExportService      exportService;

    public DashboardView(DashboardViewModel vm, ExportService exportService) {
        this.vm            = vm;
        this.exportService = exportService;
        build();
        vm.refresh();
    }

    public void refresh() {
        vm.refresh();
        // Rebuild to reflect fresh data
        build();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("dashboard-view");

        setTop(buildToolbar());

        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("edge-to-edge");
        setCenter(scroll);
    }

    private HBox buildToolbar() {
        Label title = new Label("Dashboard");
        title.getStyleClass().add("view-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button refreshBtn = new Button();
        refreshBtn.setGraphic(new FontIcon(MaterialDesignR.REFRESH));
        refreshBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        refreshBtn.setOnAction(e -> refresh());

        Button exportBtn = new Button("Export JSON");
        exportBtn.setGraphic(new FontIcon(MaterialDesignD.DOWNLOAD));
        exportBtn.getStyleClass().add(Styles.ACCENT);
        exportBtn.setOnAction(e -> onExport());

        HBox toolbar = new HBox(8, title, refreshBtn, exportBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("list-toolbar");
        toolbar.setPadding(new Insets(8, 16, 8, 16));
        return toolbar;
    }

    private VBox buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        content.getChildren().addAll(
            buildStatCards(),
            buildChartsRow(),
            buildStreaksPanel()
        );
        return content;
    }

    // ── Stat cards ────────────────────────────────────────────────────────────

    private HBox buildStatCards() {
        HBox row = new HBox(12,
            statCard("Active Tasks",          String.valueOf(vm.totalActiveProperty().get()),
                     "#3B82F6", MaterialDesignC.CHECKBOX_MARKED_CIRCLE_OUTLINE),
            statCard("Due Today",             String.valueOf(vm.dueTodayProperty().get()),
                     "#F59E0B", MaterialDesignC.CALENDAR_TODAY),
            statCard("Completed This Week",   String.valueOf(vm.completedWeekProperty().get()),
                     "#10B981", MaterialDesignC.CHECK_CIRCLE_OUTLINE),
            statCard("Overdue",               String.valueOf(vm.overdueTasksProperty().get()),
                     "#EF4444", MaterialDesignA.ALERT_CIRCLE_OUTLINE),
            statCard("Pomodoro Today",        String.valueOf(vm.pomodoroTodayProperty().get()),
                     "#8B5CF6", MaterialDesignT.TIMER_OUTLINE)
        );
        row.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        return row;
    }

    private VBox statCard(String label, String value, String color, Object icon) {
        FontIcon fi = new FontIcon((org.kordamp.ikonli.Ikon) icon);
        fi.setIconSize(22);
        fi.setStyle("-fx-icon-color: " + color + ";");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label labelLbl = new Label(label);
        labelLbl.getStyleClass().add("task-card-meta");
        labelLbl.setStyle("-fx-font-size: 11px;");

        VBox card = new VBox(6, fi, valueLbl, labelLbl);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("dashboard-stat-card");
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: -color-bg-subtle;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: -color-border-muted;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;" +
            "-fx-border-left-width: 4;" +
            "-fx-border-color: " + color + " -color-border-muted -color-border-muted " + color + ";"
        );
        return card;
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    private HBox buildChartsRow() {
        BarChart<String, Number> weekChart = buildWeeklyChart();
        PieChart pieChart                   = buildCategoryPie();

        HBox row = new HBox(16, weekChart, pieChart);
        HBox.setHgrow(weekChart, Priority.ALWAYS);
        HBox.setHgrow(pieChart,  Priority.ALWAYS);
        return row;
    }

    private BarChart<String, Number> buildWeeklyChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Day");
        yAxis.setLabel("Completed");
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Tasks Completed This Week");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        int[] counts = new int[7];
        if (!vm.getWeeklyCompletions().isEmpty()) {
            counts = vm.getWeeklyCompletions().get(0);
        }
        for (int i = 0; i < 7; i++) {
            series.getData().add(new XYChart.Data<>(days[i], counts[i]));
        }
        chart.getData().add(series);

        // Highlight today
        int todayIdx = LocalDate.now().getDayOfWeek().getValue() - 1;
        if (todayIdx >= 0 && todayIdx < series.getData().size()) {
            series.getData().get(todayIdx).getNode()
                .setStyle("-fx-bar-fill: -color-accent-emphasis;");
        }

        return chart;
    }

    private PieChart buildCategoryPie() {
        PieChart pie = new PieChart();
        pie.setTitle("Active Tasks by Life Area");
        pie.setAnimated(false);
        pie.setLegendVisible(true);
        pie.setPrefHeight(260);
        pie.setLabelsVisible(true);

        Map<String, Integer> breakdown = vm.getCategoryBreakdown();
        if (breakdown.isEmpty()) {
            pie.getData().add(new PieChart.Data("No tasks", 1));
        } else {
            breakdown.forEach((name, count) ->
                pie.getData().add(new PieChart.Data(name + " (" + count + ")", count)));
        }
        return pie;
    }

    // ── Streaks panel ─────────────────────────────────────────────────────────

    private VBox buildStreaksPanel() {
        Label heading = new Label("STREAKS");
        heading.getStyleClass().add("sidebar-section-label");
        heading.setPadding(new Insets(0, 0, 8, 0));

        HBox streakCards = new HBox(12);
        streakCards.setAlignment(Pos.CENTER_LEFT);

        if (vm.getStreaks().isEmpty()) {
            Label empty = new Label("No streaks tracked yet. Complete recurring tasks to build streaks.");
            empty.getStyleClass().add("task-card-meta");
            streakCards.getChildren().add(empty);
        } else {
            for (Streak streak : vm.getStreaks()) {
                streakCards.getChildren().add(buildStreakCard(streak));
            }
        }

        VBox panel = new VBox(6, heading, streakCards);
        return panel;
    }

    private VBox buildStreakCard(Streak streak) {
        String accentColor = streak.getCategory() != null
            ? streak.getCategory().getColor() : "#6B7280";

        boolean active = streak.isActive();
        String fireColor = active ? "#F97316" : "-color-fg-muted";

        Label fireIcon = new Label(active ? "🔥" : "❄");
        fireIcon.setStyle("-fx-font-size: 20px;");

        Label nameLbl = new Label(streak.getTitle());
        nameLbl.getStyleClass().add("task-card-title");
        nameLbl.setStyle("-fx-font-size: 13px;");

        Label currentLbl = new Label(streak.getCurrentStreak() + " day" +
            (streak.getCurrentStreak() != 1 ? "s" : ""));
        currentLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + fireColor + ";");

        Label bestLbl = new Label("Best: " + streak.getLongestStreak());
        bestLbl.getStyleClass().add("task-card-meta");
        bestLbl.setStyle("-fx-font-size: 10px;");

        String lastStr = streak.getLastCompletedDate() != null
            ? "Last: " + streak.getLastCompletedDate()
            : "Not started";
        Label lastLbl = new Label(lastStr);
        lastLbl.getStyleClass().add("task-card-meta");
        lastLbl.setStyle("-fx-font-size: 10px;");

        HBox header = new HBox(6, fireIcon, nameLbl);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(4, header, currentLbl, bestLbl, lastLbl);
        card.setPadding(new Insets(14));
        card.setPrefWidth(160);
        card.setStyle(
            "-fx-background-color: -color-bg-subtle;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + accentColor + " -color-border-muted -color-border-muted " + accentColor + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1 1 1 4;"
        );
        return card;
    }

    // ── Export ────────────────────────────────────────────────────────────────

    private void onExport() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Export Directory");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File dir = chooser.showDialog(getScene().getWindow());
        if (dir == null) return;

        try {
            File exported = exportService.exportTo(dir);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText("Backup saved");
            alert.setContentText("File: " + exported.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setHeaderText("Could not export backup");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}

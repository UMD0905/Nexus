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
import javafx.stage.DirectoryChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;

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
        build();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("dashboard-view");
        setTop(buildToolbar());

        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
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
        VBox content = new VBox(24);
        content.setPadding(new Insets(22));
        content.setStyle("-fx-background-color: #090d18;");
        content.getChildren().addAll(
            buildStatCards(),
            buildChartsRow(),
            buildStreaksPanel()
        );
        return content;
    }

    // ── Stat cards ────────────────────────────────────────────────────────────

    private HBox buildStatCards() {
        HBox row = new HBox(14,
            statCard("Active Tasks",        vm.totalActiveProperty().get(),
                     "#6373f4", "stat-accent-active",    MaterialDesignC.CHECKBOX_MARKED_CIRCLE_OUTLINE),
            statCard("Due Today",           vm.dueTodayProperty().get(),
                     "#e8a020", "stat-accent-due",       MaterialDesignC.CALENDAR_TODAY),
            statCard("Done This Week",      vm.completedWeekProperty().get(),
                     "#2dba82", "stat-accent-done",      MaterialDesignC.CHECK_CIRCLE_OUTLINE),
            statCard("Overdue",             vm.overdueTasksProperty().get(),
                     "#f05a5a", "stat-accent-overdue",   MaterialDesignA.ALERT_CIRCLE_OUTLINE),
            statCard("Pomodoros Today",     vm.pomodoroTodayProperty().get(),
                     "#a78bfa", "stat-accent-pomodoro",  MaterialDesignT.TIMER_OUTLINE)
        );
        row.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        return row;
    }

    private VBox statCard(String label, int value, String accentHex,
                          String valueStyleClass, org.kordamp.ikonli.Ikon icon) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(20);
        fi.setStyle("-fx-icon-color: " + accentHex + "; opacity: 0.75;");

        Label valueLbl = new Label(String.valueOf(value));
        valueLbl.getStyleClass().addAll("stat-value", valueStyleClass);

        Label labelLbl = new Label(label.toUpperCase());
        labelLbl.getStyleClass().add("stat-label");

        Region spacer = new Region();
        HBox iconRow = new HBox(fi);
        iconRow.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(spacer, iconRow);
        VBox card = new VBox(4, top, valueLbl, labelLbl);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("dashboard-stat-card");
        card.setPadding(new Insets(16, 16, 14, 18));
        // 4-px accent left border
        card.setStyle(
            "-fx-border-color: " + accentHex + " rgba(255,255,255,0.07) " +
            "rgba(255,255,255,0.07) " + accentHex + ";" +
            "-fx-border-width: 1 1 1 4; -fx-border-radius: 10; -fx-background-radius: 10;"
        );
        return card;
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    private HBox buildChartsRow() {
        BarChart<String, Number> weekChart = buildWeeklyChart();
        PieChart pieChart                  = buildCategoryPie();

        HBox row = new HBox(18, weekChart, pieChart);
        HBox.setHgrow(weekChart, Priority.ALWAYS);
        HBox.setHgrow(pieChart,  Priority.ALWAYS);
        return row;
    }

    private BarChart<String, Number> buildWeeklyChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Completions This Week");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(240);
        chart.setStyle("-fx-background-color: transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[] days   = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int[]    counts = vm.getWeeklyCompletions().isEmpty()
            ? new int[7] : vm.getWeeklyCompletions().get(0);

        for (int i = 0; i < 7; i++) series.getData().add(new XYChart.Data<>(days[i], counts[i]));
        chart.getData().add(series);

        // Highlight today's bar
        int todayIdx = LocalDate.now().getDayOfWeek().getValue() - 1;
        for (int i = 0; i < series.getData().size(); i++) {
            javafx.scene.Node node = series.getData().get(i).getNode();
            if (node != null) {
                node.setStyle(i == todayIdx
                    ? "-fx-bar-fill: #2dba82; -fx-effect: dropshadow(gaussian,rgba(45,186,130,0.45),8,0,0,0);"
                    : "-fx-bar-fill: #6373f4; -fx-effect: dropshadow(gaussian,rgba(99,115,244,0.30),6,0,0,0);");
            }
        }
        return chart;
    }

    private PieChart buildCategoryPie() {
        PieChart pie = new PieChart();
        pie.setTitle("Tasks by Life Area");
        pie.setAnimated(false);
        pie.setLegendVisible(true);
        pie.setPrefHeight(240);
        pie.setLabelsVisible(true);
        pie.setStyle("-fx-background-color: transparent;");

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
        heading.getStyleClass().add("section-header-label");
        heading.setPadding(new Insets(0, 0, 10, 0));

        HBox cards = new HBox(14);
        cards.setAlignment(Pos.CENTER_LEFT);

        if (vm.getStreaks().isEmpty()) {
            Label empty = new Label("No streaks yet — complete recurring tasks to start tracking.");
            empty.getStyleClass().add("empty-state-label");
            cards.getChildren().add(empty);
        } else {
            vm.getStreaks().forEach(s -> cards.getChildren().add(buildStreakCard(s)));
        }

        VBox panel = new VBox(6, heading, cards);
        return panel;
    }

    private VBox buildStreakCard(Streak streak) {
        boolean active     = streak.isActive();
        String  accentHex  = active ? "#e8a020" : "#4a5770";
        String  catColor   = streak.getCategory() != null ? streak.getCategory().getColor() : accentHex;

        Label icon = new Label(active ? "🔥" : "❄");
        icon.setStyle("-fx-font-size: 22px;");

        Label name = new Label(streak.getTitle());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #dce7f5;");

        HBox header = new HBox(7, icon, name);
        header.setAlignment(Pos.CENTER_LEFT);

        Label currentVal = new Label(streak.getCurrentStreak() + "d");
        currentVal.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + accentHex + ";" +
            (active ? " -fx-effect: dropshadow(gaussian,rgba(232,160,32,0.45),10,0,0,0);" : ""));

        Label currentLbl = new Label("current streak");
        currentLbl.getStyleClass().add("stat-label");

        Label bestLbl = new Label("Best: " + streak.getLongestStreak() + " days");
        bestLbl.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #5e7090;");

        String lastStr = streak.getLastCompletedDate() != null
            ? "Last: " + streak.getLastCompletedDate() : "Not started";
        Label lastLbl = new Label(lastStr);
        lastLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a5770;");

        VBox card = new VBox(5, header, currentVal, currentLbl, bestLbl, lastLbl);
        card.setPadding(new Insets(16));
        card.setPrefWidth(170);
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #131d30, #0f1828);" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + catColor + " rgba(255,255,255,0.07) rgba(255,255,255,0.07) " + catColor + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1 1 1 4;" +
            (active ? "-fx-effect: dropshadow(gaussian,rgba(232,160,32,0.15),12,0,0,3);" : "")
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
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Export Successful"); a.setHeaderText("Backup saved");
            a.setContentText("File: " + exported.getAbsolutePath()); a.showAndWait();
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Export Failed"); a.setHeaderText("Could not export backup");
            a.setContentText(e.getMessage()); a.showAndWait();
        }
    }
}

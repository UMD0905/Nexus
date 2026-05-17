package com.nexus.ui.views;

import atlantafx.base.theme.Styles;
import com.nexus.model.Category;
import com.nexus.model.Goal;
import com.nexus.model.Task;
import com.nexus.viewmodel.GoalsViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Goals management view.
 *
 * <p>Shows each goal as a card with a progress bar (% linked tasks done),
 * target date, and action buttons. A "New Goal" dialog lets the user create
 * and configure goals.
 */
public class GoalsView extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final GoalsViewModel vm;
    private VBox goalsList;

    public GoalsView(GoalsViewModel vm) {
        this.vm = vm;
        build();
        vm.initialize();
        vm.getGoals().addListener((javafx.collections.ListChangeListener<Goal>) c -> refreshList());
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("goals-view");
        setTop(buildToolbar());

        goalsList = new VBox(10);
        goalsList.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(goalsList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("edge-to-edge");

        setCenter(scroll);
        refreshList();
    }

    private HBox buildToolbar() {
        Label title = new Label("Goals");
        title.getStyleClass().add("view-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button newBtn = new Button("New Goal");
        newBtn.setGraphic(new FontIcon(MaterialDesignP.PLUS));
        newBtn.getStyleClass().add(Styles.ACCENT);
        newBtn.setOnAction(e -> showNewGoalDialog());

        HBox toolbar = new HBox(8, title, newBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("list-toolbar");
        toolbar.setPadding(new Insets(8, 16, 8, 16));
        return toolbar;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    private void refreshList() {
        goalsList.getChildren().clear();

        List<Goal> goals = vm.getGoals();
        if (goals.isEmpty()) {
            Label empty = new Label("No goals yet — create your first goal to get started.");
            empty.getStyleClass().add("empty-state-label");
            empty.setPadding(new Insets(40));
            empty.setWrapText(true);
            goalsList.getChildren().add(empty);
            return;
        }

        // Group: Active, then Completed/Abandoned
        goals.stream()
            .filter(g -> "ACTIVE".equals(g.getStatus()))
            .forEach(g -> goalsList.getChildren().add(buildGoalCard(g)));

        List<Goal> inactive = goals.stream()
            .filter(g -> !"ACTIVE".equals(g.getStatus()))
            .toList();

        if (!inactive.isEmpty()) {
            Label sep = new Label("COMPLETED / ABANDONED");
            sep.getStyleClass().add("sidebar-section-label");
            sep.setPadding(new Insets(12, 0, 4, 0));
            goalsList.getChildren().add(sep);
            inactive.forEach(g -> goalsList.getChildren().add(buildGoalCard(g)));
        }
    }

    private VBox buildGoalCard(Goal goal) {
        String accentColor = goal.getCategory() != null
            ? goal.getCategory().getColor() : "#6B7280";

        // Left border strip
        Rectangle strip = new Rectangle(4, 60);
        strip.setFill(Color.web(accentColor));
        strip.setArcWidth(4);
        strip.setArcHeight(4);

        // Title + category
        Label titleLbl = new Label(goal.getTitle());
        titleLbl.getStyleClass().add("task-card-title");
        titleLbl.setStyle("-fx-font-size: 14px;");
        titleLbl.setWrapText(true);

        String catName = goal.getCategory() != null ? goal.getCategory().getName() : "Uncategorised";
        Label catLbl = new Label(catName);
        catLbl.getStyleClass().add("task-card-meta");

        // Progress bar
        double progress = vm.getProgress(goal);
        ProgressBar bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("goal-progress-bar");
        int pct = (int) (progress * 100);
        Label pctLbl = new Label(pct + "%  (" + completionSummary(goal) + ")");
        pctLbl.getStyleClass().add("task-card-meta");
        pctLbl.setStyle("-fx-font-size: 10px;");

        VBox progressBox = new VBox(3, bar, pctLbl);

        // Target date
        String dateStr = goal.getTargetDate() != null
            ? "Target: " + goal.getTargetDate().format(DATE_FMT) : "No target date";
        Label dateLbl = new Label(dateStr);
        dateLbl.getStyleClass().add("task-card-meta");

        // Status badge
        Label statusBadge = new Label(goal.getStatus());
        statusBadge.getStyleClass().addAll("task-card-status-badge",
            "ACTIVE".equals(goal.getStatus()) ? "status-in_progress"
          : "COMPLETED".equals(goal.getStatus()) ? "status-done" : "status-cancelled");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox metaRow = new HBox(8, catLbl, spacer, dateLbl, statusBadge);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(6, titleLbl, metaRow, progressBox);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Action buttons
        Button doneBtn = new Button();
        doneBtn.setGraphic(new FontIcon(MaterialDesignC.CHECK));
        doneBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.SUCCESS);
        doneBtn.setTooltip(new Tooltip("Mark completed"));
        doneBtn.setDisable(!"ACTIVE".equals(goal.getStatus()));
        doneBtn.setOnAction(e -> vm.completeGoal(goal));

        Button deleteBtn = new Button();
        deleteBtn.setGraphic(new FontIcon(MaterialDesignD.DELETE_OUTLINE));
        deleteBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
        deleteBtn.setTooltip(new Tooltip("Delete goal"));
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete goal \"" + goal.getTitle() + "\"?",
                ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().filter(b -> b == ButtonType.YES)
                .ifPresent(b -> vm.deleteGoal(goal));
        });

        VBox actions = new VBox(4, doneBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER);

        HBox card = new HBox(10, strip, content, actions);
        card.getStyleClass().add("goal-card");
        card.setPadding(new Insets(12, 12, 12, 0));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: -color-bg-subtle;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-border-color: -color-border-muted;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 4, 0, 0, 1);"
        );

        return new VBox(card);
    }

    private String completionSummary(Goal goal) {
        List<Task> tasks = goal.getTasks();
        if (tasks.isEmpty()) return "no tasks linked";
        long done = tasks.stream()
            .filter(t -> t.getStatus() == com.nexus.model.enums.TaskStatus.DONE)
            .count();
        return done + "/" + tasks.size() + " tasks";
    }

    // ── New goal dialog ───────────────────────────────────────────────────────

    private void showNewGoalDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("New Goal");
        dialog.setHeaderText("Define your next goal");

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Ship MVP, Complete kickboxing belt…");

        TextArea descField = new TextArea();
        descField.setPromptText("Description (optional)");
        descField.setPrefHeight(70);
        descField.setWrapText(true);

        ComboBox<Category> catCombo = new ComboBox<>(vm.getCategories());
        catCombo.setPromptText("Life area…");
        catCombo.setMaxWidth(Double.MAX_VALUE);
        catCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Category c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) { setText(null); setGraphic(null); return; }
                Circle dot = new Circle(6, Color.web(c.getColor()));
                Label lbl  = new Label(c.getName());
                HBox row   = new HBox(6, dot, lbl);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        catCombo.setButtonCell(catCombo.getCellFactory().call(null));

        DatePicker targetPicker = new DatePicker();
        targetPicker.setPromptText("Target date (optional)");
        targetPicker.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Title:"),       titleField);
        grid.addRow(1, new Label("Description:"), descField);
        grid.addRow(2, new Label("Life area:"),   catCombo);
        grid.addRow(3, new Label("Target date:"), targetPicker);
        GridPane.setHgrow(titleField,    Priority.ALWAYS);
        GridPane.setHgrow(descField,     Priority.ALWAYS);
        GridPane.setHgrow(catCombo,      Priority.ALWAYS);
        GridPane.setHgrow(targetPicker,  Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.disableProperty().bind(titleField.textProperty().map(String::isBlank));

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                vm.createGoal(
                    titleField.getText(),
                    descField.getText(),
                    catCombo.getValue(),
                    targetPicker.getValue()
                );
            }
            return null;
        });

        dialog.showAndWait();
    }
}

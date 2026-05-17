package com.nexus.ui.views;

import atlantafx.base.theme.Styles;
import com.nexus.model.Task;
import com.nexus.viewmodel.EisenhowerViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

/**
 * Eisenhower priority matrix view.
 *
 * <p>Layout:
 * <pre>
 *  ┌──────────────────┬──────────────────┐
 *  │  Q1 DO FIRST     │  Q2 SCHEDULE     │
 *  │  Urgent+Imp      │  !Urgent+Imp     │
 *  ├──────────────────┼──────────────────┤
 *  │  Q3 DELEGATE     │  Q4 ELIMINATE    │
 *  │  Urgent+!Imp     │  !Urgent+!Imp    │
 *  └──────────────────┴──────────────────┘
 * </pre>
 *
 * Each task card has a context menu to move it to another quadrant.
 */
public class EisenhowerView extends BorderPane {

    private final EisenhowerViewModel vm;

    public EisenhowerView(EisenhowerViewModel vm) {
        this.vm = vm;
        build();
        vm.initialize();
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("eisenhower-view");

        // Toolbar
        Label title = new Label("Eisenhower Matrix");
        title.getStyleClass().add("view-title");

        Button refreshBtn = new Button();
        refreshBtn.setGraphic(new FontIcon(MaterialDesignR.REFRESH));
        refreshBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        refreshBtn.setOnAction(e -> vm.reload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, title, spacer, refreshBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("list-toolbar");
        toolbar.setPadding(new Insets(8, 16, 8, 16));
        setTop(toolbar);

        // 2×2 grid
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.getStyleClass().add("eisenhower-grid");
        grid.setPadding(new Insets(0));

        // Column constraints: equal widths
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col, new ColumnConstraints() {{ setPercentWidth(50); }});

        // Row constraints: equal heights
        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);
        row1.setVgrow(Priority.ALWAYS);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(50);
        row2.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(row1, row2);

        // Quadrant colours
        String q1Color = "#EF4444"; // red  — do first
        String q2Color = "#3B82F6"; // blue — schedule
        String q3Color = "#F59E0B"; // amber — delegate
        String q4Color = "#6B7280"; // grey — eliminate

        grid.add(buildQuadrant("DO FIRST",  "Urgent + Important",    q1Color, true,  true),  0, 0);
        grid.add(buildQuadrant("SCHEDULE",  "Not Urgent + Important", q2Color, false, true),  1, 0);
        grid.add(buildQuadrant("DELEGATE",  "Urgent + Not Important", q3Color, true,  false), 0, 1);
        grid.add(buildQuadrant("ELIMINATE", "Not Urgent + Not Important", q4Color, false, false), 1, 1);

        setCenter(grid);

        // Reload quadrants when data changes
        vm.getDoFirst().addListener((javafx.collections.ListChangeListener<Task>) c -> refreshQuadrants(grid));
        vm.getSchedule().addListener((javafx.collections.ListChangeListener<Task>) c -> refreshQuadrants(grid));
        vm.getDelegate().addListener((javafx.collections.ListChangeListener<Task>) c -> refreshQuadrants(grid));
        vm.getEliminate().addListener((javafx.collections.ListChangeListener<Task>) c -> refreshQuadrants(grid));
    }

    private void refreshQuadrants(GridPane grid) {
        grid.getChildren().clear();
        String q1Color = "#EF4444";
        String q2Color = "#3B82F6";
        String q3Color = "#F59E0B";
        String q4Color = "#6B7280";
        grid.add(buildQuadrant("DO FIRST",  "Urgent + Important",         q1Color, true,  true),  0, 0);
        grid.add(buildQuadrant("SCHEDULE",  "Not Urgent + Important",     q2Color, false, true),  1, 0);
        grid.add(buildQuadrant("DELEGATE",  "Urgent + Not Important",     q3Color, true,  false), 0, 1);
        grid.add(buildQuadrant("ELIMINATE", "Not Urgent + Not Important", q4Color, false, false), 1, 1);
    }

    // ── Quadrant builder ──────────────────────────────────────────────────────

    private VBox buildQuadrant(String heading, String subtitle,
                                String accentHex,
                                boolean urgent, boolean important) {
        // Header strip
        Label headLabel = new Label(heading);
        headLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
                           "-fx-text-fill: white;");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.75);");

        VBox header = new VBox(2, headLabel, subLabel);
        header.setPadding(new Insets(10, 14, 10, 14));
        header.setStyle("-fx-background-color: " + accentHex + ";" +
                        "-fx-background-radius: 0;");

        // Task list
        javafx.collections.ObservableList<Task> items = switch (heading) {
            case "DO FIRST"  -> vm.getDoFirst();
            case "SCHEDULE"  -> vm.getSchedule();
            case "DELEGATE"  -> vm.getDelegate();
            default          -> vm.getEliminate();
        };

        VBox taskList = new VBox(4);
        taskList.setPadding(new Insets(8, 8, 8, 8));
        VBox.setVgrow(taskList, Priority.ALWAYS);

        if (items.isEmpty()) {
            Label empty = new Label("No tasks");
            empty.setStyle("-fx-text-fill: -color-fg-subtle; -fx-font-size: 12px;");
            taskList.getChildren().add(empty);
        } else {
            for (Task task : items) {
                taskList.getChildren().add(buildTaskRow(task, accentHex, urgent, important));
            }
        }

        ScrollPane scroll = new ScrollPane(taskList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox quadrant = new VBox(0, header, scroll);
        quadrant.getStyleClass().add("eisenhower-quadrant");
        VBox.setVgrow(quadrant, Priority.ALWAYS);
        return quadrant;
    }

    private HBox buildTaskRow(Task task, String accentHex,
                               boolean currentUrgent, boolean currentImportant) {
        Rectangle dot = new Rectangle(8, 8);
        dot.setFill(Color.web(accentHex));
        dot.setArcWidth(8);
        dot.setArcHeight(8);

        Label lbl = new Label(task.getTitle());
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-fg-default;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, dot, lbl, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("eisenhower-task-row");
        row.setPadding(new Insets(5, 8, 5, 4));
        row.setStyle("-fx-background-radius: 4; -fx-cursor: hand;");

        // Hover highlight
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: -color-bg-inset; " +
                                                "-fx-background-radius: 4; -fx-cursor: hand;"));
        row.setOnMouseExited(e ->  row.setStyle("-fx-background-radius: 4; -fx-cursor: hand;"));

        // Context menu: move to another quadrant
        ContextMenu menu = new ContextMenu();
        if (!( currentUrgent &&  currentImportant)) addMoveItem(menu, task, "Do First",  true,  true);
        if (!(!currentUrgent &&  currentImportant)) addMoveItem(menu, task, "Schedule",  false, true);
        if (!( currentUrgent && !currentImportant)) addMoveItem(menu, task, "Delegate",  true,  false);
        if (!(!currentUrgent && !currentImportant)) addMoveItem(menu, task, "Eliminate", false, false);

        row.setOnContextMenuRequested(e -> menu.show(row, e.getScreenX(), e.getScreenY()));

        return row;
    }

    private void addMoveItem(ContextMenu menu, Task task,
                              String label, boolean urgent, boolean important) {
        MenuItem item = new MenuItem("→ Move to " + label);
        item.setOnAction(e -> vm.moveTask(task, urgent, important));
        menu.getItems().add(item);
    }
}

package com.nexus.ui.views;

import atlantafx.base.theme.Styles;
import com.nexus.model.Category;
import com.nexus.model.Task;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.ui.components.TaskCard;
import com.nexus.viewmodel.TaskDetailViewModel;
import com.nexus.viewmodel.TaskListViewModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

/**
 * Main task list view.
 *
 * <p>Layout:
 * <pre>
 * ┌───────────────────────────────────────────────────────────┐
 * │  View title     [Search…]  [Priority▼] [Status▼]  [+ New]│
 * ├───────────────────────────────────────────────────────────┤
 * │  TaskCard                                                 │
 * │  TaskCard                                                 │
 * │  …                                                        │
 * └───────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Selecting a task exposes the detail panel in the parent
 * {@code MainWindow}.
 */
public class TaskListView extends BorderPane {

    private final TaskListViewModel   listVm;
    private final TaskDetailViewModel detailVm;
    private final Runnable            onShowDetail;
    private final Runnable            onHideDetail;

    private final ListView<Task>  taskListView   = new ListView<>();
    private final Label           viewTitleLabel = new Label("All Tasks");
    private final TextField       searchField    = new TextField();

    public TaskListView(TaskListViewModel listVm,
                        TaskDetailViewModel detailVm,
                        Runnable onShowDetail,
                        Runnable onHideDetail) {
        this.listVm      = listVm;
        this.detailVm    = detailVm;
        this.onShowDetail = onShowDetail;
        this.onHideDetail = onHideDetail;
        build();
        bindToViewModel();
    }

    /** Updates the title and reloads for a different scope (category / archive). */
    public void setViewTitle(String title) {
        viewTitleLabel.setText(title);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        // ── Toolbar ──────────────────────────────────────────────────────────
        viewTitleLabel.getStyleClass().add("view-title");

        searchField.setPromptText("Search tasks…");
        searchField.setPrefWidth(200);
        searchField.getStyleClass().add(Styles.ROUNDED);

        ComboBox<Priority>   priorityFilter = new ComboBox<>();
        ComboBox<TaskStatus> statusFilter   = new ComboBox<>();

        priorityFilter.getItems().add(null);
        priorityFilter.getItems().addAll(Priority.values());
        priorityFilter.setPromptText("Priority");
        priorityFilter.setPrefWidth(110);
        priorityFilter.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Priority p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty ? null : (p == null ? "All Priorities" : p.getDisplayName()));
            }
        });
        priorityFilter.setButtonCell(priorityFilter.getCellFactory().call(null));

        statusFilter.getItems().add(null);
        statusFilter.getItems().addAll(TaskStatus.values());
        statusFilter.setPromptText("Status");
        statusFilter.setPrefWidth(120);
        statusFilter.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(TaskStatus s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : (s == null ? "All Statuses" : s.getDisplayName()));
            }
        });
        statusFilter.setButtonCell(statusFilter.getCellFactory().call(null));

        // Wire filter combos to the ViewModel
        priorityFilter.valueProperty().bindBidirectional(listVm.priorityFilterProperty());
        statusFilter.valueProperty().bindBidirectional(listVm.statusFilterProperty());
        searchField.textProperty().bindBidirectional(listVm.searchTextProperty());

        Button newBtn = new Button("New Task");
        newBtn.setGraphic(new FontIcon(MaterialDesignP.PLUS));
        newBtn.getStyleClass().addAll(Styles.ACCENT);
        newBtn.setOnAction(e -> onNewTask());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox toolbar = new HBox(10,
            viewTitleLabel, spacer,
            searchField, priorityFilter, statusFilter, newBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("list-toolbar");
        toolbar.setPadding(new Insets(12, 16, 12, 16));

        // ── Task list ─────────────────────────────────────────────────────────
        taskListView.setCellFactory(lv -> new TaskCard());
        taskListView.getStyleClass().add("task-list");
        taskListView.setItems(listVm.getSortedTasks());
        taskListView.setFixedCellSize(-1);  // variable height cells

        // Empty-state placeholder
        Label emptyLabel = new Label("No tasks here yet.\nPress 'New Task' to add one.");
        emptyLabel.getStyleClass().add("empty-state-label");
        emptyLabel.setAlignment(Pos.CENTER);
        taskListView.setPlaceholder(emptyLabel);

        // ── Context menu ──────────────────────────────────────────────────────
        taskListView.setContextMenu(buildContextMenu());

        // ── Click to select / show detail ─────────────────────────────────────
        taskListView.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            listVm.selectedTaskProperty().set(nv);
            if (nv != null) {
                detailVm.loadTask(nv);
                onShowDetail.run();
            }
        });

        // ── Keyboard shortcuts ────────────────────────────────────────────────
        setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case N -> { if (e.isControlDown()) onNewTask(); }
                case F -> { if (e.isControlDown()) searchField.requestFocus(); }
                case D -> { if (e.isControlDown()) onMarkDone(); }
                case DELETE -> onArchiveSelected();
            }
        });
        setFocusTraversable(true);

        setTop(toolbar);
        setCenter(taskListView);
    }

    // ── Context menu ──────────────────────────────────────────────────────────

    private ContextMenu buildContextMenu() {
        MenuItem editItem     = new MenuItem("Edit");
        MenuItem doneItem     = new MenuItem("Mark as Done");
        MenuItem archiveItem  = new MenuItem("Archive");
        MenuItem restoreItem  = new MenuItem("Restore from Archive");
        MenuItem deleteItem   = new MenuItem("Delete…");

        editItem.setOnAction(e    -> onEditSelected());
        doneItem.setOnAction(e    -> onMarkDone());
        archiveItem.setOnAction(e -> onArchiveSelected());
        restoreItem.setOnAction(e -> onRestoreSelected());
        deleteItem.setOnAction(e  -> onDeleteSelected());

        ContextMenu menu = new ContextMenu(editItem, doneItem,
            new SeparatorMenuItem(), archiveItem, restoreItem,
            new SeparatorMenuItem(), deleteItem);

        // Show "Restore" only in archive view, hide in active view
        menu.setOnShowing(e -> {
            boolean archived = listVm.showArchivedProperty().get();
            archiveItem.setVisible(!archived);
            restoreItem.setVisible(archived);
        });
        return menu;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onNewTask() {
        detailVm.resetForNewTask();
        detailVm.setOnSaved(listVm::loadTasks);
        taskListView.getSelectionModel().clearSelection();
        onShowDetail.run();
    }

    private void onEditSelected() {
        Task selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            detailVm.loadTask(selected);
            detailVm.setOnSaved(listVm::loadTasks);
            onShowDetail.run();
        }
    }

    private void onMarkDone() {
        Task selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            listVm.markDone(selected);
            showToast("✔ Marked as done: " + selected.getTitle());
        }
    }

    private void onArchiveSelected() {
        Task selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            listVm.archiveTask(selected);
            onHideDetail.run();
            showToast("Archived: " + selected.getTitle());
        }
    }

    private void onRestoreSelected() {
        Task selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            listVm.restoreTask(selected);
            onHideDetail.run();
            showToast("Restored: " + selected.getTitle());
        }
    }

    private void onDeleteSelected() {
        Task selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Task");
        confirm.setHeaderText("Delete \"" + selected.getTitle() + "\"?");
        confirm.setContentText("This cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                listVm.deleteTask(selected);
                onHideDetail.run();
            }
        });
    }

    // ── Bind ──────────────────────────────────────────────────────────────────

    private void bindToViewModel() {
        listVm.initialize();

        // Show/hide loading indicator (Phase 2: add a ProgressIndicator overlay)
        listVm.loadingProperty().addListener((obs, old, nv) -> {
            // Placeholder for a loading overlay in future phases
        });
    }

    // ── Toast ──────────────────────────────────────────────────────────────────

    private void showToast(String message) {
        Platform.runLater(() ->
            Notifications.create()
                .title("Nexus")
                .text(message)
                .showInformation());
    }

    // ── Binding helpers ───────────────────────────────────────────────────────

    /** Bind the view's ListView selection model to allow external clearing. */
    public void clearSelection() {
        taskListView.getSelectionModel().clearSelection();
    }
}

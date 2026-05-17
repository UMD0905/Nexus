package com.nexus.ui.views;

import atlantafx.base.theme.Styles;
import com.nexus.model.Category;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.viewmodel.TaskDetailViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.time.LocalDate;

/**
 * The right-side task detail and edit panel.
 *
 * <p>Binds directly to {@link TaskDetailViewModel} properties.
 * A {@code Runnable} passed to {@link #setOnClose} hides this panel.
 */
public class TaskDetailPanel extends VBox {

    private final TaskDetailViewModel vm;
    private Runnable onClose;

    // ── Form fields ───────────────────────────────────────────────────────────
    private final TextField             titleField       = new TextField();
    private final TextArea              descArea         = new TextArea();
    private final ComboBox<Category>    categoryCombo    = new ComboBox<>();
    private final ComboBox<Priority>    priorityCombo    = new ComboBox<>();
    private final ComboBox<TaskStatus>  statusCombo      = new ComboBox<>();
    private final DatePicker            dueDatePicker    = new DatePicker();
    private final TextField             dueTimeField     = new TextField();
    private final Spinner<Integer>      estMinSpinner    = new Spinner<>(0, 480, 30, 15);
    private final CheckBox              importantCheck   = new CheckBox("Important");
    private final CheckBox              urgentCheck      = new CheckBox("Urgent");
    private final Label                 errorLabel       = new Label();
    private final Button                saveBtn          = new Button("Save Task");
    private final Button                cancelBtn        = new Button("Cancel");

    public TaskDetailPanel(TaskDetailViewModel vm) {
        this.vm = vm;
        build();
        bind();
    }

    public void setOnClose(Runnable onClose) { this.onClose = onClose; }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        getStyleClass().add("detail-panel");
        setPrefWidth(320);
        setMinWidth(280);
        setMaxWidth(380);
        setSpacing(0);
        setPadding(new Insets(0));

        // Header
        HBox header = buildHeader();

        // Scrollable form body
        VBox form = buildForm();
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("detail-scroll");
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        // Footer with buttons
        HBox footer = buildFooter();

        getChildren().addAll(header, scroll, footer);
    }

    private HBox buildHeader() {
        Label panelTitle = new Label();
        panelTitle.textProperty().bind(
            vm.isNewTaskProperty().map(isNew -> isNew ? "New Task" : "Edit Task")
        );
        panelTitle.getStyleClass().add("detail-panel-title");
        HBox.setHgrow(panelTitle, javafx.scene.layout.Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(new FontIcon(MaterialDesignC.CLOSE));
        closeBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        closeBtn.setOnAction(e -> { if (onClose != null) onClose.run(); });

        HBox header = new HBox(8, panelTitle, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("detail-panel-header");
        header.setPadding(new Insets(14, 12, 14, 16));
        return header;
    }

    private VBox buildForm() {
        VBox form = new VBox(12);
        form.setPadding(new Insets(16));

        // Title
        titleField.setPromptText("What needs to be done?");
        titleField.getStyleClass().add(Styles.LARGE);

        // Description
        descArea.setPromptText("Add details, notes, or a plan of attack…");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        // Category
        categoryCombo.setItems(vm.getCategories());
        categoryCombo.setPromptText("Life area");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Category c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getName());
            }
        });
        categoryCombo.setButtonCell(categoryCombo.getCellFactory().call(null));

        // Priority
        priorityCombo.getItems().setAll(Priority.values());
        priorityCombo.setMaxWidth(Double.MAX_VALUE);

        // Status
        statusCombo.getItems().setAll(TaskStatus.values());
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        // Due date + time in one row
        dueDatePicker.setPromptText("Due date");
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(dueDatePicker, javafx.scene.layout.Priority.ALWAYS);

        dueTimeField.setPromptText("HH:mm");
        dueTimeField.setPrefWidth(70);

        HBox dateTimeRow = new HBox(8, dueDatePicker, dueTimeField);
        dateTimeRow.setAlignment(Pos.CENTER_LEFT);

        // Estimated time
        estMinSpinner.setEditable(true);
        estMinSpinner.setPrefWidth(100);

        // Important / Urgent checkboxes (Eisenhower)
        HBox eisenRow = new HBox(16, importantCheck, urgentCheck);
        eisenRow.setAlignment(Pos.CENTER_LEFT);

        // Error label
        errorLabel.getStyleClass().add(Styles.DANGER);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        form.getChildren().addAll(
            formRow("Title",       titleField),
            formRow("Description", descArea),
            formRow("Life Area",   categoryCombo),
            formRow("Priority",    priorityCombo),
            formRow("Status",      statusCombo),
            formRow("Due Date",    dateTimeRow),
            formRow("Est. (min)",  estMinSpinner),
            eisenRow,
            errorLabel
        );
        return form;
    }

    private HBox buildFooter() {
        saveBtn.getStyleClass().addAll(Styles.ACCENT);
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(saveBtn, javafx.scene.layout.Priority.ALWAYS);
        saveBtn.setOnAction(e -> onSave());

        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> { if (onClose != null) onClose.run(); });

        HBox footer = new HBox(8, cancelBtn, saveBtn);
        footer.getStyleClass().add("detail-panel-footer");
        footer.setPadding(new Insets(12, 16, 16, 16));
        return footer;
    }

    /** Wraps a control in a labelled form row. */
    private VBox formRow(String labelText, javafx.scene.Node control) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        VBox row = new VBox(4, lbl, control);
        ((javafx.scene.Node) control).setStyle("");  // ensure no leftover inline style
        return row;
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private void bind() {
        titleField.textProperty().bindBidirectional(vm.titleProperty());
        descArea.textProperty().bindBidirectional(vm.descriptionProperty());
        categoryCombo.valueProperty().bindBidirectional(vm.categoryProperty());
        priorityCombo.valueProperty().bindBidirectional(vm.priorityProperty());
        statusCombo.valueProperty().bindBidirectional(vm.statusProperty());
        dueDatePicker.valueProperty().bindBidirectional(vm.dueDateProperty());
        dueTimeField.textProperty().bindBidirectional(vm.dueTimeProperty());
        importantCheck.selectedProperty().bindBidirectional(vm.importantProperty());
        urgentCheck.selectedProperty().bindBidirectional(vm.urgentProperty());

        // Spinner needs a manual two-way link
        estMinSpinner.getValueFactory().valueProperty().addListener((obs, o, nv) ->
            vm.estimatedMinsProperty().set(nv));
        vm.estimatedMinsProperty().addListener((obs, o, nv) -> {
            if (nv != null) estMinSpinner.getValueFactory().setValue(nv);
        });

        // Error display
        vm.errorMessageProperty().addListener((obs, o, nv) -> {
            errorLabel.setText(nv);
            errorLabel.setVisible(nv != null && !nv.isBlank());
        });
    }

    private void onSave() {
        boolean ok = vm.saveTask();
        if (ok && onClose != null) {
            onClose.run();
        }
    }
}

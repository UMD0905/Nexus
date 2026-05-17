package com.nexus.ui.components;

import com.nexus.model.Tag;
import com.nexus.model.Task;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;

import static javafx.scene.layout.Priority.ALWAYS;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Custom {@link ListCell} that renders a {@link Task} as a polished card.
 *
 * Layout:
 * <pre>
 * ┌─[priority bar]────────────────────────────────────────────┐
 * │  Title                                       [Status]     │
 * │  📅 Due date  ⏱ 90 min  [#tag]         [Category]        │
 * └────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class TaskCard extends ListCell<Task> {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("MMM d");

    // ── Card skeleton ─────────────────────────────────────────────────────────
    private final BorderPane card      = new BorderPane();
    private final VBox       body      = new VBox(5);
    private final HBox       topRow    = new HBox(8);
    private final HBox       metaRow   = new HBox(8);

    // ── Top row elements ──────────────────────────────────────────────────────
    private final Label titleLabel    = new Label();
    private final Label statusBadge   = new Label();

    // ── Meta row elements ─────────────────────────────────────────────────────
    private final Label dueDateLabel  = new Label();
    private final Label estLabel      = new Label();
    private final HBox  tagsBox       = new HBox(4);
    private final Label categoryLabel = new Label();

    public TaskCard() {
        super();
        buildLayout();
    }

    // ── ListCell contract ─────────────────────────────────────────────────────

    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        if (empty || task == null) {
            setGraphic(null);
            setText(null);
            return;
        }
        populate(task);
        setGraphic(card);
        setText(null);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void buildLayout() {
        // Title
        titleLabel.getStyleClass().add("task-card-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, ALWAYS);

        // Status badge (right of title)
        statusBadge.getStyleClass().add("task-card-status-badge");

        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(titleLabel, statusBadge);

        // Due date + estimate + tags (left) + category pill (right)
        dueDateLabel.getStyleClass().add("task-card-meta");
        estLabel.getStyleClass().add("task-card-meta");
        tagsBox.setAlignment(Pos.CENTER_LEFT);

        categoryLabel.getStyleClass().add("task-card-category");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, ALWAYS);

        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getChildren().addAll(dueDateLabel, estLabel, tagsBox, metaSpacer, categoryLabel);

        body.setPadding(new Insets(10, 14, 10, 14));
        body.getChildren().addAll(topRow, metaRow);

        card.setCenter(body);
        card.getStyleClass().add("task-card");
    }

    // ── Populate ──────────────────────────────────────────────────────────────

    private void populate(Task task) {
        applyPriorityBorder(task.getPriority());

        // Title
        titleLabel.setText(task.getTitle());
        titleLabel.setStyle(task.getStatus() == TaskStatus.DONE
            ? "-fx-strikethrough: true; -fx-text-fill: #4a5770;"
            : "-fx-strikethrough: false;");

        // Status badge
        statusBadge.setText(task.getStatus().getDisplayName());
        statusBadge.getStyleClass().removeIf(s -> s.startsWith("status-"));
        statusBadge.getStyleClass().add("status-" + task.getStatus().name().toLowerCase());

        // Due date
        boolean hasDate = task.getDueDate() != null;
        dueDateLabel.setVisible(hasDate);
        dueDateLabel.setManaged(hasDate);
        if (hasDate) {
            dueDateLabel.setText("📅 " + formatDue(task));
            dueDateLabel.getStyleClass().removeIf("overdue"::equals);
            if (task.isOverdue()) dueDateLabel.getStyleClass().add("overdue");
        }

        // Estimate
        boolean hasEst = task.getEstimatedMinutes() != null && task.getEstimatedMinutes() > 0;
        estLabel.setVisible(hasEst);
        estLabel.setManaged(hasEst);
        if (hasEst) estLabel.setText("⏱ " + task.getEstimatedMinutes() + "m");

        // Tags
        tagsBox.getChildren().clear();
        for (Tag tag : task.getTags()) {
            Label chip = new Label("#" + tag.getName());
            chip.getStyleClass().add("tag-chip");
            chip.setStyle("-fx-background-color: " + tag.getColor() + "22;");
            tagsBox.getChildren().add(chip);
        }

        // Category
        boolean hasCat = task.getCategory() != null;
        categoryLabel.setVisible(hasCat);
        categoryLabel.setManaged(hasCat);
        if (hasCat) {
            categoryLabel.setText(task.getCategory().getName());
            categoryLabel.setStyle(
                "-fx-background-color: " + task.getCategory().getColor() + "20;" +
                "-fx-border-color: " + task.getCategory().getColor() + "50;" +
                "-fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;" +
                "-fx-text-fill: " + task.getCategory().getColor() + ";"
            );
        }

        // Dim done / archived tasks
        double opacity = (task.getStatus() == TaskStatus.DONE || task.isArchived()) ? 0.55 : 1.0;
        card.setOpacity(opacity);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyPriorityBorder(Priority priority) {
        String color = priority.getColor();
        // 4-px left accent bar; thin borders on other three sides
        card.setStyle(
            "-fx-border-color: rgba(255,255,255,0.07) rgba(255,255,255,0.07) " +
            "rgba(255,255,255,0.07) " + color + ";"
        );
    }

    private String formatDue(Task task) {
        LocalDate d = task.getDueDate().toLocalDate();
        LocalDate today = LocalDate.now();
        if (d.equals(today))           return "Today";
        if (d.equals(today.plusDays(1))) return "Tomorrow";
        if (d.equals(today.minusDays(1))) return "Yesterday";
        return d.format(DATE_FMT);
    }
}

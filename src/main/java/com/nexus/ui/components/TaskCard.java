package com.nexus.ui.components;

import com.nexus.model.Tag;
import com.nexus.model.Task;
import com.nexus.model.enums.TaskStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;

/**
 * Custom {@link ListCell} that renders a {@link Task} as a polished card.
 *
 * Layout:
 * <pre>
 * ┌──────────────────────────────────────────────────────────┐
 * │  ● [Cat pill]  Task title                    [Status]    │
 * │     📅 Due date · ⏱ 90 min est.   #focus #health        │
 * └──────────────────────────────────────────────────────────┘
 * </pre>
 */
public class TaskCard extends ListCell<Task> {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private final BorderPane card     = new BorderPane();
    private final VBox       mainArea = new VBox(4);
    private final HBox       topRow   = new HBox(8);
    private final HBox       metaRow  = new HBox(8);

    private final Circle     priorityDot = new Circle(5);
    private final Label      titleLabel  = new Label();
    private final Label      statusBadge = new Label();
    private final Label      dueDateLabel = new Label();
    private final Label      estLabel    = new Label();
    private final HBox       tagsBox     = new HBox(4);

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
        // Top row: priority dot + title + status badge
        topRow.setAlignment(Pos.CENTER_LEFT);
        titleLabel.getStyleClass().add("task-card-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        statusBadge.getStyleClass().addAll("task-card-status-badge");

        topRow.getChildren().addAll(priorityDot, titleLabel, statusBadge);

        // Meta row: due date + estimated time + tags
        metaRow.setAlignment(Pos.CENTER_LEFT);
        dueDateLabel.getStyleClass().add("task-card-meta");
        estLabel.getStyleClass().add("task-card-meta");
        tagsBox.setAlignment(Pos.CENTER_LEFT);
        metaRow.getChildren().addAll(dueDateLabel, estLabel, tagsBox);

        mainArea.setPadding(new Insets(0));
        mainArea.getChildren().addAll(topRow, metaRow);

        card.setCenter(mainArea);
        card.getStyleClass().add("task-card");
        card.setPadding(new Insets(10, 14, 10, 14));
    }

    // ── Populate ──────────────────────────────────────────────────────────────

    private void populate(Task task) {
        // Priority dot colour
        priorityDot.setFill(Color.web(task.getPriority().getColor()));

        // Title — CSS strikethrough when done (Label has no setStrikethrough; use style)
        titleLabel.setText(task.getTitle());
        if (task.getStatus() == TaskStatus.DONE) {
            titleLabel.setStyle("-fx-strikethrough: true;");
        } else {
            titleLabel.setStyle("-fx-strikethrough: false;");
        }

        // Status badge
        statusBadge.setText(task.getStatus().getDisplayName());
        statusBadge.getStyleClass().removeIf(s -> s.startsWith("status-"));
        statusBadge.getStyleClass().add("status-" + task.getStatus().name().toLowerCase());

        // Category colour strip (left border via CSS class)
        card.getStyleClass().removeIf(s -> s.startsWith("cat-"));
        if (task.getCategory() != null) {
            String safeColor = task.getCategory().getColor().replace("#", "");
            card.getStyleClass().add("cat-strip");
            // Pass the actual color through a -fx-border inline style
            card.setStyle("-fx-border-color: " + task.getCategory().getColor()
                + " transparent transparent transparent; -fx-border-width: 0 0 0 3;");
        } else {
            card.setStyle("");
        }

        // Due date
        if (task.getDueDate() != null) {
            String dateText = "📅 " + task.getDueDate().format(DATE_FMT);
            dueDateLabel.setText(dateText);
            dueDateLabel.setVisible(true);
            dueDateLabel.setManaged(true);
            if (task.isOverdue()) {
                dueDateLabel.getStyleClass().add("overdue");
            } else {
                dueDateLabel.getStyleClass().remove("overdue");
            }
        } else {
            dueDateLabel.setVisible(false);
            dueDateLabel.setManaged(false);
        }

        // Estimated time
        if (task.getEstimatedMinutes() != null && task.getEstimatedMinutes() > 0) {
            estLabel.setText("⏱ " + task.getEstimatedMinutes() + " min");
            estLabel.setVisible(true);
            estLabel.setManaged(true);
        } else {
            estLabel.setVisible(false);
            estLabel.setManaged(false);
        }

        // Tags
        tagsBox.getChildren().clear();
        for (Tag tag : task.getTags()) {
            Label chip = new Label("#" + tag.getName());
            chip.getStyleClass().add("tag-chip");
            chip.setStyle("-fx-background-color: " + tag.getColor() + "33;");  // 20% opacity
            tagsBox.getChildren().add(chip);
        }

        // Dim done/archived tasks
        if (task.getStatus() == TaskStatus.DONE || task.isArchived()) {
            card.setOpacity(0.6);
        } else {
            card.setOpacity(1.0);
        }
    }
}

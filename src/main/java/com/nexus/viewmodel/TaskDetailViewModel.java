package com.nexus.viewmodel;

import com.nexus.model.Category;
import com.nexus.model.Task;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.service.CategoryService;
import com.nexus.service.TaskService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ViewModel for the task detail / edit panel.
 *
 * <p>Holds one task's field values as individual JavaFX properties so the
 * form can bind to them directly.  Calling {@link #saveTask()} assembles
 * a {@link Task} from the current property values and delegates to the
 * {@link TaskService}.
 */
public class TaskDetailViewModel {

    private final TaskService     taskService;
    private final CategoryService categoryService;

    // Notify the list to refresh after a save
    private Runnable onSaved;

    // ── Form field properties ─────────────────────────────────────────────────
    private final LongProperty               taskId        = new SimpleLongProperty(-1);
    private final StringProperty             title         = new SimpleStringProperty("");
    private final StringProperty             description   = new SimpleStringProperty("");
    private final ObjectProperty<Category>   category      = new SimpleObjectProperty<>();
    private final ObjectProperty<Priority>   priority      = new SimpleObjectProperty<>(Priority.MEDIUM);
    private final ObjectProperty<TaskStatus> status        = new SimpleObjectProperty<>(TaskStatus.TODO);
    private final ObjectProperty<LocalDate>  dueDate       = new SimpleObjectProperty<>();
    private final StringProperty             dueTime       = new SimpleStringProperty(""); // "HH:mm"
    private final ObjectProperty<Integer>    estimatedMins = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer>    reminderMins  = new SimpleObjectProperty<>();
    private final BooleanProperty            important     = new SimpleBooleanProperty(false);
    private final BooleanProperty            urgent        = new SimpleBooleanProperty(false);

    // ── UI state ──────────────────────────────────────────────────────────────
    private final BooleanProperty            isNewTask     = new SimpleBooleanProperty(true);
    private final StringProperty             errorMessage  = new SimpleStringProperty("");
    private final ObservableList<Category>   categories    = FXCollections.observableArrayList();

    public TaskDetailViewModel(TaskService taskService, CategoryService categoryService) {
        this.taskService     = taskService;
        this.categoryService = categoryService;
        loadCategories();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Populates the form fields from an existing task (edit mode). */
    public void loadTask(Task task) {
        taskId.set(task.getId());
        title.set(task.getTitle() != null ? task.getTitle() : "");
        description.set(task.getDescription() != null ? task.getDescription() : "");
        priority.set(task.getPriority());
        status.set(task.getStatus());
        important.set(task.isImportant());
        urgent.set(task.isUrgent());
        estimatedMins.set(task.getEstimatedMinutes());
        reminderMins.set(task.getReminderMinutesBefore());
        isNewTask.set(false);

        if (task.getDueDate() != null) {
            dueDate.set(task.getDueDate().toLocalDate());
            dueTime.set(task.getDueDate().toLocalTime().toString().substring(0, 5));
        } else {
            dueDate.set(null);
            dueTime.set("");
        }
        // Match category object from the loaded list
        if (task.getCategoryId() != null) {
            categories.stream()
                .filter(c -> c.getId().equals(task.getCategoryId()))
                .findFirst()
                .ifPresent(category::set);
        } else {
            category.set(null);
        }
    }

    /** Clears all fields for creating a new task. */
    public void resetForNewTask() {
        taskId.set(-1);
        title.set("");
        description.set("");
        category.set(null);
        priority.set(Priority.MEDIUM);
        status.set(TaskStatus.TODO);
        dueDate.set(null);
        dueTime.set("");
        estimatedMins.set(null);
        reminderMins.set(null);
        important.set(false);
        urgent.set(false);
        errorMessage.set("");
        isNewTask.set(true);
    }

    /**
     * Validates the form, then either creates or updates the task.
     *
     * @return {@code true} if the save succeeded, {@code false} if validation failed.
     */
    public boolean saveTask() {
        errorMessage.set("");

        if (title.get() == null || title.get().isBlank()) {
            errorMessage.set("Title is required.");
            return false;
        }

        Task task = buildTask();
        try {
            if (isNewTask.get()) {
                taskService.createTask(task);
            } else {
                taskService.updateTask(task);
            }
            if (onSaved != null) onSaved.run();
            return true;
        } catch (Exception e) {
            errorMessage.set(e.getMessage());
            return false;
        }
    }

    public void setOnSaved(Runnable callback) { this.onSaved = callback; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Task buildTask() {
        var builder = Task.builder()
            .title(title.get().trim())
            .description(description.get())
            .priority(priority.get())
            .status(status.get())
            .estimatedMinutes(estimatedMins.get())
            .reminderMinutesBefore(reminderMins.get())
            .important(important.get())
            .urgent(urgent.get());

        if (taskId.get() > 0) {
            builder.id(taskId.get());
        }
        if (category.get() != null) {
            builder.categoryId(category.get().getId());
        }
        if (dueDate.get() != null) {
            LocalTime time = parseTime(dueTime.get());
            builder.dueDate(dueDate.get().atTime(time));
        }

        return builder.build();
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return LocalTime.of(8, 0);
        try {
            return LocalTime.parse(timeStr.trim());
        } catch (Exception e) {
            return LocalTime.of(8, 0);
        }
    }

    /** Public method so MainWindow can push updated categories after create/edit/delete. */
    public void reloadCategories() {
        loadCategories();
    }

    private void loadCategories() {
        try {
            categories.setAll(categoryService.getAllCategories());
        } catch (Exception e) {
            // Non-fatal; category will just be unset
        }
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public StringProperty              titleProperty()        { return title; }
    public StringProperty              descriptionProperty()  { return description; }
    public ObjectProperty<Category>    categoryProperty()     { return category; }
    public ObjectProperty<Priority>    priorityProperty()     { return priority; }
    public ObjectProperty<TaskStatus>  statusProperty()       { return status; }
    public ObjectProperty<LocalDate>   dueDateProperty()      { return dueDate; }
    public StringProperty              dueTimeProperty()      { return dueTime; }
    public ObjectProperty<Integer>     estimatedMinsProperty(){ return estimatedMins; }
    public ObjectProperty<Integer>     reminderMinsProperty() { return reminderMins; }
    public BooleanProperty             importantProperty()    { return important; }
    public BooleanProperty             urgentProperty()       { return urgent; }
    public BooleanProperty             isNewTaskProperty()    { return isNewTask; }
    public StringProperty              errorMessageProperty() { return errorMessage; }
    public ObservableList<Category>    getCategories()        { return categories; }
}

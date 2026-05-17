package com.nexus.viewmodel;

import com.nexus.model.Category;
import com.nexus.model.Task;
import com.nexus.model.TaskFilter;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.service.CategoryService;
import com.nexus.service.TaskService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * ViewModel for the task list view.
 *
 * <p>Holds all observable state that the {@code TaskListView} binds to.
 * Business logic (creating, archiving, etc.) is delegated to
 * {@link TaskService} — the ViewModel acts as the bridge.
 *
 * <p>Filter properties drive a {@link FilteredList} in memory; a database
 * reload is triggered when the category or archive scope changes (those
 * change the SQL WHERE clause).
 */
public class TaskListViewModel {

    private static final Logger log = LoggerFactory.getLogger(TaskListViewModel.class);

    private final TaskService     taskService;
    private final CategoryService categoryService;

    // ── Observable collections ────────────────────────────────────────────────
    private final ObservableList<Task>       allTasks     = FXCollections.observableArrayList();
    private final FilteredList<Task>         filteredTasks;
    private final SortedList<Task>           sortedTasks;
    private final ObservableList<Category>   categories   = FXCollections.observableArrayList();

    // ── Filter / UI state properties ──────────────────────────────────────────
    /** Free-text search applied in-memory to the loaded task list. */
    private final StringProperty    searchText       = new SimpleStringProperty("");
    /** Currently selected category; null = all areas. */
    private final ObjectProperty<Category> selectedCategory = new SimpleObjectProperty<>(null);
    /** Currently selected status filter; null = all. */
    private final ObjectProperty<TaskStatus> statusFilter  = new SimpleObjectProperty<>(null);
    /** Currently selected priority filter; null = all. */
    private final ObjectProperty<Priority>   priorityFilter = new SimpleObjectProperty<>(null);
    /** Whether the archive view is active. */
    private final BooleanProperty   showArchived     = new SimpleBooleanProperty(false);
    /** The task currently selected in the list. */
    private final ObjectProperty<Task> selectedTask  = new SimpleObjectProperty<>(null);
    /** True while loading from DB (used to show a spinner). */
    private final BooleanProperty   loading          = new SimpleBooleanProperty(false);

    public TaskListViewModel(TaskService taskService, CategoryService categoryService) {
        this.taskService     = taskService;
        this.categoryService = categoryService;

        // In-memory filter: applied on top of whatever the DB returned
        this.filteredTasks = new FilteredList<>(allTasks, task -> true);
        this.sortedTasks   = new SortedList<>(filteredTasks,
            Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                      .thenComparing(t -> t.getPriority().ordinal(), Comparator.reverseOrder()));

        // Re-apply the in-memory filter whenever any filter property changes
        searchText.addListener((obs, old, nv)  -> applyInMemoryFilter());
        statusFilter.addListener((obs, old, nv) -> applyInMemoryFilter());
        priorityFilter.addListener((obs, old, nv) -> applyInMemoryFilter());

        // Re-load from DB when category or archive scope changes
        selectedCategory.addListener((obs, old, nv) -> loadTasks());
        showArchived.addListener((obs, old, nv)     -> loadTasks());
    }

    // ── Public API called by the View ─────────────────────────────────────────

    /** Initial load — call once from the view's initialisation. */
    public void initialize() {
        loadCategories();
        loadTasks();
    }

    /** Reloads tasks from the database with the current filter state. */
    public void loadTasks() {
        loading.set(true);
        // For Phase 1 we stay on the FX thread (H2 is local and fast).
        // Phase 2 will move this to a background thread.
        try {
            TaskFilter filter = buildFilter();
            List<Task> loaded = taskService.getTasks(filter);
            allTasks.setAll(loaded);
            applyInMemoryFilter();
            log.debug("Loaded {} task(s) from DB", loaded.size());
        } catch (Exception e) {
            log.error("Failed to load tasks", e);
        } finally {
            loading.set(false);
        }
    }

    /** Creates a task and refreshes the list. */
    public Task createTask(Task task) {
        Task saved = taskService.createTask(task);
        loadTasks();
        return saved;
    }

    /** Updates a task and refreshes the list. */
    public Task updateTask(Task task) {
        Task updated = taskService.updateTask(task);
        loadTasks();
        return updated;
    }

    /** Marks the given task as done and refreshes. */
    public void markDone(Task task) {
        taskService.markDone(task.getId());
        loadTasks();
    }

    /** Archives the given task and refreshes. */
    public void archiveTask(Task task) {
        taskService.archiveTask(task.getId());
        selectedTask.set(null);
        loadTasks();
    }

    /** Restores an archived task back to active. */
    public void restoreTask(Task task) {
        taskService.restoreTask(task.getId());
        selectedTask.set(null);
        loadTasks();
    }

    /** Bulk-archives all completed tasks. */
    public int archiveAllCompleted() {
        int count = taskService.archiveAllCompleted();
        loadTasks();
        return count;
    }

    /** Deletes a task permanently. */
    public void deleteTask(Task task) {
        taskService.deleteTask(task.getId());
        selectedTask.set(null);
        loadTasks();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private TaskFilter buildFilter() {
        TaskFilter.TaskFilterBuilder b = TaskFilter.builder()
            .showArchived(showArchived.get());
        if (selectedCategory.get() != null) {
            b.categoryId(selectedCategory.get().getId());
        }
        return b.build();
    }

    /**
     * Applies the in-memory predicate (search text + status + priority).
     * Does NOT hit the database.
     */
    private void applyInMemoryFilter() {
        filteredTasks.setPredicate(task -> {
            String search = searchText.get();
            if (search != null && !search.isBlank()) {
                if (!task.getTitle().toLowerCase().contains(search.toLowerCase().trim())) {
                    return false;
                }
            }
            TaskStatus statusF = statusFilter.get();
            if (statusF != null && task.getStatus() != statusF) {
                return false;
            }
            Priority prioF = priorityFilter.get();
            if (prioF != null && task.getPriority() != prioF) {
                return false;
            }
            return true;
        });
    }

    /** Public method so MainWindow can push updated categories after create/edit/delete. */
    public void reloadCategories() {
        loadCategories();
    }

    private void loadCategories() {
        try {
            categories.setAll(categoryService.getAllCategories());
        } catch (Exception e) {
            log.error("Failed to load categories", e);
        }
    }

    // ── Property accessors ────────────────────────────────────────────────────

    /** The list the table/list view should bind to. */
    public SortedList<Task>                getSortedTasks()       { return sortedTasks; }
    public ObservableList<Category>        getCategories()        { return categories; }
    public StringProperty                  searchTextProperty()   { return searchText; }
    public ObjectProperty<Category>        selectedCategoryProperty() { return selectedCategory; }
    public ObjectProperty<TaskStatus>      statusFilterProperty() { return statusFilter; }
    public ObjectProperty<Priority>        priorityFilterProperty(){ return priorityFilter; }
    public BooleanProperty                 showArchivedProperty() { return showArchived; }
    public ObjectProperty<Task>            selectedTaskProperty() { return selectedTask; }
    public BooleanProperty                 loadingProperty()      { return loading; }
}

package com.nexus.viewmodel;

import com.nexus.model.Task;
import com.nexus.model.TaskFilter;
import com.nexus.service.TaskService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ViewModel for the Eisenhower matrix view.
 *
 * <p>Splits the active (non-archived) task list into four quadrants:
 * <pre>
 *  Urgent + Important  → Do First (Q1)
 *  !Urgent + Important → Schedule (Q2)
 *  Urgent + !Important → Delegate (Q3)
 *  !Urgent + !Important→ Eliminate (Q4)
 * </pre>
 * Moving a task between quadrants updates its {@code urgent} and {@code important} flags.
 */
public class EisenhowerViewModel {

    private static final Logger log = LoggerFactory.getLogger(EisenhowerViewModel.class);

    private final TaskService taskService;

    private final ObservableList<Task> doFirst   = FXCollections.observableArrayList();
    private final ObservableList<Task> schedule  = FXCollections.observableArrayList();
    private final ObservableList<Task> delegate  = FXCollections.observableArrayList();
    private final ObservableList<Task> eliminate = FXCollections.observableArrayList();

    public EisenhowerViewModel(TaskService taskService) {
        this.taskService = taskService;
    }

    public void initialize() { load(); }

    public void reload() { load(); }

    /**
     * Moves a task into a target quadrant by updating its urgent/important flags.
     *
     * @param task     the task to move
     * @param urgent   new urgent flag
     * @param important new important flag
     */
    public void moveTask(Task task, boolean urgent, boolean important) {
        task.setUrgent(urgent);
        task.setImportant(important);
        taskService.updateTask(task);
        load();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void load() {
        try {
            List<Task> tasks = taskService.getTasks(TaskFilter.builder().showArchived(false).build());

            doFirst.setAll(tasks.stream()
                .filter(t ->  t.isUrgent() &&  t.isImportant()).toList());
            schedule.setAll(tasks.stream()
                .filter(t -> !t.isUrgent() &&  t.isImportant()).toList());
            delegate.setAll(tasks.stream()
                .filter(t ->  t.isUrgent() && !t.isImportant()).toList());
            eliminate.setAll(tasks.stream()
                .filter(t -> !t.isUrgent() && !t.isImportant()).toList());

            log.debug("Eisenhower: Q1={} Q2={} Q3={} Q4={}",
                doFirst.size(), schedule.size(), delegate.size(), eliminate.size());
        } catch (Exception e) {
            log.error("Failed to load Eisenhower matrix", e);
        }
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public ObservableList<Task> getDoFirst()   { return doFirst; }
    public ObservableList<Task> getSchedule()  { return schedule; }
    public ObservableList<Task> getDelegate()  { return delegate; }
    public ObservableList<Task> getEliminate() { return eliminate; }
}

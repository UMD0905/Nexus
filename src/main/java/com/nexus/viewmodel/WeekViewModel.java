package com.nexus.viewmodel;

import com.nexus.model.Task;
import com.nexus.service.TaskService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ViewModel for the Week view.
 *
 * <p>Loads all tasks with a due date in the current ISO week (Mon–Sun).
 * Navigate with {@link #prevWeek()} / {@link #nextWeek()}.
 */
public class WeekViewModel {

    private static final Logger log = LoggerFactory.getLogger(WeekViewModel.class);

    private final TaskService taskService;

    /** Monday of the week currently displayed. */
    private final ObjectProperty<LocalDate> weekStart =
        new SimpleObjectProperty<>(mondayOf(LocalDate.now()));

    private final ObservableList<Task> weekTasks = FXCollections.observableArrayList();

    public WeekViewModel(TaskService taskService) {
        this.taskService = taskService;
        weekStart.addListener((obs, old, nv) -> load());
    }

    public void initialize() { load(); }

    public void prevWeek() { weekStart.set(weekStart.get().minusWeeks(1)); }
    public void nextWeek() { weekStart.set(weekStart.get().plusWeeks(1)); }
    public void goToCurrentWeek() { weekStart.set(mondayOf(LocalDate.now())); }

    public void reload() { load(); }

    /** Returns tasks for a specific day within the loaded week. */
    public List<Task> getTasksForDay(LocalDate date) {
        return weekTasks.stream()
            .filter(t -> t.getDueDate() != null
                      && t.getDueDate().toLocalDate().equals(date))
            .toList();
    }

    /** Returns all 7 dates of the current week (Mon → Sun). */
    public List<LocalDate> getWeekDays() {
        LocalDate monday = weekStart.get();
        return List.of(
            monday,
            monday.plusDays(1),
            monday.plusDays(2),
            monday.plusDays(3),
            monday.plusDays(4),
            monday.plusDays(5),
            monday.plusDays(6)
        );
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void load() {
        try {
            List<Task> tasks = taskService.getTasksDueThisWeek(weekStart.get());
            weekTasks.setAll(tasks);
            log.debug("Week view loaded: {} tasks for week of {}", tasks.size(), weekStart.get());
        } catch (Exception e) {
            log.error("Failed to load week tasks", e);
        }
    }

    private static LocalDate mondayOf(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public ObjectProperty<LocalDate> weekStartProperty() { return weekStart; }
    public ObservableList<Task>      getWeekTasks()      { return weekTasks; }
}

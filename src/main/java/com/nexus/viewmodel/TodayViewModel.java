package com.nexus.viewmodel;

import com.nexus.model.Task;
import com.nexus.model.TimeBlock;
import com.nexus.service.TaskService;
import com.nexus.service.TimeBlockService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * ViewModel for the Today view.
 *
 * <p>Exposes:
 * <ul>
 *   <li>{@link #getTimeBlocks()} — scheduled time blocks for the current day</li>
 *   <li>{@link #getBacklogTasks()} — tasks due today without a time block</li>
 *   <li>{@link #currentDateProperty()} — the date being viewed (navigate with arrows)</li>
 * </ul>
 */
public class TodayViewModel {

    private static final Logger log = LoggerFactory.getLogger(TodayViewModel.class);

    private final TaskService      taskService;
    private final TimeBlockService timeBlockService;

    private final ObjectProperty<LocalDate>  currentDate  = new SimpleObjectProperty<>(LocalDate.now());
    private final ObservableList<TimeBlock>  timeBlocks   = FXCollections.observableArrayList();
    private final ObservableList<Task>       backlogTasks = FXCollections.observableArrayList();

    public TodayViewModel(TaskService taskService, TimeBlockService timeBlockService) {
        this.taskService      = taskService;
        this.timeBlockService = timeBlockService;

        // Reload whenever the viewed date changes
        currentDate.addListener((obs, old, nv) -> load());
    }

    public void initialize() { load(); }

    public void navigatePrevDay() { currentDate.set(currentDate.get().minusDays(1)); }
    public void navigateNextDay() { currentDate.set(currentDate.get().plusDays(1)); }
    public void goToToday()       { currentDate.set(LocalDate.now()); }

    /** Adds a new time block and refreshes. */
    public void addTimeBlock(TimeBlock block) {
        block.setBlockDate(currentDate.get());
        timeBlockService.createBlock(block);
        load();
    }

    /** Deletes a time block and refreshes. */
    public void deleteTimeBlock(TimeBlock block) {
        timeBlockService.deleteBlock(block.getId());
        load();
    }

    public void reload() { load(); }

    // ── Private ───────────────────────────────────────────────────────────────

    private void load() {
        LocalDate date = currentDate.get();
        try {
            // Time blocks for the day
            List<TimeBlock> blocks = timeBlockService.getBlocksForDate(date);
            timeBlocks.setAll(blocks);

            // Backlog: tasks due today (or overdue) not linked to a time block
            List<Long> scheduledTaskIds = blocks.stream()
                .filter(b -> b.getTaskId() != null)
                .map(TimeBlock::getTaskId)
                .toList();

            List<Task> dueTasks = taskService.getTasksDueToday();
            List<Task> backlog  = dueTasks.stream()
                .filter(t -> !scheduledTaskIds.contains(t.getId()))
                .toList();
            backlogTasks.setAll(backlog);

            log.debug("Today view loaded: {} blocks, {} backlog tasks for {}",
                blocks.size(), backlog.size(), date);
        } catch (Exception e) {
            log.error("Failed to load today view for {}", date, e);
        }
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public ObjectProperty<LocalDate> currentDateProperty() { return currentDate; }
    public ObservableList<TimeBlock> getTimeBlocks()       { return timeBlocks; }
    public ObservableList<Task>      getBacklogTasks()     { return backlogTasks; }
}

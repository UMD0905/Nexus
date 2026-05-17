package com.nexus.viewmodel;

import com.nexus.model.Category;
import com.nexus.model.Streak;
import com.nexus.model.Task;
import com.nexus.model.TaskFilter;
import com.nexus.model.enums.TaskStatus;
import com.nexus.service.PomodoroService;
import com.nexus.service.StreakService;
import com.nexus.service.TaskService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ViewModel for the Dashboard view.
 *
 * <p>Pre-computes all stats in {@link #refresh()} — called once per view activation.
 */
public class DashboardViewModel {

    private static final Logger log = LoggerFactory.getLogger(DashboardViewModel.class);

    private final TaskService     taskService;
    private final PomodoroService pomodoroService;
    private final StreakService    streakService;

    // ── Stats ─────────────────────────────────────────────────────────────────
    private final IntegerProperty totalActive    = new SimpleIntegerProperty(0);
    private final IntegerProperty dueToday       = new SimpleIntegerProperty(0);
    private final IntegerProperty completedWeek  = new SimpleIntegerProperty(0);
    private final IntegerProperty pomodoroToday  = new SimpleIntegerProperty(0);
    private final IntegerProperty overdueTasks   = new SimpleIntegerProperty(0);
    private final IntegerProperty activeGoals    = new SimpleIntegerProperty(0);

    /** Daily completed-task counts for Mon–Sun of the current week. */
    private final ObservableList<int[]> weeklyCompletions = FXCollections.observableArrayList();
    /** Map<categoryName, taskCount> for the pie chart. */
    private final Map<String, Integer>  categoryBreakdown = new LinkedHashMap<>();
    /** All active streaks. */
    private final ObservableList<Streak> streaks          = FXCollections.observableArrayList();

    public DashboardViewModel(TaskService taskService,
                               PomodoroService pomodoroService,
                               StreakService streakService) {
        this.taskService     = taskService;
        this.pomodoroService = pomodoroService;
        this.streakService   = streakService;
    }

    public void refresh() {
        try {
            List<Task> allActive = taskService.getTasks(TaskFilter.builder().showArchived(false).build());

            // Basic counts
            totalActive.set(allActive.size());
            dueToday.set((int) allActive.stream().filter(Task::isDueToday).count());
            overdueTasks.set((int) allActive.stream().filter(Task::isOverdue).count());

            // Completed this week (check archived tasks completed this week)
            List<Task> archived = taskService.getTasks(TaskFilter.builder().showArchived(true).build());
            LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
            completedWeek.set((int) archived.stream()
                .filter(t -> t.getCompletedAt() != null
                          && !t.getCompletedAt().toLocalDate().isBefore(monday))
                .count());

            // Pomodoro sessions today
            pomodoroToday.set(pomodoroService.getTodaySessions().size());

            // Weekly completions: [day0=Mon … day6=Sun]
            int[] daily = new int[7];
            for (Task t : archived) {
                if (t.getCompletedAt() == null) continue;
                LocalDate d = t.getCompletedAt().toLocalDate();
                if (!d.isBefore(monday) && !d.isAfter(monday.plusDays(6))) {
                    int dayIdx = d.getDayOfWeek().getValue() - 1; // Mon=0
                    daily[dayIdx]++;
                }
            }
            weeklyCompletions.setAll(new int[][]{daily});

            // Category breakdown for active tasks
            categoryBreakdown.clear();
            Map<String, Long> catMap = allActive.stream()
                .collect(Collectors.groupingBy(t ->
                    t.getCategory() != null ? t.getCategory().getName() : "Uncategorised",
                    Collectors.counting()));
            catMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> categoryBreakdown.put(e.getKey(), e.getValue().intValue()));

            // Streaks
            streaks.setAll(streakService.getAllStreaks());

            log.debug("Dashboard refreshed: active={} dueToday={} streak count={}",
                totalActive.get(), dueToday.get(), streaks.size());
        } catch (Exception e) {
            log.error("Dashboard refresh failed", e);
        }
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public IntegerProperty            totalActiveProperty()   { return totalActive; }
    public IntegerProperty            dueTodayProperty()      { return dueToday; }
    public IntegerProperty            completedWeekProperty() { return completedWeek; }
    public IntegerProperty            pomodoroTodayProperty() { return pomodoroToday; }
    public IntegerProperty            overdueTasksProperty()  { return overdueTasks; }
    public ObservableList<int[]>      getWeeklyCompletions()  { return weeklyCompletions; }
    public Map<String, Integer>       getCategoryBreakdown()  { return categoryBreakdown; }
    public ObservableList<Streak>     getStreaks()            { return streaks; }
}

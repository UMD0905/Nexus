package com.nexus.viewmodel;

import com.nexus.model.PomodoroSession;
import com.nexus.model.Task;
import com.nexus.service.PomodoroService;
import com.nexus.service.TaskService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

/**
 * ViewModel for the Pomodoro timer view.
 *
 * <p>Uses a JavaFX {@link Timeline} that ticks every second.
 * State machine: IDLE → WORKING → BREAK → IDLE
 *
 * <p>Session flow:
 * <ol>
 *   <li>User picks a task and clicks Start.</li>
 *   <li>{@link PomodoroService#startSession} is called; a DB record is created.</li>
 *   <li>Timer counts down {@code workMinutes} × 60 seconds.</li>
 *   <li>On completion, {@link PomodoroService#completeSession} is called and a break starts.</li>
 *   <li>After the break the timer returns to IDLE.</li>
 * </ol>
 */
public class PomodoroViewModel {

    // ── Timer state enum ──────────────────────────────────────────────────────
    public enum TimerState { IDLE, WORKING, BREAK }

    // ── Defaults ──────────────────────────────────────────────────────────────
    public static final int DEFAULT_WORK_MINUTES  = 25;
    public static final int DEFAULT_BREAK_MINUTES = 5;

    private final PomodoroService pomodoroService;
    private final TaskService     taskService;

    // ── Timer ─────────────────────────────────────────────────────────────────
    private Timeline               timeline;
    private PomodoroSession        activeSession;

    // ── Observable state ──────────────────────────────────────────────────────
    private final IntegerProperty           remainingSeconds = new SimpleIntegerProperty(DEFAULT_WORK_MINUTES * 60);
    private final IntegerProperty           totalSeconds     = new SimpleIntegerProperty(DEFAULT_WORK_MINUTES * 60);
    private final ObjectProperty<TimerState> timerState      = new SimpleObjectProperty<>(TimerState.IDLE);
    private final ObjectProperty<Task>       selectedTask    = new SimpleObjectProperty<>();
    private final IntegerProperty           completedToday   = new SimpleIntegerProperty(0);
    private final IntegerProperty           workMinutes      = new SimpleIntegerProperty(DEFAULT_WORK_MINUTES);
    private final IntegerProperty           breakMinutes     = new SimpleIntegerProperty(DEFAULT_BREAK_MINUTES);
    private final StringProperty            statusMessage    = new SimpleStringProperty("Select a task and press Start");
    private final ObservableList<Task>      activeTasks      = FXCollections.observableArrayList();
    private final ObservableList<PomodoroSession> todaySessions = FXCollections.observableArrayList();

    public PomodoroViewModel(PomodoroService pomodoroService, TaskService taskService) {
        this.pomodoroService = pomodoroService;
        this.taskService     = taskService;
    }

    public void initialize() {
        loadActiveTasks();
        refreshTodaySessions();
    }

    // ── Timer controls ────────────────────────────────────────────────────────

    public void startWork() {
        if (selectedTask.get() == null) {
            statusMessage.set("Please select a task first.");
            return;
        }
        stopTimeline();

        int secs = workMinutes.get() * 60;
        totalSeconds.set(secs);
        remainingSeconds.set(secs);
        timerState.set(TimerState.WORKING);
        statusMessage.set("Focus: " + selectedTask.get().getTitle());

        activeSession = pomodoroService.startSession(selectedTask.get(), workMinutes.get());

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickWork()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void pauseResume() {
        if (timeline == null) return;
        if (timeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            timeline.pause();
            statusMessage.set("Paused");
        } else {
            timeline.play();
            statusMessage.set(timerState.get() == TimerState.WORKING
                ? "Focus: " + (selectedTask.get() != null ? selectedTask.get().getTitle() : "")
                : "Break time");
        }
    }

    public void stop() {
        stopTimeline();
        if (activeSession != null) {
            pomodoroService.abandonSession(activeSession);
            activeSession = null;
        }
        resetToIdle();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void tickWork() {
        int remaining = remainingSeconds.get() - 1;
        if (remaining <= 0) {
            remainingSeconds.set(0);
            onWorkComplete();
        } else {
            remainingSeconds.set(remaining);
        }
    }

    private void onWorkComplete() {
        stopTimeline();
        if (activeSession != null) {
            pomodoroService.completeSession(activeSession);
            activeSession = null;
        }
        completedToday.set(completedToday.get() + 1);
        refreshTodaySessions();
        startBreak();
    }

    private void startBreak() {
        int secs = breakMinutes.get() * 60;
        totalSeconds.set(secs);
        remainingSeconds.set(secs);
        timerState.set(TimerState.BREAK);
        statusMessage.set("Break time! Great work.");

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickBreak()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void tickBreak() {
        int remaining = remainingSeconds.get() - 1;
        if (remaining <= 0) {
            remainingSeconds.set(0);
            stopTimeline();
            resetToIdle();
            statusMessage.set("Break over — ready for another session!");
        } else {
            remainingSeconds.set(remaining);
        }
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void resetToIdle() {
        timerState.set(TimerState.IDLE);
        int secs = workMinutes.get() * 60;
        totalSeconds.set(secs);
        remainingSeconds.set(secs);
    }

    private void loadActiveTasks() {
        try {
            activeTasks.setAll(taskService.getTasks(
                com.nexus.model.TaskFilter.builder().showArchived(false).build()
            ).stream()
                .filter(t -> t.getStatus() != com.nexus.model.enums.TaskStatus.DONE
                          && t.getStatus() != com.nexus.model.enums.TaskStatus.CANCELLED)
                .toList());
        } catch (Exception e) {
            // non-fatal
        }
    }

    private void refreshTodaySessions() {
        try {
            todaySessions.setAll(pomodoroService.getTodaySessions());
        } catch (Exception e) {
            // non-fatal
        }
    }

    // ── Derived helpers ───────────────────────────────────────────────────────

    /** Progress 0.0 → 1.0 for the circular ring. */
    public double getProgress() {
        int total = totalSeconds.get();
        if (total <= 0) return 0.0;
        return 1.0 - ((double) remainingSeconds.get() / total);
    }

    /** "MM:SS" formatted countdown string. */
    public String getFormattedTime() {
        int secs = remainingSeconds.get();
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public IntegerProperty            remainingSecondsProperty() { return remainingSeconds; }
    public IntegerProperty            totalSecondsProperty()     { return totalSeconds; }
    public ObjectProperty<TimerState> timerStateProperty()       { return timerState; }
    public ObjectProperty<Task>       selectedTaskProperty()     { return selectedTask; }
    public IntegerProperty            completedTodayProperty()   { return completedToday; }
    public IntegerProperty            workMinutesProperty()      { return workMinutes; }
    public IntegerProperty            breakMinutesProperty()     { return breakMinutes; }
    public StringProperty             statusMessageProperty()    { return statusMessage; }
    public ObservableList<Task>       getActiveTasks()           { return activeTasks; }
    public ObservableList<PomodoroSession> getTodaySessions()    { return todaySessions; }
}

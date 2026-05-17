package com.nexus.service;

import com.nexus.model.PomodoroSession;
import com.nexus.model.Task;
import com.nexus.repository.PomodoroSessionRepository;
import com.nexus.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages Pomodoro work sessions.
 *
 * <p>The actual countdown timer lives in {@code PomodoroViewModel} (JavaFX Timeline).
 * This service is responsible for persisting sessions and updating the task's
 * {@code actualMinutes} counter when a session completes.
 */
public class PomodoroService {

    private static final Logger log = LoggerFactory.getLogger(PomodoroService.class);

    private final PomodoroSessionRepository sessionRepo;
    private final TaskRepository            taskRepo;

    public PomodoroService(PomodoroSessionRepository sessionRepo, TaskRepository taskRepo) {
        this.sessionRepo = sessionRepo;
        this.taskRepo    = taskRepo;
    }

    /**
     * Starts a new session for the given task.
     * Persists an in-progress record immediately so it survives an app crash.
     */
    public PomodoroSession startSession(Task task, int durationMinutes) {
        PomodoroSession session = PomodoroSession.builder()
            .taskId(task.getId())
            .startedAt(LocalDateTime.now())
            .durationMinutes(durationMinutes)
            .completed(false)
            .build();
        sessionRepo.save(session);
        log.info("Pomodoro started: task='{}' duration={}m id={}", task.getTitle(), durationMinutes, session.getId());
        return session;
    }

    /**
     * Marks a session as successfully completed and adds its duration to the task's
     * {@code actual_minutes}.
     */
    public void completeSession(PomodoroSession session) {
        LocalDateTime now = LocalDateTime.now();
        sessionRepo.markCompleted(session.getId(), now);
        session.setEndedAt(now);
        session.setCompleted(true);

        // Update task actual minutes
        taskRepo.findById(session.getTaskId()).ifPresent(task -> {
            int totalMinutes = task.getActualMinutes() + session.getDurationMinutes();
            task.setActualMinutes(totalMinutes);
            taskRepo.update(task);
            log.debug("Updated task id={} actual_minutes={}", task.getId(), totalMinutes);
        });

        log.info("Pomodoro completed: session id={} taskId={}", session.getId(), session.getTaskId());
    }

    /** Abandons an in-progress session without crediting time to the task. */
    public void abandonSession(PomodoroSession session) {
        // Mark with ended_at but completed=false (already stored that way)
        sessionRepo.markCompleted(session.getId(), LocalDateTime.now());
        log.info("Pomodoro abandoned: session id={}", session.getId());
    }

    public List<PomodoroSession> getSessionsForTask(long taskId) {
        return sessionRepo.findByTaskId(taskId);
    }

    public List<PomodoroSession> getTodaySessions() {
        return sessionRepo.findToday();
    }
}

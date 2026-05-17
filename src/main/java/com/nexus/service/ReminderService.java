package com.nexus.service;

import com.nexus.model.Task;
import com.nexus.model.TaskFilter;
import com.nexus.repository.TaskRepository;
import javafx.application.Platform;
import org.controlsfx.control.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background service that fires task reminders.
 *
 * <p>A single-threaded scheduler checks every 60 seconds for tasks whose
 * {@code reminder_minutes_before} window is about to trigger.
 * Already-reminded task IDs are kept in-memory so each task fires at most once
 * per app session.
 *
 * <p>Each reminder shows:
 * <ol>
 *   <li>A ControlsFX OS toast notification</li>
 *   <li>An {@link AppNotification} record in the database (via {@link NotificationService})</li>
 * </ol>
 *
 * <p>Call {@link #start()} once on app startup and {@link #shutdown()} in
 * {@code Application.stop()}.
 */
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final TaskRepository      taskRepo;
    private final NotificationService notificationService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "reminder-scheduler");
        t.setDaemon(true);
        return t;
    });

    /** Task IDs that have already been reminded in this session. */
    private final Set<Long> reminded = new HashSet<>();

    /** Optional JavaFX callback so the bell badge can refresh. */
    private Runnable onNotificationCreated;

    public ReminderService(TaskRepository taskRepo, NotificationService notificationService) {
        this.taskRepo            = taskRepo;
        this.notificationService = notificationService;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 60, TimeUnit.SECONDS);
        log.info("Reminder service started (checking every 60 s).");
    }

    public void shutdown() {
        scheduler.shutdownNow();
        log.info("Reminder service stopped.");
    }

    public void setOnNotificationCreated(Runnable callback) {
        this.onNotificationCreated = callback;
    }

    // ── Check logic ───────────────────────────────────────────────────────────

    private void checkReminders() {
        try {
            List<Task> tasks = taskRepo.findAll(TaskFilter.builder().showArchived(false).build());
            LocalDateTime now = LocalDateTime.now();

            for (Task task : tasks) {
                if (task.getReminderMinutesBefore() == null) continue;
                if (task.getDueDate() == null) continue;
                if (reminded.contains(task.getId())) continue;

                LocalDateTime triggerAt = task.getDueDate()
                    .minusMinutes(task.getReminderMinutesBefore());

                // Fire if we are within a 90-second window of the trigger time
                boolean shouldFire = !now.isBefore(triggerAt)
                    && now.isBefore(triggerAt.plusSeconds(90));

                if (shouldFire) {
                    reminded.add(task.getId());
                    fireReminder(task);
                }
            }
        } catch (Exception e) {
            log.error("Reminder check failed", e);
        }
    }

    private void fireReminder(Task task) {
        String title   = "Reminder: " + task.getTitle();
        String message = "Due at " + (task.getDueDate() != null
            ? task.getDueDate().toLocalTime().toString().substring(0, 5)
            : "soon");

        // In-app notification record
        notificationService.createReminder(title, message, task.getId());

        // OS toast — must run on the JavaFX thread
        Platform.runLater(() -> {
            try {
                Notifications.create()
                    .title(title)
                    .text(message)
                    .showInformation();
            } catch (Exception e) {
                log.warn("OS notification failed (display may be unavailable): {}", e.getMessage());
            }

            if (onNotificationCreated != null) {
                onNotificationCreated.run();
            }
        });

        log.info("Fired reminder for task id={} '{}'", task.getId(), task.getTitle());
    }
}

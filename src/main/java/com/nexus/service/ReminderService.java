package com.nexus.service;

import com.nexus.model.Task;
import com.nexus.repository.NotificationRepository;
import com.nexus.repository.TaskRepository;
import javafx.application.Platform;
import org.controlsfx.control.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
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
 *
 * <p>Deduplication uses an in-memory {@code Set<Long>} of already-reminded task IDs,
 * reset at midnight.  This avoids a DB query per task per minute.
 *
 * <p>On startup, a one-shot catchup sweep fires any reminder whose trigger window
 * fell in the last 24 hours while the app was closed.
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

    private final TaskRepository         taskRepo;
    private final NotificationService    notificationService;
    private final NotificationRepository notificationRepo;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "reminder-scheduler");
        t.setDaemon(true);
        return t;
    });

    /** Task IDs reminded today — cleared at midnight rollover. Accessed only from scheduler thread. */
    private final Set<Long> remindedToday = new HashSet<>();
    private LocalDate       remindedDate  = LocalDate.now();

    /** Optional JavaFX callback so the bell badge can refresh. */
    private Runnable onNotificationCreated;

    public ReminderService(TaskRepository taskRepo, NotificationService notificationService,
                           NotificationRepository notificationRepo) {
        this.taskRepo            = taskRepo;
        this.notificationService = notificationService;
        this.notificationRepo    = notificationRepo;
    }

    public void start() {
        // Catchup sweep first so missed reminders fire before the first regular tick.
        scheduler.submit(this::catchupReminders);
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 60, TimeUnit.SECONDS);
        log.info("Reminder service started (checking every 60 s, catchup submitted).");
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
            LocalDateTime now  = LocalDateTime.now();
            LocalDate     today = now.toLocalDate();

            // Clear set on midnight rollover
            if (!today.equals(remindedDate)) {
                remindedToday.clear();
                remindedDate = today;
            }

            List<Task> tasks = taskRepo.findWithActiveReminders();
            for (Task task : tasks) {
                if (task.getReminderMinutesBefore() == null || task.getDueDate() == null) continue;
                // Skip snoozed tasks
                if (task.getSnoozedUntil() != null && now.isBefore(task.getSnoozedUntil())) continue;

                LocalDateTime triggerAt = task.getDueDate()
                    .minusMinutes(task.getReminderMinutesBefore());

                boolean inWindow = !now.isBefore(triggerAt) && now.isBefore(triggerAt.plusSeconds(90));
                if (inWindow && !remindedToday.contains(task.getId())) {
                    fireReminder(task);
                }
            }
        } catch (Exception e) {
            log.error("Reminder check failed", e);
        }
    }

    /**
     * One-shot sweep on startup: fires reminders for any task whose trigger window
     * fell in the last 24 hours while the app was closed.
     */
    private void catchupReminders() {
        try {
            LocalDateTime now    = LocalDateTime.now();
            LocalDateTime cutoff = now.minusHours(24);

            List<Task> tasks = taskRepo.findWithActiveReminders();
            int caught = 0;
            for (Task task : tasks) {
                if (task.getReminderMinutesBefore() == null || task.getDueDate() == null) continue;
                // Skip snoozed tasks
                if (task.getSnoozedUntil() != null && now.isBefore(task.getSnoozedUntil())) continue;

                LocalDateTime triggerAt = task.getDueDate()
                    .minusMinutes(task.getReminderMinutesBefore());

                // Trigger was in [now-24h, now) and not yet reminded this session
                boolean missed = !triggerAt.isBefore(cutoff) && triggerAt.isBefore(now);
                if (missed && !remindedToday.contains(task.getId())) {
                    fireReminder(task);
                    caught++;
                }
            }
            if (caught > 0) {
                log.info("Catchup: fired {} missed reminder(s) from the last 24 hours", caught);
            }
        } catch (Exception e) {
            log.error("Catchup reminder sweep failed", e);
        }
    }

    private void fireReminder(Task task) {
        remindedToday.add(task.getId());   // mark before firing to prevent double-fire if slow

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

package com.nexus.model;

import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The core domain object.  A task represents any unit of work —
 * from "write landing page copy" to "Tuesday kickboxing session".
 *
 * <p>The {@code isImportant} / {@code isUrgent} flags drive the
 * Eisenhower matrix view (Phase 2).
 *
 * <p>Archive lifecycle:
 * <ol>
 *   <li>Status flips to {@code DONE} → {@code completedAt} is stamped.</li>
 *   <li>User archives the task → {@code archived = true}, {@code archivedAt} stamped.</li>
 *   <li>Archived tasks vanish from all active views; the Archive view shows them
 *       grouped by completion week.</li>
 * </ol>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private Long          id;
    private String        title;
    private String        description;
    private Long          categoryId;
    private Long          projectId;

    @Builder.Default
    private Priority      priority  = Priority.MEDIUM;
    @Builder.Default
    private TaskStatus    status    = TaskStatus.TODO;

    private LocalDateTime dueDate;
    private java.time.LocalTime startTime;   // when the task is planned to begin
    private Integer       estimatedMinutes;
    @Builder.Default
    private int           actualMinutes = 0;  // accumulated from Pomodoro sessions

    private Long          recurrenceRuleId;
    private Long          parentTaskId;
    private Integer       reminderMinutesBefore;

    private boolean       important;  // Eisenhower: important axis
    private boolean       urgent;     // Eisenhower: urgent axis

    // ── Defer / GTD ──────────────────────────────────────────────
    /** Task is hidden from main views until this time passes. */
    private LocalDateTime deferUntil;
    /**
     * GTD lifecycle bucket: INBOX | ANYTIME | TODAY | SOMEDAY.
     * Quick-add lands tasks in INBOX; default for normal creation is ANYTIME.
     */
    @Builder.Default
    private String        lifecycle = "ANYTIME";

    // ── Snooze ────────────────────────────────────────────────────
    /** When set, reminders for this task are suppressed until this time. */
    private LocalDateTime snoozedUntil;

    // ── Archive ───────────────────────────────────────────────────
    private LocalDateTime completedAt;
    private boolean       archived;
    private LocalDateTime archivedAt;

    // ── Audit ─────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Enriched (loaded on demand by service layer) ──────────────
    @ToString.Exclude
    private Category      category;
    @ToString.Exclude
    @Builder.Default
    private List<Category> categories = new ArrayList<>();  // all assigned life areas
    @ToString.Exclude
    @Builder.Default
    private List<Tag>     tags     = new ArrayList<>();
    @ToString.Exclude
    @Builder.Default
    private List<Subtask> subtasks = new ArrayList<>();

    // ── Convenience helpers ────────────────────────────────────────

    /** True when the task has a non-null due date that is in the past and not yet done. */
    public boolean isOverdue() {
        return dueDate != null
            && dueDate.isBefore(LocalDateTime.now())
            && status != TaskStatus.DONE
            && status != TaskStatus.CANCELLED;
    }

    /** True when the task is due today (regardless of time). */
    public boolean isDueToday() {
        if (dueDate == null) return false;
        return dueDate.toLocalDate().equals(java.time.LocalDate.now());
    }
}

package com.nexus.repository;

import com.nexus.model.Task;
import com.nexus.model.TaskFilter;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nexus.db.Tables.TASKS;

/**
 * Data-access layer for {@link Task}.
 *
 * <p>The {@link #findAll(TaskFilter)} method builds a fully dynamic WHERE clause
 * using JOOQ — this is one of JOOQ's key strengths over string-based SQL.
 */
public class TaskRepository {

    private static final Logger log = LoggerFactory.getLogger(TaskRepository.class);

    /** Raw typed field for the START_TIME column (added in V6).
     *  Replaced by TASKS.START_TIME once 'mvn generate-sources' is re-run. */
    private static final Field<LocalTime> START_TIME =
        DSL.field("START_TIME", SQLDataType.LOCALTIME);

    /** Raw typed field for the SNOOZED_UNTIL column (added in V11). */
    private static final Field<LocalDateTime> SNOOZED_UNTIL =
        DSL.field("SNOOZED_UNTIL", SQLDataType.LOCALDATETIME);

    /** Raw typed field for defer_until (added in V12). */
    private static final Field<LocalDateTime> DEFER_UNTIL =
        DSL.field("DEFER_UNTIL", SQLDataType.LOCALDATETIME);

    /** Raw typed field for lifecycle bucket (added in V12). */
    private static final Field<String> LIFECYCLE =
        DSL.field("LIFECYCLE", SQLDataType.VARCHAR(10));

    private final DSLContext dsl;

    public TaskRepository(DSLContext dsl) { this.dsl = dsl; }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Optional<Task> findById(long id) {
        return dsl.selectFrom(TASKS)
            .where(TASKS.ID.eq(id))
            .fetchOptional()
            .map(this::recordToTask);
    }

    /**
     * Returns tasks matching the given filter, ordered by due date (nulls last)
     * then by priority (CRITICAL first).
     *
     * <p>Only top-level tasks are returned — subtasks (parent_task_id IS NOT NULL)
     * are loaded separately on demand.
     */
    public List<Task> findAll(TaskFilter filter) {
        List<Condition> conditions = new ArrayList<>();

        // Archive gate — always applied
        conditions.add(TASKS.IS_ARCHIVED.eq(filter.isShowArchived()));

        // Only top-level tasks in the list view
        conditions.add(TASKS.PARENT_TASK_ID.isNull());

        if (filter.getCategoryId() != null) {
            conditions.add(TASKS.CATEGORY_ID.eq(filter.getCategoryId()));
        }
        if (filter.getProjectId() != null) {
            conditions.add(TASKS.PROJECT_ID.eq(filter.getProjectId()));
        }
        if (filter.getStatus() != null) {
            conditions.add(TASKS.STATUS.eq(filter.getStatus().name()));
        }
        if (filter.getPriority() != null) {
            conditions.add(TASKS.PRIORITY.eq(filter.getPriority().name()));
        }
        if (filter.getSearchText() != null && !filter.getSearchText().isBlank()) {
            conditions.add(TASKS.TITLE.containsIgnoreCase(filter.getSearchText().trim()));
        }
        if (filter.getLifecycle() != null) {
            conditions.add(LIFECYCLE.eq(filter.getLifecycle()));
        }
        // Deferred gate: unless showDeferred=true, exclude tasks with a future defer_until
        if (!filter.isShowDeferred()) {
            conditions.add(DEFER_UNTIL.isNull()
                .or(DEFER_UNTIL.le(DSL.field("CURRENT_TIMESTAMP", LocalDateTime.class))));
        } else {
            // Scheduled view: only tasks with a future defer_until
            conditions.add(DEFER_UNTIL.isNotNull()
                .and(DEFER_UNTIL.gt(DSL.field("CURRENT_TIMESTAMP", LocalDateTime.class))));
        }

        return dsl.selectFrom(TASKS)
            .where(DSL.and(conditions))
            .orderBy(
                TASKS.DUE_DATE.asc().nullsLast(),
                // Map priority to sort order: CRITICAL(4) → HIGH(3) → MEDIUM(2) → LOW(1)
                DSL.field("CASE TASKS.PRIORITY " +
                           "WHEN 'CRITICAL' THEN 4 " +
                           "WHEN 'HIGH'     THEN 3 " +
                           "WHEN 'MEDIUM'   THEN 2 " +
                           "ELSE 1 END").desc()
            )
            .fetch()
            .map(this::recordToTask);
    }

    /**
     * Returns only the tasks that could ever trigger a reminder:
     * non-archived, not done/cancelled, with both DUE_DATE and REMINDER_MINUTES_BEFORE set.
     * Used by ReminderService to avoid loading the full task table every minute.
     */
    public List<Task> findWithActiveReminders() {
        return dsl.selectFrom(TASKS)
            .where(TASKS.IS_ARCHIVED.eq(false)
                .and(TASKS.PARENT_TASK_ID.isNull())
                .and(TASKS.DUE_DATE.isNotNull())
                .and(DSL.field("REMINDER_MINUTES_BEFORE").isNotNull())
                .and(TASKS.STATUS.notIn("DONE", "CANCELLED")))
            .fetch()
            .map(this::recordToTask);
    }

    /** Returns all tasks due today (due_date on today's date, not archived). */
    public List<Task> findDueToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(LocalTime.MAX);
        return dsl.selectFrom(TASKS)
            .where(TASKS.DUE_DATE.between(startOfDay, endOfDay)
                .and(TASKS.IS_ARCHIVED.eq(false))
                .and(TASKS.PARENT_TASK_ID.isNull()))
            .orderBy(TASKS.DUE_DATE.asc())
            .fetch()
            .map(this::recordToTask);
    }

    /** Returns tasks for the current ISO week (Monday–Sunday), not archived. */
    public List<Task> findDueThisWeek() {
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        return findDueInWeek(monday, monday.plusDays(6));
    }

    /** Returns tasks for any arbitrary week range, not archived. */
    public List<Task> findDueInWeek(LocalDate monday, LocalDate sunday) {
        return dsl.selectFrom(TASKS)
            .where(TASKS.DUE_DATE.between(
                        monday.atStartOfDay(),
                        sunday.atTime(LocalTime.MAX))
                .and(TASKS.IS_ARCHIVED.eq(false))
                .and(TASKS.PARENT_TASK_ID.isNull()))
            .orderBy(TASKS.DUE_DATE.asc())
            .fetch()
            .map(this::recordToTask);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public Task save(Task task) {
        var record = dsl.newRecord(TASKS);
        applyTaskToRecord(task, record);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.store();
        task.setId(record.getId());
        // START_TIME not in generated schema yet — set via raw SQL after insert
        if (task.getStartTime() != null) {
            dsl.execute("UPDATE TASKS SET START_TIME = ? WHERE ID = ?",
                task.getStartTime(), task.getId());
        }
        log.debug("Saved task '{}' id={}", task.getTitle(), task.getId());
        return task;
    }

    public Task update(Task task) {
        dsl.update(TASKS)
            .set(TASKS.TITLE,                    task.getTitle())
            .set(TASKS.DESCRIPTION,              task.getDescription())
            .set(TASKS.CATEGORY_ID,              task.getCategoryId())
            .set(TASKS.PROJECT_ID,               task.getProjectId())
            .set(TASKS.PRIORITY,                 task.getPriority().name())
            .set(TASKS.STATUS,                   task.getStatus().name())
            .set(TASKS.DUE_DATE,                 task.getDueDate())
            .set(START_TIME,                     task.getStartTime())
            .set(TASKS.ESTIMATED_MINUTES,        task.getEstimatedMinutes())
            .set(TASKS.ACTUAL_MINUTES,           task.getActualMinutes())
            .set(TASKS.RECURRENCE_RULE_ID,       task.getRecurrenceRuleId())
            .set(TASKS.PARENT_TASK_ID,           task.getParentTaskId())
            .set(TASKS.REMINDER_MINUTES_BEFORE,  task.getReminderMinutesBefore())
            .set(TASKS.IS_IMPORTANT,             task.isImportant())
            .set(TASKS.IS_URGENT,                task.isUrgent())
            .set(TASKS.COMPLETED_AT,             task.getCompletedAt())
            .set(TASKS.IS_ARCHIVED,              task.isArchived())
            .set(TASKS.ARCHIVED_AT,              task.getArchivedAt())
            .set(SNOOZED_UNTIL,                  task.getSnoozedUntil())
            .set(DEFER_UNTIL,                    task.getDeferUntil())
            .set(LIFECYCLE,                      task.getLifecycle() != null ? task.getLifecycle() : "ANYTIME")
            .set(TASKS.UPDATED_AT,               LocalDateTime.now())
            .where(TASKS.ID.eq(task.getId()))
            .execute();
        log.debug("Updated task id={}", task.getId());
        return task;
    }

    public void delete(long id) {
        dsl.deleteFrom(TASKS).where(TASKS.ID.eq(id)).execute();
        log.debug("Deleted task id={}", id);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    /**
     * Maps a JOOQ {@code TasksRecord} to the domain {@link Task}.
     * Enriched fields (category, tags, subtasks) are NOT loaded here —
     * they are populated by the service layer on demand.
     */
    Task recordToTask(org.jooq.Record r) {
        // START_TIME: read by column name since it was added in V6 (not in generated schema yet)
        LocalTime startTime = null;
        try { startTime = r.get("START_TIME", LocalTime.class); } catch (Exception ignored) {}

        // SNOOZED_UNTIL: added in V11 (not in generated schema yet)
        LocalDateTime snoozedUntil = null;
        try { snoozedUntil = r.get(SNOOZED_UNTIL); } catch (Exception ignored) {}

        // DEFER_UNTIL, LIFECYCLE: added in V12
        LocalDateTime deferUntil = null;
        try { deferUntil = r.get(DEFER_UNTIL); } catch (Exception ignored) {}
        String lifecycle = "ANYTIME";
        try {
            String lc = r.get(LIFECYCLE);
            if (lc != null) lifecycle = lc;
        } catch (Exception ignored) {}

        return Task.builder()
            .id(r.get(TASKS.ID))
            .title(r.get(TASKS.TITLE))
            .description(r.get(TASKS.DESCRIPTION))
            .categoryId(r.get(TASKS.CATEGORY_ID))
            .projectId(r.get(TASKS.PROJECT_ID))
            .priority(r.get(TASKS.PRIORITY) != null ? Priority.valueOf(r.get(TASKS.PRIORITY)) : Priority.MEDIUM)
            .status(r.get(TASKS.STATUS) != null ? TaskStatus.valueOf(r.get(TASKS.STATUS)) : TaskStatus.TODO)
            .dueDate(r.get(TASKS.DUE_DATE))
            .startTime(startTime)
            .estimatedMinutes(r.get(TASKS.ESTIMATED_MINUTES))
            .actualMinutes(r.get(TASKS.ACTUAL_MINUTES) != null ? r.get(TASKS.ACTUAL_MINUTES) : 0)
            .recurrenceRuleId(r.get(TASKS.RECURRENCE_RULE_ID))
            .parentTaskId(r.get(TASKS.PARENT_TASK_ID))
            .reminderMinutesBefore(r.get(TASKS.REMINDER_MINUTES_BEFORE))
            .important(Boolean.TRUE.equals(r.get(TASKS.IS_IMPORTANT)))
            .urgent(Boolean.TRUE.equals(r.get(TASKS.IS_URGENT)))
            .deferUntil(deferUntil)
            .lifecycle(lifecycle)
            .snoozedUntil(snoozedUntil)
            .completedAt(r.get(TASKS.COMPLETED_AT))
            .archived(Boolean.TRUE.equals(r.get(TASKS.IS_ARCHIVED)))
            .archivedAt(r.get(TASKS.ARCHIVED_AT))
            .createdAt(r.get(TASKS.CREATED_AT))
            .updatedAt(r.get(TASKS.UPDATED_AT))
            .build();
    }

    // ── Multi-category join table ─────────────────────────────────────────────

    public List<Long> getTaskCategoryIds(long taskId) {
        return dsl.select(DSL.field("CATEGORY_ID", Long.class))
            .from(DSL.table("TASK_CATEGORIES"))
            .where(DSL.field("TASK_ID", Long.class).eq(taskId))
            .fetch(DSL.field("CATEGORY_ID", Long.class));
    }

    /** Batch variant — one query for multiple tasks, returns taskId → categoryId list map. */
    public Map<Long, List<Long>> getTaskCategoryIdsBatch(List<Long> taskIds) {
        if (taskIds.isEmpty()) return Map.of();
        return dsl.select(DSL.field("TASK_ID", Long.class), DSL.field("CATEGORY_ID", Long.class))
            .from(DSL.table("TASK_CATEGORIES"))
            .where(DSL.field("TASK_ID", Long.class).in(taskIds))
            .fetch()
            .stream()
            .collect(Collectors.groupingBy(
                r -> r.get(DSL.field("TASK_ID", Long.class)),
                Collectors.mapping(r -> r.get(DSL.field("CATEGORY_ID", Long.class)), Collectors.toList())));
    }

    public void setTaskCategories(long taskId, List<Long> categoryIds) {
        dsl.deleteFrom(DSL.table("TASK_CATEGORIES"))
            .where(DSL.field("TASK_ID", Long.class).eq(taskId))
            .execute();
        for (Long catId : categoryIds) {
            dsl.insertInto(DSL.table("TASK_CATEGORIES"))
                .set(DSL.field("TASK_ID",     Long.class), taskId)
                .set(DSL.field("CATEGORY_ID", Long.class), catId)
                .execute();
        }
    }

    private void applyTaskToRecord(Task task, org.jooq.Record record) {
        record.set(TASKS.TITLE,                   task.getTitle());
        record.set(TASKS.DESCRIPTION,             task.getDescription());
        record.set(TASKS.CATEGORY_ID,             task.getCategoryId());
        record.set(TASKS.PROJECT_ID,              task.getProjectId());
        record.set(TASKS.PRIORITY,                task.getPriority().name());
        record.set(TASKS.STATUS,                  task.getStatus().name());
        record.set(TASKS.DUE_DATE,                task.getDueDate());
        record.set(TASKS.ESTIMATED_MINUTES,       task.getEstimatedMinutes());
        record.set(TASKS.ACTUAL_MINUTES,          task.getActualMinutes());
        record.set(TASKS.RECURRENCE_RULE_ID,      task.getRecurrenceRuleId());
        record.set(TASKS.PARENT_TASK_ID,          task.getParentTaskId());
        record.set(TASKS.REMINDER_MINUTES_BEFORE, task.getReminderMinutesBefore());
        record.set(TASKS.IS_IMPORTANT,            task.isImportant());
        record.set(TASKS.IS_URGENT,               task.isUrgent());
        record.set(TASKS.COMPLETED_AT,            task.getCompletedAt());
        record.set(TASKS.IS_ARCHIVED,             task.isArchived());
        record.set(TASKS.ARCHIVED_AT,             task.getArchivedAt());
    }
}

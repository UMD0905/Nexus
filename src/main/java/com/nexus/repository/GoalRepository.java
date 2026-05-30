package com.nexus.repository;

import com.nexus.model.Goal;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.nexus.db.Tables.GOALS;
import static com.nexus.db.Tables.GOAL_TASKS;
import static com.nexus.db.Tables.TASKS;

/**
 * Data-access layer for {@link Goal}.
 */
public class GoalRepository {

    private static final Logger log = LoggerFactory.getLogger(GoalRepository.class);

    private final DSLContext dsl;

    public GoalRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<Goal> findAll() {
        return dsl.selectFrom(GOALS)
            .orderBy(GOALS.TARGET_DATE.asc().nullsLast(), GOALS.CREATED_AT.desc())
            .fetch()
            .map(this::recordToGoal);
    }

    public Optional<Goal> findById(long id) {
        return dsl.selectFrom(GOALS)
            .where(GOALS.ID.eq(id))
            .fetchOptional()
            .map(this::recordToGoal);
    }

    public Goal save(Goal goal) {
        var record = dsl.newRecord(GOALS);
        applyToRecord(goal, record);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.store();
        goal.setId(record.getId());
        log.debug("Saved goal '{}' id={}", goal.getTitle(), goal.getId());
        return goal;
    }

    public void update(Goal goal) {
        dsl.update(GOALS)
            .set(GOALS.TITLE,       goal.getTitle())
            .set(GOALS.DESCRIPTION, goal.getDescription())
            .set(GOALS.CATEGORY_ID, goal.getCategoryId())
            .set(GOALS.TARGET_DATE, goal.getTargetDate())
            .set(GOALS.STATUS,      goal.getStatus())
            .set(GOALS.UPDATED_AT,  LocalDateTime.now())
            .where(GOALS.ID.eq(goal.getId()))
            .execute();
    }

    public void delete(long id) {
        dsl.deleteFrom(GOALS).where(GOALS.ID.eq(id)).execute();
    }

    // ── Goal ↔ Task links ─────────────────────────────────────────────────────

    public void linkTask(long goalId, long taskId) {
        // Ignore if already linked
        int existing = dsl.fetchCount(GOAL_TASKS,
            GOAL_TASKS.GOAL_ID.eq(goalId).and(GOAL_TASKS.TASK_ID.eq(taskId)));
        if (existing == 0) {
            dsl.insertInto(GOAL_TASKS)
                .set(GOAL_TASKS.GOAL_ID, goalId)
                .set(GOAL_TASKS.TASK_ID, taskId)
                .execute();
        }
    }

    public void unlinkTask(long goalId, long taskId) {
        dsl.deleteFrom(GOAL_TASKS)
            .where(GOAL_TASKS.GOAL_ID.eq(goalId).and(GOAL_TASKS.TASK_ID.eq(taskId)))
            .execute();
    }

    public List<Long> findLinkedTaskIds(long goalId) {
        return dsl.select(GOAL_TASKS.TASK_ID)
            .from(GOAL_TASKS)
            .where(GOAL_TASKS.GOAL_ID.eq(goalId))
            .fetch(GOAL_TASKS.TASK_ID);
    }

    /** Returns the first goalId this task is linked to, or empty. */
    public Optional<Long> findGoalIdByTaskId(long taskId) {
        return dsl.select(GOAL_TASKS.GOAL_ID)
            .from(GOAL_TASKS)
            .where(GOAL_TASKS.TASK_ID.eq(taskId))
            .limit(1)
            .fetchOptional(GOAL_TASKS.GOAL_ID);
    }

    /** Removes all goal links for a task (called before re-linking). */
    public void unlinkTaskFromAllGoals(long taskId) {
        dsl.deleteFrom(GOAL_TASKS).where(GOAL_TASKS.TASK_ID.eq(taskId)).execute();
    }

    /**
     * Finds the first goal linked to any task that belongs to the given recurrence rule.
     * Used by RecurrenceService to copy goal links to newly generated task instances.
     */
    public Optional<Long> findGoalIdByRuleId(long ruleId) {
        return dsl.select(GOAL_TASKS.GOAL_ID)
            .from(GOAL_TASKS)
            .join(TASKS).on(GOAL_TASKS.TASK_ID.eq(TASKS.ID))
            .where(TASKS.RECURRENCE_RULE_ID.eq(ruleId))
            .limit(1)
            .fetchOptional(GOAL_TASKS.GOAL_ID);
    }

    // ── Multi-category join table ─────────────────────────────────────────────

    public List<Long> getGoalCategoryIds(long goalId) {
        return dsl.select(DSL.field("CATEGORY_ID", Long.class))
            .from(DSL.table("GOAL_CATEGORIES"))
            .where(DSL.field("GOAL_ID", Long.class).eq(goalId))
            .fetch(DSL.field("CATEGORY_ID", Long.class));
    }

    public void setGoalCategories(long goalId, List<Long> categoryIds) {
        dsl.deleteFrom(DSL.table("GOAL_CATEGORIES"))
            .where(DSL.field("GOAL_ID", Long.class).eq(goalId))
            .execute();
        for (Long catId : categoryIds) {
            dsl.insertInto(DSL.table("GOAL_CATEGORIES"))
                .set(DSL.field("GOAL_ID",     Long.class), goalId)
                .set(DSL.field("CATEGORY_ID", Long.class), catId)
                .execute();
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private void applyToRecord(Goal goal, org.jooq.Record record) {
        record.set(GOALS.TITLE,       goal.getTitle());
        record.set(GOALS.DESCRIPTION, goal.getDescription());
        record.set(GOALS.CATEGORY_ID, goal.getCategoryId());
        record.set(GOALS.TARGET_DATE, goal.getTargetDate());
        record.set(GOALS.STATUS,      goal.getStatus() != null ? goal.getStatus() : "ACTIVE");
    }

    private Goal recordToGoal(org.jooq.Record r) {
        return Goal.builder()
            .id(r.get(GOALS.ID))
            .title(r.get(GOALS.TITLE))
            .description(r.get(GOALS.DESCRIPTION))
            .categoryId(r.get(GOALS.CATEGORY_ID))
            .targetDate(r.get(GOALS.TARGET_DATE))
            .status(r.get(GOALS.STATUS))
            .createdAt(r.get(GOALS.CREATED_AT))
            .updatedAt(r.get(GOALS.UPDATED_AT))
            .build();
    }
}

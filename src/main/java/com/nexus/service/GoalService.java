package com.nexus.service;

import com.nexus.model.Goal;
import com.nexus.model.Task;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.CategoryRepository;
import com.nexus.repository.GoalRepository;
import com.nexus.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for {@link Goal}.
 *
 * <p>Progress is computed as:
 * <pre>  completedTasks / linkedTasks  (0.0 – 1.0)</pre>
 * A goal auto-completes when all linked tasks are done.
 */
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);

    private final GoalRepository     goalRepo;
    private final TaskRepository     taskRepo;
    private final CategoryRepository categoryRepo;

    public GoalService(GoalRepository goalRepo,
                       TaskRepository taskRepo,
                       CategoryRepository categoryRepo) {
        this.goalRepo     = goalRepo;
        this.taskRepo     = taskRepo;
        this.categoryRepo = categoryRepo;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns all goals, enriched with category and linked tasks. */
    public List<Goal> getAllGoals() {
        List<Goal> goals = goalRepo.findAll();
        goals.forEach(this::enrich);
        return goals;
    }

    public Optional<Goal> getGoalById(long id) {
        return goalRepo.findById(id).map(g -> { enrich(g); return g; });
    }

    /**
     * Progress ratio (0.0 – 1.0) for the given goal.
     * Returns 0 if no tasks are linked.
     */
    public double getProgress(Goal goal) {
        List<Task> tasks = goal.getTasks();
        if (tasks.isEmpty()) return 0.0;
        long done = tasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.DONE)
            .count();
        return (double) done / tasks.size();
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public Goal createGoal(Goal goal) {
        if (goal.getTitle() == null || goal.getTitle().isBlank()) {
            throw new IllegalArgumentException("Goal title must not be blank.");
        }
        goal.setStatus("ACTIVE");
        Goal saved = goalRepo.save(goal);
        log.info("Created goal '{}' id={}", saved.getTitle(), saved.getId());
        return saved;
    }

    public void updateGoal(Goal goal) {
        if (goal.getId() == null) throw new IllegalArgumentException("Cannot update goal without id.");
        goalRepo.update(goal);
        log.debug("Updated goal id={}", goal.getId());
    }

    public void completeGoal(long goalId) {
        goalRepo.findById(goalId).ifPresent(goal -> {
            goal.setStatus("COMPLETED");
            goalRepo.update(goal);
            log.info("Marked goal '{}' as COMPLETED", goal.getTitle());
        });
    }

    public void abandonGoal(long goalId) {
        goalRepo.findById(goalId).ifPresent(goal -> {
            goal.setStatus("ABANDONED");
            goalRepo.update(goal);
            log.info("Marked goal '{}' as ABANDONED", goal.getTitle());
        });
    }

    public void deleteGoal(long goalId) {
        goalRepo.delete(goalId);
        log.info("Deleted goal id={}", goalId);
    }

    public void linkTask(long goalId, long taskId) {
        goalRepo.linkTask(goalId, taskId);
    }

    public void unlinkTask(long goalId, long taskId) {
        goalRepo.unlinkTask(goalId, taskId);
    }

    /** Returns the goalId this task is currently linked to, or empty. */
    public Optional<Long> findGoalIdByTask(long taskId) {
        return goalRepo.findGoalIdByTaskId(taskId);
    }

    /** Replaces any existing goal link for the task with a new one (or clears it). */
    public void relinkTask(long taskId, Long newGoalId) {
        goalRepo.unlinkTaskFromAllGoals(taskId);
        if (newGoalId != null) {
            goalRepo.linkTask(newGoalId, taskId);
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void enrich(Goal goal) {
        if (goal.getCategoryId() != null) {
            categoryRepo.findById(goal.getCategoryId()).ifPresent(goal::setCategory);
        }
        List<Long> taskIds = goalRepo.findLinkedTaskIds(goal.getId());
        List<Task> tasks = taskIds.stream()
            .map(taskRepo::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            // Exclude soft-deleted recurring instances (CANCELLED + archived) from goal progress.
            .filter(t -> !(t.getRecurrenceRuleId() != null
                           && t.getStatus() == com.nexus.model.enums.TaskStatus.CANCELLED
                           && t.isArchived()))
            .toList();
        goal.setTasks(tasks);
    }
}

package com.nexus.service;

import com.nexus.model.*;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.*;
import com.nexus.repository.GoalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core business logic for {@link Task}.
 *
 * <p>This class owns the task lifecycle rules:
 * <ul>
 *   <li>Validation on create/update</li>
 *   <li>Stamping {@code completedAt} when a task is marked done</li>
 *   <li>Stamping {@code archivedAt} when a task is archived</li>
 *   <li>Enriching tasks with their category, tags, and subtasks</li>
 * </ul>
 *
 * <p>Service methods are all synchronous — JavaFX callers should run them
 * on a background thread if the result set is large (> ~1000 tasks).
 */
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository         taskRepository;
    private final CategoryRepository     categoryRepository;
    private final TagRepository          tagRepository;
    private final SubtaskRepository      subtaskRepository;
    private final StreakService          streakService;
    private final GoalRepository         goalRepository;
    private final GoalService            goalService;
    private RecurrenceService            recurrenceService;  // set after construction to avoid cycle

    public TaskService(TaskRepository taskRepository,
                       CategoryRepository categoryRepository,
                       TagRepository tagRepository,
                       SubtaskRepository subtaskRepository,
                       StreakService streakService,
                       GoalRepository goalRepository,
                       GoalService goalService) {
        this.taskRepository      = taskRepository;
        this.categoryRepository  = categoryRepository;
        this.tagRepository       = tagRepository;
        this.subtaskRepository   = subtaskRepository;
        this.streakService       = streakService;
        this.goalRepository      = goalRepository;
        this.goalService         = goalService;
    }

    /** Called by AppContext after RecurrenceService is constructed (avoids circular dependency). */
    public void setRecurrenceService(RecurrenceService recurrenceService) {
        this.recurrenceService = recurrenceService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns tasks matching the filter, enriched with category and tags.
     * This is the primary method called by the task list ViewModel.
     */
    public List<Task> getTasks(TaskFilter filter) {
        List<Task> tasks = taskRepository.findAll(filter);
        enrich(tasks);
        return tasks;
    }

    /** Returns the single task with all enrichment (category, tags, subtasks). */
    public Optional<Task> getTaskById(long id) {
        return taskRepository.findById(id).map(task -> {
            enrichOne(task);
            task.setSubtasks(subtaskRepository.findByTaskId(id));
            return task;
        });
    }

    public List<Task> getTasksDueToday() {
        List<Task> tasks = taskRepository.findDueToday();
        enrich(tasks);
        return tasks;
    }

    public List<Task> getTasksDueThisWeek() {
        List<Task> tasks = taskRepository.findDueThisWeek();
        enrich(tasks);
        return tasks;
    }

    /** Returns tasks for the ISO week starting at the given Monday. */
    public List<Task> getTasksDueThisWeek(java.time.LocalDate monday) {
        List<Task> tasks = taskRepository.findDueInWeek(monday, monday.plusDays(6));
        enrich(tasks);
        return tasks;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Persists a new task.  Validates, sets defaults, and returns the saved task
     * with its auto-generated id.
     */
    public Task createTask(Task task) {
        validate(task);
        task.setActualMinutes(0);
        task.setArchived(false);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);
        log.info("Created task '{}' (id={})", saved.getTitle(), saved.getId());
        return saved;
    }

    public Task updateTask(Task task) {
        if (task.getId() == null) {
            throw new IllegalArgumentException("Cannot update task without an id");
        }
        validate(task);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.update(task);
    }

    /**
     * Marks a task as done and stamps {@code completedAt}.
     * Does nothing if the task is already done.
     */
    public Task markDone(long taskId) {
        Task task = requireTask(taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            return task;  // idempotent
        }
        task.setStatus(TaskStatus.DONE);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task updated = taskRepository.update(task);
        log.info("Marked task '{}' (id={}) as DONE at {}", task.getTitle(), taskId, task.getCompletedAt());
        streakService.recordCompletion(updated);
        goalService.checkAutoComplete(taskId);
        // AFTER_COMPLETION recurrence: generate the next instance on completion
        if (recurrenceService != null && updated.getRecurrenceRuleId() != null) {
            recurrenceService.generateAfterCompletion(updated);
        }
        return updated;
    }

    /**
     * Moves a task to In Progress.
     */
    public Task markInProgress(long taskId) {
        Task task = requireTask(taskId);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.update(task);
    }

    /**
     * Archives a task.  If it is not already done, marks it done first.
     * Sets {@code isArchived = true} and stamps {@code archivedAt}.
     */
    public Task archiveTask(long taskId) {
        Task task = requireTask(taskId);
        if (task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED) {
            task.setStatus(TaskStatus.DONE);
            task.setCompletedAt(LocalDateTime.now());
        }
        task.setArchived(true);
        task.setArchivedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task updated = taskRepository.update(task);
        log.info("Archived task '{}' (id={})", task.getTitle(), taskId);
        return updated;
    }

    /**
     * Restores an archived task back to active.
     * Resets {@code isArchived} and clears {@code archivedAt}.
     * Status is reset to TODO so the task re-enters the workflow.
     */
    public Task restoreTask(long taskId) {
        Task task = requireTask(taskId);
        task.setArchived(false);
        task.setArchivedAt(null);
        task.setStatus(TaskStatus.TODO);
        task.setCompletedAt(null);
        task.setUpdatedAt(LocalDateTime.now());
        Task updated = taskRepository.update(task);
        log.info("Restored task '{}' (id={}) from archive", task.getTitle(), taskId);
        return updated;
    }

    /**
     * Bulk-archives all tasks with status DONE that are not yet archived.
     * Useful for the "archive all completed" button in the toolbar.
     */
    public int archiveAllCompleted() {
        List<Task> allActive = taskRepository.findAll(TaskFilter.allActive());
        List<Task> toArchive = allActive.stream()
            .filter(t -> t.getStatus() == TaskStatus.DONE)
            .toList();
        for (Task t : toArchive) {
            t.setArchived(true);
            t.setArchivedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            taskRepository.update(t);
        }
        log.info("Bulk-archived {} completed task(s)", toArchive.size());
        return toArchive.size();
    }

    /**
     * Deletes a task permanently.
     *
     * <p>Non-recurring tasks are hard-deleted from the database.
     *
     * <p>Recurring task <em>instances</em> (tasks that have a {@code recurrenceRuleId}) are
     * <em>soft-deleted</em>: the row is kept in the database with
     * {@code status=CANCELLED} and {@code is_archived=TRUE}.  This is necessary because
     * {@link RecurrenceService#generateUpcoming} deduplicates by (ruleId, dueDate) — if the
     * row were hard-deleted, the service would blindly recreate the instance on the next
     * application startup.  A soft-deleted row acts as a permanent "skip this date" marker
     * while remaining invisible to all active-task queries.
     */
    public void deleteTask(long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;

        if (task.getRecurrenceRuleId() != null) {
            // Soft-delete: mark cancelled + archived so RecurrenceService skips this date.
            task.setStatus(TaskStatus.CANCELLED);
            task.setArchived(true);
            task.setArchivedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.update(task);
            log.info("Soft-deleted recurring task instance id={} (rule={})", taskId, task.getRecurrenceRuleId());
        } else {
            taskRepository.delete(taskId);
            log.info("Deleted task id={}", taskId);
        }
        goalRepository.unlinkTaskFromAllGoals(taskId);
    }

    // ── Tag management ────────────────────────────────────────────────────────

    public void addTagToTask(long taskId, long tagId) {
        tagRepository.addTagToTask(taskId, tagId);
    }

    public void removeTagFromTask(long taskId, long tagId) {
        tagRepository.removeTagFromTask(taskId, tagId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Task requireTask(long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    private void validate(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
        if (task.getPriority() == null) {
            throw new IllegalArgumentException("Task priority must not be null");
        }
        if (task.getStatus() == null) {
            throw new IllegalArgumentException("Task status must not be null");
        }
    }

    /**
     * Bulk-enriches a list with category and tag data using three queries total —
     * one for categories, one for tags, one for multi-category join — regardless
     * of list size.  Replaces the previous N+1 per-task approach.
     */
    private void enrich(List<Task> tasks) {
        if (tasks.isEmpty()) return;

        Map<Long, Category> categoryById = categoryRepository.findAll()
            .stream().collect(Collectors.toMap(Category::getId, c -> c));

        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        Map<Long, List<Tag>>  tagsByTaskId   = tagRepository.findByTaskIds(taskIds);
        Map<Long, List<Long>> catIdsByTaskId = taskRepository.getTaskCategoryIdsBatch(taskIds);

        for (Task t : tasks) {
            if (t.getCategoryId() != null) {
                t.setCategory(categoryById.get(t.getCategoryId()));
            }
            t.setTags(tagsByTaskId.getOrDefault(t.getId(), List.of()));
            t.setCategories(catIdsByTaskId.getOrDefault(t.getId(), List.of()).stream()
                .map(categoryById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        }
    }

    private void enrichOne(Task task) {
        Map<Long, Category> categoryById = categoryRepository.findAll()
            .stream().collect(Collectors.toMap(Category::getId, c -> c));
        if (task.getCategoryId() != null) {
            task.setCategory(categoryById.get(task.getCategoryId()));
        }
        task.setTags(tagRepository.findByTaskId(task.getId()));
        List<Long> catIds = taskRepository.getTaskCategoryIds(task.getId());
        task.setCategories(catIds.stream()
            .map(categoryById::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
    }
}

package com.nexus.model;

import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import lombok.*;

/**
 * Value object that carries all active filter/search state for the task list.
 * Passed from the ViewModel to the repository's dynamic WHERE clause builder.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilter {

    /** Filter by life area; null = all areas. */
    private Long       categoryId;
    /** Filter by project; null = all projects. */
    private Long       projectId;
    /** Filter by status; null = all statuses. */
    private TaskStatus status;
    /** Filter by priority; null = all priorities. */
    private Priority   priority;
    /** Free-text substring match on task title (case-insensitive). */
    private String     searchText;
    /**
     * When {@code true}, show only archived tasks.
     * When {@code false} or {@code null}, show only non-archived tasks.
     */
    @Builder.Default
    private boolean    showArchived = false;

    /**
     * GTD lifecycle bucket filter: INBOX | ANYTIME | TODAY | SOMEDAY.
     * Null = no filter (all buckets).
     */
    private String     lifecycle;

    /**
     * When {@code true}, only tasks with a future defer_until are returned (Scheduled view).
     * When {@code false} or {@code null}, deferred tasks are excluded from results.
     */
    @Builder.Default
    private boolean    showDeferred = false;

    /** A filter that shows all active (non-archived) tasks across all areas. */
    public static TaskFilter allActive() {
        return TaskFilter.builder().showArchived(false).build();
    }

    /** A filter that shows only archived tasks. */
    public static TaskFilter archiveOnly() {
        return TaskFilter.builder().showArchived(true).build();
    }

    /** A filter scoped to a single category, showing active tasks. */
    public static TaskFilter forCategory(long categoryId) {
        return TaskFilter.builder().categoryId(categoryId).showArchived(false).build();
    }
}

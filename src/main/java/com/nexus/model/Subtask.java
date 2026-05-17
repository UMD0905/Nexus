package com.nexus.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * A checklist item belonging to a parent {@link Task}.
 * Subtasks do not have their own priority or due date — they are
 * lightweight checkboxes that track progress within a task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subtask {

    private Long          id;
    private Long          taskId;
    private String        title;
    private boolean       completed;
    /** Display order within the parent task's checklist. */
    private int           position;
    private LocalDateTime createdAt;
}

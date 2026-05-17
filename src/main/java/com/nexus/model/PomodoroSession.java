package com.nexus.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Records one Pomodoro work interval for a {@link Task}.
 * The {@code actualMinutes} field on the parent task is the sum of all
 * completed sessions — updated by {@code PomodoroService} on completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PomodoroSession {

    private Long          id;
    private Long          taskId;
    private LocalDateTime startedAt;
    /** Null if the session was interrupted / abandoned. */
    private LocalDateTime endedAt;
    @Builder.Default
    private int           durationMinutes = 25;
    private boolean       completed;
    private String        notes;
    private LocalDateTime createdAt;
}

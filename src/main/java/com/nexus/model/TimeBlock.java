package com.nexus.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A time-blocked slot in the day planner.
 * A block can be linked to a {@link Task} or stand alone as a free/buffer block.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlock {

    private Long          id;
    /** The task occupying this slot; null for unlinked buffer blocks. */
    private Long          taskId;
    /** Display label — defaults to the linked task's title if null. */
    private String        title;
    private LocalDate     blockDate;
    private LocalTime     startTime;
    private LocalTime     endTime;
    /** CSS hex override; falls back to the linked task's category colour. */
    private String        color;
    private LocalDateTime createdAt;

    // ── Enriched ──────────────────────────────────────────────────
    @ToString.Exclude
    private Task          task;
}

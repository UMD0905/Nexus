package com.nexus.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks the consecutive-day completion streak for a recurring task series.
 * One row per {@link RecurrenceRule}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Streak {

    private Long       id;
    private Long       recurrenceRuleId;
    /** Human-readable name shown in the UI (e.g. "Kickboxing"). */
    private String     title;
    private Long       categoryId;
    private int        currentStreak;
    private int        longestStreak;
    /** Date the most recent completing instance was marked DONE. */
    private LocalDate  lastCompletedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Enriched ──────────────────────────────────────────────────
    @ToString.Exclude
    private Category category;

    /** True if the streak is "alive" (last completed today or yesterday). */
    public boolean isActive() {
        if (lastCompletedDate == null) return false;
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return !lastCompletedDate.isBefore(yesterday);
    }
}

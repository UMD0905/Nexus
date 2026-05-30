package com.nexus.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A high-level goal that is achieved by completing linked tasks.
 * Goals belong to a {@link Category} and have an optional deadline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    private Long          id;
    private String        title;
    private String        description;
    private Long          categoryId;
    private LocalDate     targetDate;
    @Builder.Default
    private String        status    = "ACTIVE";  // ACTIVE | COMPLETED | ABANDONED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Enriched ──────────────────────────────────────────────────
    @ToString.Exclude
    private Category      category;
    @ToString.Exclude
    @Builder.Default
    private List<Category> categories = new ArrayList<>();  // all assigned life areas
    @ToString.Exclude
    @Builder.Default
    private List<Task>    tasks     = new ArrayList<>();
}

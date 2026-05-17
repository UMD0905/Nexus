package com.nexus.model;

import com.nexus.model.enums.ProjectStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A project groups related tasks under a shared goal and deadline.
 * Projects belong to a {@link Category} (life area).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    private Long          id;
    private String        name;
    private String        description;
    private Long          categoryId;
    /** CSS hex accent colour, used in the project list. */
    private String        color;
    private LocalDate     startDate;
    private LocalDate     dueDate;
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Enriched (loaded on demand) ──────────────────────────────
    @ToString.Exclude
    private Category      category;
}

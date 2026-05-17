package com.nexus.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a "life area" — e.g. Work, Kickboxing, Gym.
 * Categories give tasks and projects their colour-coded identity
 * throughout the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    private Long          id;
    private String        name;
    /** CSS hex colour, e.g. "#3B82F6". Used for sidebar icons and task pills. */
    private String        color;
    /** Ikonli icon literal, e.g. "mdi2b-briefcase-outline". */
    private String        icon;
    /** Display order in the sidebar. */
    private int           position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.nexus.model;

import lombok.*;

/**
 * A lightweight label that can be applied to any number of tasks.
 * Tags are user-defined and colour-coded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    private Long   id;
    private String name;
    /** CSS hex colour for the tag chip in the UI. */
    private String color;
}

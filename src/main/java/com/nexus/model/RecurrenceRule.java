package com.nexus.model;

import com.nexus.model.enums.RecurrenceType;
import lombok.*;

import java.time.LocalDate;

/**
 * Defines how a recurring task repeats.
 * Kept simple: daily, weekly on specific days, or every weekday.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurrenceRule {

    private Long          id;
    private RecurrenceType type;
    /**
     * Comma-separated day abbreviations for WEEKLY recurrence.
     * E.g. "TUE,THU" for kickboxing.  Null for DAILY / WEEKDAYS.
     */
    private String        daysOfWeek;
    /** Repeat every N occurrences of the unit (almost always 1). */
    private int           intervalVal;
    /** Inclusive end date; null = repeat forever. */
    private LocalDate     endDate;
}

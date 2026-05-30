package com.nexus.model.enums;

public enum RecurrenceType {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    WEEKDAYS("Weekdays (Mon–Fri)"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    private final String displayName;

    RecurrenceType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}

package com.nexus.model.enums;

/** Task priority level, ordered from lowest to highest urgency. */
public enum Priority {
    LOW("Low",      "#2dba82"),   // teal-green
    MEDIUM("Medium","#6373f4"),   // indigo
    HIGH("High",    "#e8a020"),   // amber
    CRITICAL("Critical", "#f05a5a"); // coral-red

    private final String displayName;
    private final String color;

    Priority(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    /** CSS hex colour for UI indicators. */
    public String getColor()       { return color; }

    @Override
    public String toString() { return displayName; }
}

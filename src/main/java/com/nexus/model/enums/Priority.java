package com.nexus.model.enums;

/** Task priority level, ordered from lowest to highest urgency. */
public enum Priority {
    LOW("Low",      "#22C55E"),   // green
    MEDIUM("Medium","#EAB308"),   // yellow
    HIGH("High",    "#F97316"),   // orange
    CRITICAL("Critical", "#EF4444"); // red

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

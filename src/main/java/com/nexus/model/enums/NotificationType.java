package com.nexus.model.enums;

public enum NotificationType {
    REMINDER("Reminder"),
    SYSTEM("System"),
    STREAK("Streak");

    private final String displayName;

    NotificationType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}

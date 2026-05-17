package com.nexus.model.enums;

public enum ProjectStatus {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    ARCHIVED("Archived");

    private final String displayName;

    ProjectStatus(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}

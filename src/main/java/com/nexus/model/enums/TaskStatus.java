package com.nexus.model.enums;

/** Lifecycle status of a task. */
public enum TaskStatus {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done"),
    CANCELLED("Cancelled");

    private final String displayName;

    TaskStatus(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    /** Returns true if this status represents a finished task (done or cancelled). */
    public boolean isTerminal() {
        return this == DONE || this == CANCELLED;
    }

    @Override
    public String toString() { return displayName; }
}

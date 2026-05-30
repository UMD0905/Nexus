package com.nexus.ui.bridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskInput {
    public Long    id;
    public String  title;
    public String  description;
    public Long    categoryId;
    public String  priority;
    public String  status;
    public String  dueDate;
    public String  dueTime;             // "HH:mm" for the end/due time component
    public String  startTime;           // "HH:mm" for when the task begins
    public Integer estimatedMinutes;
    public Boolean urgent;
    public Boolean important;
    public Long    goalId;              // null=no change; -1=clear link; >0=link to goal
    public List<Long> categoryIds;      // all assigned life areas (multi-category)
    // Recurrence (only for new recurring tasks)
    public String  recurrenceType;      // "DAILY" | "WEEKDAYS" | "WEEKLY"
    public String  recurrenceDays;      // e.g. "MON,WED,FRI" for WEEKLY
    public String  recurrenceEndDate;   // "yyyy-MM-dd" or null = repeat forever
    public String  recurrenceMode;      // "FIXED" | "AFTER_COMPLETION"
    // GTD
    public String  lifecycle;           // "INBOX" | "ANYTIME" | "TODAY" | "SOMEDAY"
    public String  deferUntil;          // ISO datetime string
    // Project
    public Long    projectId;
}

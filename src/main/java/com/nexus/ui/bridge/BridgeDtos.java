package com.nexus.ui.bridge;

import com.nexus.config.AppContext;
import com.nexus.model.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static DTO factory methods shared by all sub-bridge classes.
 * Each method takes {@code AppContext} as the first parameter so they can access
 * services (e.g. {@code GoalService.findGoalIdByTask}) without holding a reference themselves.
 */
public final class BridgeDtos {

    private BridgeDtos() {}

    public static Map<String, Object> taskDto(AppContext ctx, Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                 t.getId());
        m.put("title",              t.getTitle());
        m.put("description",        t.getDescription());
        m.put("categoryId",         t.getCategoryId());
        m.put("category",           t.getCategory() != null ? categoryDto(t.getCategory()) : null);
        m.put("priority",           t.getPriority() != null ? t.getPriority().name() : "MEDIUM");
        m.put("status",             t.getStatus() != null ? t.getStatus().name() : "TODO");
        m.put("dueDate",            t.getDueDate() != null ? t.getDueDate().toString() : null);
        m.put("startTime",          t.getStartTime() != null ? t.getStartTime().toString().substring(0, 5) : null);
        m.put("estimatedMinutes",   t.getEstimatedMinutes());
        m.put("urgent",             t.isUrgent());
        m.put("important",          t.isImportant());
        m.put("archived",           t.isArchived());
        m.put("recurrenceRuleId",   t.getRecurrenceRuleId());
        m.put("categoryIds",        t.getCategories() != null
            ? t.getCategories().stream().map(c -> c.getId()).toList() : List.of());
        m.put("categories",         t.getCategories() != null
            ? t.getCategories().stream().map(BridgeDtos::categoryDto).toList() : List.of());
        m.put("tags",               t.getTags() != null
            ? t.getTags().stream().map(BridgeDtos::tagDto).toList() : List.of());
        m.put("createdAt",          t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        m.put("updatedAt",          t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : "");
        m.put("completedAt",        t.getCompletedAt() != null ? t.getCompletedAt().toString() : null);
        m.put("actualMinutes",      t.getActualMinutes());
        m.put("snoozedUntil",       t.getSnoozedUntil() != null ? t.getSnoozedUntil().toString() : null);
        m.put("deferUntil",         t.getDeferUntil()  != null ? t.getDeferUntil().toString()  : null);
        m.put("lifecycle",          t.getLifecycle() != null ? t.getLifecycle() : "ANYTIME");
        m.put("projectId",          t.getProjectId());
        m.put("goalId",             ctx.getGoalService().findGoalIdByTask(t.getId()).orElse(null));
        return m;
    }

    public static Map<String, Object> categoryDto(Category c) {
        return Map.of(
            "id",       c.getId(),
            "name",     c.getName(),
            "color",    c.getColor() != null ? c.getColor() : "#6366f1",
            "position", c.getPosition()
        );
    }

    public static Map<String, Object> tagDto(Tag t) {
        return Map.of(
            "id",    t.getId(),
            "name",  t.getName(),
            "color", t.getColor() != null ? t.getColor() : "#6366f1"
        );
    }

    public static Map<String, Object> subtaskDto(Subtask s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",       s.getId());
        m.put("taskId",   s.getTaskId());
        m.put("title",    s.getTitle());
        m.put("done",     s.isCompleted());
        m.put("position", s.getPosition());
        return m;
    }

    public static Map<String, Object> goalDto(AppContext ctx, Goal g, double progress) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          g.getId());
        m.put("title",       g.getTitle());
        m.put("description", g.getDescription());
        m.put("categoryId",  g.getCategoryId());
        m.put("category",    g.getCategory() != null ? categoryDto(g.getCategory()) : null);
        m.put("targetDate",  g.getTargetDate() != null ? g.getTargetDate().toString() : null);
        m.put("status",      g.getStatus());
        m.put("completed",   "COMPLETED".equals(g.getStatus()));
        m.put("progress",    progress);
        m.put("createdAt",   g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
        m.put("categoryIds", g.getCategories() != null
            ? g.getCategories().stream().map(c -> c.getId()).toList() : List.of());
        m.put("categories",  g.getCategories() != null
            ? g.getCategories().stream().map(BridgeDtos::categoryDto).toList() : List.of());
        m.put("tasks",       g.getTasks() != null
            ? g.getTasks().stream().map(t -> Map.of(
                "id", t.getId(), "title", t.getTitle(),
                "status", t.getStatus() != null ? t.getStatus().name() : "TODO")).toList()
            : List.of());
        return m;
    }

    public static Map<String, Object> streakDto(AppContext ctx, Streak s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                s.getId());
        m.put("title",             s.getTitle());
        m.put("categoryId",        s.getCategoryId());
        m.put("category",          s.getCategory() != null ? categoryDto(s.getCategory()) : null);
        m.put("currentStreak",     s.getCurrentStreak());
        m.put("longestStreak",     s.getLongestStreak());
        m.put("active",            s.isActive());
        m.put("lastCompletedDate", s.getLastCompletedDate() != null ? s.getLastCompletedDate().toString() : null);
        return m;
    }

    public static Map<String, Object> timeBlockDto(TimeBlock b) {
        return Map.of(
            "id",        b.getId(),
            "title",     b.getTitle(),
            "date",      b.getBlockDate() != null ? b.getBlockDate().toString() : "",
            "startTime", b.getStartTime() != null ? b.getStartTime().toString() : "",
            "endTime",   b.getEndTime()   != null ? b.getEndTime().toString()   : "",
            "color",     b.getColor()     != null ? b.getColor() : "#6366f1"
        );
    }

    public static Map<String, Object> projectDto(Project p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          p.getId());
        m.put("name",        p.getName());
        m.put("description", p.getDescription());
        m.put("categoryId",  p.getCategoryId());
        m.put("category",    p.getCategory() != null ? categoryDto(p.getCategory()) : null);
        m.put("color",       p.getColor() != null ? p.getColor() : "#4A90D9");
        m.put("startDate",   p.getStartDate() != null ? p.getStartDate().toString() : null);
        m.put("dueDate",     p.getDueDate()   != null ? p.getDueDate().toString()   : null);
        m.put("status",      p.getStatus() != null ? p.getStatus().name() : "ACTIVE");
        m.put("createdAt",   p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
        m.put("updatedAt",   p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
        return m;
    }

    public static Map<String, Object> notifDto(AppNotification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",        n.getId());
        m.put("title",     n.getTitle()   != null ? n.getTitle()   : "");
        m.put("body",      n.getMessage() != null ? n.getMessage() : "");
        m.put("type",      n.getType()    != null ? n.getType().name() : "INFO");
        m.put("read",      n.isRead());
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
        m.put("taskId",    n.getTaskId());
        return m;
    }
}

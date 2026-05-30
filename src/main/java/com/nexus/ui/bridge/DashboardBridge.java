package com.nexus.ui.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import com.nexus.model.*;
import com.nexus.model.enums.TaskStatus;
import com.nexus.service.PomodoroService;
import com.nexus.service.StreakService;
import com.nexus.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static com.nexus.db.Tables.*;

/**
 * Bridge methods for dashboard statistics, import, and export operations.
 */
public class DashboardBridge {

    private static final Logger log = LoggerFactory.getLogger(DashboardBridge.class);

    private final AppContext ctx;
    private final ObjectMapper json;

    public DashboardBridge(AppContext ctx) {
        this.ctx  = ctx;
        this.json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String getDashboardStats() {
        try {
            TaskService ts = ctx.getTaskService();
            PomodoroService ps = ctx.getPomodoroService();
            StreakService ss = ctx.getStreakService();

            List<Task> active = ts.getTasks(TaskFilter.builder().showArchived(false).build());
            List<Task> all    = ts.getTasks(TaskFilter.builder().showArchived(true).build());

            LocalDate now    = LocalDate.now();
            LocalDate monday = now.with(DayOfWeek.MONDAY);

            // Weekly completions Mon–Sun
            int[] weekly = new int[7];
            all.stream()
               .filter(t -> t.getCompletedAt() != null
                         && !t.getCompletedAt().toLocalDate().isBefore(monday)
                         && !t.getCompletedAt().toLocalDate().isAfter(monday.plusDays(6)))
               .forEach(t -> {
                   int idx = t.getCompletedAt().getDayOfWeek().getValue() - 1;
                   weekly[idx]++;
               });

            // Category breakdown
            Map<String, Long> catBreak = active.stream()
               .filter(t -> t.getCategory() != null)
               .collect(Collectors.groupingBy(t -> t.getCategory().getName(), Collectors.counting()));

            // Streaks
            List<Streak> streaks = ss.getAllStreaks();

            // Manual stat adjustments
            Map<String, Integer> adjustments = loadStatAdjustments();

            long baseActive  = active.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
            long baseDue     = active.stream().filter(Task::isDueToday).count();
            long baseDoneWk  = ctx.getDsl().selectCount()
                .from(TASKS)
                .where(TASKS.COMPLETED_AT.greaterOrEqual(monday.atStartOfDay())
                    .and(TASKS.COMPLETED_AT.lessOrEqual(monday.plusDays(6).atTime(23, 59, 59))))
                .fetchOne(0, Long.class);
            long baseOverdue = ctx.getDsl().selectCount()
                .from(TASKS)
                .where(TASKS.DUE_DATE.lessThan(LocalDateTime.now())
                    .and(TASKS.STATUS.notEqual(TaskStatus.DONE.name()))
                    .and(TASKS.STATUS.notEqual(TaskStatus.CANCELLED.name()))
                    .and(TASKS.IS_ARCHIVED.eq(false)))
                .fetchOne(0, Long.class);
            long basePomodoro    = ps.getTodaySessions().size();
            int  focusTimeThisWk = ctx.getPomodoroSessionRepository().completedMinutesThisWeek();

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalActive",       baseActive   + adjustments.getOrDefault("totalActive", 0));
            stats.put("dueToday",          baseDue      + adjustments.getOrDefault("dueToday", 0));
            stats.put("completedThisWeek", baseDoneWk   + adjustments.getOrDefault("completedThisWeek", 0));
            stats.put("overdueTasks",      baseOverdue  + adjustments.getOrDefault("overdueTasks", 0));
            stats.put("pomodoroToday",     basePomodoro + adjustments.getOrDefault("pomodoroToday", 0));
            stats.put("focusTimeThisWeek", focusTimeThisWk);
            stats.put("weeklyCompletions", weekly);
            stats.put("categoryBreakdown", catBreak);
            stats.put("streaks",           streaks.stream().map(s -> BridgeDtos.streakDto(ctx, s)).toList());
            stats.put("statAdjustments",   adjustments);
            return toJson(stats);
        } catch (Exception e) { return error(e); }
    }

    /** Returns the last 12 months of task completions as a list of {yearMonth, monthName, completed}. */
    public String getMonthlyStats() {
        try {
            List<Task> all = ctx.getTaskService().getTasks(
                TaskFilter.builder().showArchived(true).build());

            Map<YearMonth, Long> byMonth = all.stream()
                .filter(t -> t.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                    t -> YearMonth.from(t.getCompletedAt()),
                    Collectors.counting()));

            List<Map<String, Object>> result = new ArrayList<>();
            YearMonth current = YearMonth.now();
            for (int i = 11; i >= 0; i--) {
                YearMonth ym = current.minusMonths(i);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("yearMonth", ym.toString());
                entry.put("monthName", ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                                       + " " + ym.getYear());
                entry.put("completed", byMonth.getOrDefault(ym, 0L).intValue());
                result.add(entry);
            }
            return toJson(result);
        } catch (Exception e) { return error(e); }
    }

    /**
     * Applies a manual delta to one of the dashboard stat counters.
     * Valid keys: totalActive, dueToday, completedThisWeek, overdueTasks, pomodoroToday.
     */
    public void adjustStat(String key, int delta) {
        try {
            Integer current = ctx.getDsl()
                .select(STAT_ADJUSTMENTS.ADJUSTMENT)
                .from(STAT_ADJUSTMENTS)
                .where(STAT_ADJUSTMENTS.STAT_KEY.eq(key))
                .fetchOne(STAT_ADJUSTMENTS.ADJUSTMENT);
            if (current == null) {
                ctx.getDsl().insertInto(STAT_ADJUSTMENTS)
                    .set(STAT_ADJUSTMENTS.STAT_KEY, key)
                    .set(STAT_ADJUSTMENTS.ADJUSTMENT, delta)
                    .set(STAT_ADJUSTMENTS.UPDATED_AT, LocalDateTime.now())
                    .execute();
            } else {
                ctx.getDsl().update(STAT_ADJUSTMENTS)
                    .set(STAT_ADJUSTMENTS.ADJUSTMENT, current + delta)
                    .set(STAT_ADJUSTMENTS.UPDATED_AT, LocalDateTime.now())
                    .where(STAT_ADJUSTMENTS.STAT_KEY.eq(key))
                    .execute();
            }
        } catch (Exception e) { log.error("adjustStat key={} delta={} failed", key, delta, e); }
    }

    public void resetStatAdjustments() {
        try { ctx.getDsl().deleteFrom(STAT_ADJUSTMENTS).execute(); }
        catch (Exception e) { log.error("resetStatAdjustments failed", e); }
    }

    @SuppressWarnings("unchecked")
    public String importData(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return error(new IOException("File not found: " + filePath));
            String content = Files.readString(file.toPath());
            Map<String, Object> snapshot = json.readValue(content, Map.class);

            int          imported = 0;
            int          skipped  = 0;
            List<String> errors   = new ArrayList<>();

            List<Map<String, Object>> cats = (List<Map<String, Object>>) snapshot.getOrDefault("categories", List.of());
            for (Map<String, Object> c : cats) {
                try {
                    com.nexus.model.Category cat = com.nexus.model.Category.builder()
                        .name((String) c.get("name"))
                        .color((String) c.getOrDefault("color", "#6366f1"))
                        .position(c.get("position") instanceof Number n ? n.intValue() : 0)
                        .build();
                    ctx.getCategoryService().createCategory(cat);
                    imported++;
                } catch (Exception e) {
                    skipped++;
                    if (errors.size() < 20) errors.add("category '" + c.get("name") + "': " + e.getMessage());
                }
            }

            List<Map<String, Object>> tags = (List<Map<String, Object>>) snapshot.getOrDefault("tags", List.of());
            for (Map<String, Object> t : tags) {
                try {
                    Tag tag = Tag.builder()
                        .name((String) t.get("name"))
                        .color((String) t.getOrDefault("color", "#888888"))
                        .build();
                    ctx.getTagRepository().save(tag);
                    imported++;
                } catch (Exception e) {
                    skipped++;
                    if (errors.size() < 20) errors.add("tag '" + t.get("name") + "': " + e.getMessage());
                }
            }

            List<Map<String, Object>> tasks = (List<Map<String, Object>>) snapshot.getOrDefault("tasks", List.of());
            for (Map<String, Object> t : tasks) {
                try {
                    Task task = Task.builder()
                        .title((String) t.get("title"))
                        .description((String) t.get("description"))
                        .priority(t.get("priority") != null
                            ? com.nexus.model.enums.Priority.valueOf((String) t.get("priority"))
                            : com.nexus.model.enums.Priority.MEDIUM)
                        .status(TaskStatus.TODO)
                        .urgent(Boolean.TRUE.equals(t.get("urgent")))
                        .important(Boolean.TRUE.equals(t.get("important")))
                        .archived(false)
                        .build();
                    ctx.getTaskService().createTask(task);
                    imported++;
                } catch (Exception e) {
                    skipped++;
                    if (errors.size() < 20) errors.add("task '" + t.get("title") + "': " + e.getMessage());
                }
            }

            List<Map<String, Object>> goals = (List<Map<String, Object>>) snapshot.getOrDefault("goals", List.of());
            for (Map<String, Object> g : goals) {
                try {
                    Goal goal = Goal.builder()
                        .title((String) g.get("title"))
                        .description((String) g.get("description"))
                        .status("ACTIVE")
                        .build();
                    ctx.getGoalService().createGoal(goal);
                    imported++;
                } catch (Exception e) {
                    skipped++;
                    if (errors.size() < 20) errors.add("goal '" + g.get("title") + "': " + e.getMessage());
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("imported", imported);
            result.put("skipped",  skipped);
            result.put("errors",   errors);
            return toJson(result);
        } catch (Exception e) { return error(e); }
    }

    public String exportData(String path) {
        try {
            java.io.File dir = new java.io.File(path);
            java.io.File out = ctx.getExportService().exportTo(dir);
            return "{\"file\":\"" + out.getAbsolutePath().replace("\\", "\\\\") + "\"}";
        } catch (Exception e) { return error(e); }
    }

    public String exportIcal(String path) {
        try {
            File dir = new File(path);
            File out = ctx.getICalExportService().exportTo(dir);
            return "{\"file\":\"" + out.getAbsolutePath().replace("\\", "\\\\") + "\"}";
        } catch (Exception e) { return error(e); }
    }

    /** Triggers an immediate backup on the scheduler thread. */
    public void backupNow() {
        try {
            ctx.getBackupService().backupNow();
        } catch (Exception e) { log.error("backupNow failed", e); }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Integer> loadStatAdjustments() {
        Map<String, Integer> map = new HashMap<>();
        try {
            ctx.getDsl()
                .select(STAT_ADJUSTMENTS.STAT_KEY, STAT_ADJUSTMENTS.ADJUSTMENT)
                .from(STAT_ADJUSTMENTS)
                .fetch()
                .forEach(r -> map.put(r.get(STAT_ADJUSTMENTS.STAT_KEY), r.get(STAT_ADJUSTMENTS.ADJUSTMENT)));
        } catch (Exception ignored) {}
        return map;
    }

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"serialisation failed\"}"; }
    }

    private String error(Exception e) {
        log.error("Bridge error: {}", e.getMessage(), e);
        return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
    }
}

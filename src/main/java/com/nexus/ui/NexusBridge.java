package com.nexus.ui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import com.nexus.model.*;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.service.GoalService;
import com.nexus.service.PomodoroService;
import com.nexus.service.StreakService;
import com.nexus.service.TaskService;
import javafx.application.Platform;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exposes all Nexus services to the React/JavaScript frontend via the WebView JS bridge.
 *
 * <p>JavaFX injects an instance of this class as {@code window.nexusBridge} inside the WebView.
 * Every public method callable from JS must be non-static, return a String (JSON) or void,
 * and take only String / int / long / boolean parameters — the WebView JS bridge does not
 * handle richer Java types automatically.
 *
 * <p>All mutations run synchronously on the JavaFX application thread (the WebView already
 * calls back on that thread).  Heavy queries could be off-loaded to a background thread
 * in a future optimisation.
 */
public class NexusBridge {

    private static final Logger log = LoggerFactory.getLogger(NexusBridge.class);

    private final AppContext ctx;
    private final Stage     stage;
    private final ObjectMapper json;

    /** JS callback handle — kept alive by the WebView engine. */
    private JSObject jsWindow;

    public NexusBridge(AppContext ctx, Stage stage) {
        this.ctx   = ctx;
        this.stage = stage;
        this.json  = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Called once after the page loads to give us a handle to push events to JS. */
    public void init(JSObject window) {
        this.jsWindow = window;
        log.info("NexusBridge initialised — JS window handle acquired");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tasks
    // ─────────────────────────────────────────────────────────────────────────

    public String getTasks(String filterJson) {
        try {
            TaskFilter filter = parseFilter(filterJson);
            filter.setShowArchived(false);
            List<Task> tasks = ctx.getTaskService().getTasks(filter);
            return toJson(tasks.stream().map(this::taskDto).toList());
        } catch (Exception e) {
            return error(e);
        }
    }

    public String getArchivedTasks() {
        try {
            TaskFilter filter = TaskFilter.builder().showArchived(true).build();
            List<Task> tasks = ctx.getTaskService().getTasks(filter);
            // Exclude soft-deleted recurring instances (CANCELLED + archived) — these are
            // internal "skip this date" markers, not user-created archive entries.
            return toJson(tasks.stream()
                .filter(t -> !(t.getRecurrenceRuleId() != null
                               && t.getStatus() == com.nexus.model.enums.TaskStatus.CANCELLED))
                .map(this::taskDto).toList());
        } catch (Exception e) {
            return error(e);
        }
    }

    public String createTask(String taskJson) {
        try {
            TaskInput input = json.readValue(taskJson, TaskInput.class);
            Task task = Task.builder()
                .title(input.title)
                .description(input.description)
                .categoryId(input.categoryId)
                .priority(input.priority != null ? Priority.valueOf(input.priority) : Priority.MEDIUM)
                .status(input.status != null ? TaskStatus.valueOf(input.status) : TaskStatus.TODO)
                .dueDate(input.dueDate != null ? LocalDateTime.parse(input.dueDate.replace("Z","")) : null)
                .estimatedMinutes(input.estimatedMinutes)
                .urgent(Boolean.TRUE.equals(input.urgent))
                .important(Boolean.TRUE.equals(input.important))
                .build();
            Task saved = ctx.getTaskService().createTask(task);
            if (input.goalId != null) {
                ctx.getGoalService().linkTask(input.goalId, saved.getId());
            }
            return toJson(taskDto(saved));
        } catch (Exception e) {
            return error(e);
        }
    }

    public String updateTask(String taskJson) {
        try {
            TaskInput input = json.readValue(taskJson, TaskInput.class);
            Task existing = ctx.getTaskService().getTaskById(input.id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + input.id));
            if (input.title       != null) existing.setTitle(input.title);
            if (input.description != null) existing.setDescription(input.description);
            if (input.categoryId  != null) existing.setCategoryId(input.categoryId);
            if (input.priority    != null) existing.setPriority(Priority.valueOf(input.priority));
            if (input.status      != null) existing.setStatus(TaskStatus.valueOf(input.status));
            if (input.dueDate     != null) existing.setDueDate(input.dueDate.isBlank() ? null
                                               : LocalDateTime.parse(input.dueDate.replace("Z","")));
            if (input.estimatedMinutes != null) existing.setEstimatedMinutes(input.estimatedMinutes);
            if (input.urgent    != null) existing.setUrgent(input.urgent);
            if (input.important != null) existing.setImportant(input.important);
            Task saved = ctx.getTaskService().updateTask(existing);
            // goalId == -1 means "explicitly cleared"; any other value sets a link
            if (input.goalId != null) {
                ctx.getGoalService().relinkTask(saved.getId(), input.goalId == -1 ? null : input.goalId);
            }
            return toJson(taskDto(saved));
        } catch (Exception e) {
            return error(e);
        }
    }

    public void deleteTask(int id) {
        try { ctx.getTaskService().deleteTask(id); }
        catch (Exception e) { log.error("deleteTask failed", e); }
    }

    public void archiveTask(int id) {
        try { ctx.getTaskService().archiveTask(id); }
        catch (Exception e) { log.error("archiveTask failed", e); }
    }

    public void restoreTask(int id) {
        try { ctx.getTaskService().restoreTask(id); }
        catch (Exception e) { log.error("restoreTask failed", e); }
    }

    public String markDone(int id) {
        try { return toJson(taskDto(ctx.getTaskService().markDone(id))); }
        catch (Exception e) { return error(e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Categories
    // ─────────────────────────────────────────────────────────────────────────

    public String getCategories() {
        try {
            return toJson(ctx.getCategoryService().getAllCategories().stream()
                .map(this::categoryDto).toList());
        } catch (Exception e) { return error(e); }
    }

    @SuppressWarnings("unchecked")
    public String createCategory(String catJson) {
        try {
            Map<String, Object> input = json.readValue(catJson, Map.class);
            List<Category> existing = ctx.getCategoryService().getAllCategories();
            int nextPos = existing.stream().mapToInt(Category::getPosition).max().orElse(0) + 1;
            Category cat = Category.builder()
                .name((String) input.get("name"))
                .color((String) input.getOrDefault("color", "#6366f1"))
                .position(nextPos)
                .build();
            return toJson(categoryDto(ctx.getCategoryService().createCategory(cat)));
        } catch (Exception e) { return error(e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Goals
    // ─────────────────────────────────────────────────────────────────────────

    public String getGoals() {
        try {
            GoalService gs = ctx.getGoalService();
            return toJson(gs.getAllGoals().stream().map(g -> goalDto(g, gs.getProgress(g))).toList());
        } catch (Exception e) { return error(e); }
    }

    public String createGoal(String goalJson) {
        try {
            GoalInput input = json.readValue(goalJson, GoalInput.class);
            Goal goal = Goal.builder()
                .title(input.title)
                .description(input.description)
                .categoryId(input.categoryId)
                .targetDate(input.targetDate != null ? LocalDate.parse(input.targetDate.split("T")[0]) : null)
                .status("ACTIVE")
                .build();
            Goal saved = ctx.getGoalService().createGoal(goal);
            return toJson(goalDto(saved, 0.0));
        } catch (Exception e) { return error(e); }
    }

    public String updateGoal(String goalJson) {
        try {
            GoalInput input = json.readValue(goalJson, GoalInput.class);
            GoalService gs = ctx.getGoalService();
            Goal existing = gs.getGoalById(input.id)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + input.id));
            if (input.title       != null) existing.setTitle(input.title);
            if (input.description != null) existing.setDescription(input.description);
            existing.setCategoryId(input.categoryId);   // allow clearing
            existing.setTargetDate(input.targetDate != null
                ? LocalDate.parse(input.targetDate.split("T")[0]) : null);
            gs.updateGoal(existing);
            return toJson(goalDto(existing, gs.getProgress(existing)));
        } catch (Exception e) { return error(e); }
    }

    public void updateGoalStatus(int id, String status) {
        try {
            GoalService gs = ctx.getGoalService();
            switch (status) {
                case "COMPLETED" -> gs.completeGoal(id);
                case "ABANDONED" -> gs.abandonGoal(id);
                default -> log.warn("Unknown goal status: {}", status);
            }
        } catch (Exception e) { log.error("updateGoalStatus failed", e); }
    }

    public void deleteGoal(int id) {
        try { ctx.getGoalService().deleteGoal(id); }
        catch (Exception e) { log.error("deleteGoal failed", e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Time Blocks
    // ─────────────────────────────────────────────────────────────────────────

    public String getTimeBlocks(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return toJson(ctx.getTimeBlockService().getBlocksForDate(date).stream()
                .map(this::timeBlockDto).toList());
        } catch (Exception e) { return error(e); }
    }

    public String getTodayBlocks(String dateStr) {
        return getTimeBlocks(dateStr);
    }

    @SuppressWarnings("unchecked")
    public String createTimeBlock(String blockJson) {
        try {
            Map<String, Object> input = json.readValue(blockJson, Map.class);
            TimeBlock block = TimeBlock.builder()
                .title((String) input.get("title"))
                .blockDate(LocalDate.parse((String) input.get("date")))
                .startTime(java.time.LocalTime.parse((String) input.get("startTime")))
                .endTime(java.time.LocalTime.parse((String) input.get("endTime")))
                .color((String) input.getOrDefault("color", "#6366f1"))
                .build();
            return toJson(timeBlockDto(ctx.getTimeBlockService().createBlock(block)));
        } catch (Exception e) { return error(e); }
    }

    public void deleteTimeBlock(int id) {
        try { ctx.getTimeBlockService().deleteBlock(id); }
        catch (Exception e) { log.error("deleteTimeBlock failed", e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard
    // ─────────────────────────────────────────────────────────────────────────

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

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalActive",       active.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count());
            stats.put("dueToday",          active.stream().filter(Task::isDueToday).count());
            stats.put("completedThisWeek", all.stream().filter(t -> t.getCompletedAt() != null
                                              && !t.getCompletedAt().toLocalDate().isBefore(monday)).count());
            stats.put("overdueTasks",      active.stream().filter(Task::isOverdue).count());
            stats.put("pomodoroToday",     ps.getTodaySessions().size());
            stats.put("weeklyCompletions", weekly);
            stats.put("categoryBreakdown", catBreak);
            stats.put("streaks",           streaks.stream().map(this::streakDto).toList());
            return toJson(stats);
        } catch (Exception e) { return error(e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notifications
    // ─────────────────────────────────────────────────────────────────────────

    public String getNotifications() {
        try {
            return toJson(ctx.getNotificationService().getAll().stream()
                .map(this::notifDto).toList());
        } catch (Exception e) { return error(e); }
    }

    public void markNotificationRead(int id) {
        try { ctx.getNotificationService().markRead(id); }
        catch (Exception e) { log.error("markNotificationRead failed", e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pomodoro
    // ─────────────────────────────────────────────────────────────────────────

    public void logPomodoro() {
        // Logged implicitly via PomodoroService when timer completes via JS.
        // This stub exists so the JS bridge call doesn't fail.
        log.debug("logPomodoro called from JS (anonymous session)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Export
    // ─────────────────────────────────────────────────────────────────────────

    public String exportData(String path) {
        try {
            java.io.File dir = new java.io.File(path);
            java.io.File out = ctx.getExportService().exportTo(dir);
            return "{\"file\":\"" + out.getAbsolutePath().replace("\\", "\\\\") + "\"}";
        } catch (Exception e) { return error(e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Window controls (called from React TopBar)
    // ─────────────────────────────────────────────────────────────────────────

    public void minimizeWindow() {
        Platform.runLater(() -> stage.setIconified(true));
    }

    public void maximizeWindow() {
        Platform.runLater(() -> stage.setMaximized(!stage.isMaximized()));
    }

    public void closeWindow() {
        Platform.runLater(() -> {
            stage.close();
            AppContext.getInstance().shutdown();
            Platform.exit();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Push event to JS
    // ─────────────────────────────────────────────────────────────────────────

    /** Fires {@code window.onBridgeEvent(eventJson)} in the WebView. */
    public void pushEvent(String type, Object payload) {
        if (jsWindow == null) return;
        try {
            Map<String, Object> evt = new HashMap<>();
            evt.put("type", type);
            evt.put("payload", payload);
            String eventJson = json.writeValueAsString(evt);
            Platform.runLater(() -> {
                try { jsWindow.call("onBridgeEvent", eventJson); }
                catch (Exception e) { log.warn("pushEvent failed: {}", e.getMessage()); }
            });
        } catch (Exception e) { log.warn("pushEvent serialisation failed", e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO helpers — produce plain Map objects for JSON serialisation
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> taskDto(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               t.getId());
        m.put("title",            t.getTitle());
        m.put("description",      t.getDescription());
        m.put("categoryId",       t.getCategoryId());
        m.put("category",         t.getCategory() != null ? categoryDto(t.getCategory()) : null);
        m.put("priority",         t.getPriority() != null ? t.getPriority().name() : "MEDIUM");
        m.put("status",           t.getStatus() != null ? t.getStatus().name() : "TODO");
        m.put("dueDate",          t.getDueDate() != null ? t.getDueDate().toString() : null);
        m.put("estimatedMinutes", t.getEstimatedMinutes());
        m.put("urgent",           t.isUrgent());
        m.put("important",        t.isImportant());
        m.put("archived",         t.isArchived());
        m.put("tags",             t.getTags() != null ? t.getTags().stream().map(this::tagDto).toList() : List.of());
        m.put("createdAt",        t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        m.put("updatedAt",        t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : "");
        m.put("completedAt",      t.getCompletedAt() != null ? t.getCompletedAt().toString() : null);
        m.put("goalId",           ctx.getGoalService().findGoalIdByTask(t.getId()).orElse(null));
        return m;
    }

    private Map<String, Object> categoryDto(Category c) {
        return Map.of(
            "id",       c.getId(),
            "name",     c.getName(),
            "color",    c.getColor() != null ? c.getColor() : "#6366f1",
            "position", c.getPosition()
        );
    }

    private Map<String, Object> tagDto(Tag t) {
        return Map.of("id", t.getId(), "name", t.getName(), "color", t.getColor() != null ? t.getColor() : "#6366f1");
    }

    private Map<String, Object> goalDto(Goal g, double progress) {
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
        m.put("tasks",       g.getTasks() != null
            ? g.getTasks().stream().map(t -> Map.of(
                "id", t.getId(), "title", t.getTitle(),
                "status", t.getStatus() != null ? t.getStatus().name() : "TODO")).toList()
            : List.of());
        return m;
    }

    private Map<String, Object> streakDto(Streak s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              s.getId());
        m.put("title",           s.getTitle());
        m.put("categoryId",      s.getCategoryId());
        m.put("category",        s.getCategory() != null ? categoryDto(s.getCategory()) : null);
        m.put("currentStreak",   s.getCurrentStreak());
        m.put("longestStreak",   s.getLongestStreak());
        m.put("active",          s.isActive());
        m.put("lastCompletedDate", s.getLastCompletedDate() != null ? s.getLastCompletedDate().toString() : null);
        return m;
    }

    private Map<String, Object> timeBlockDto(TimeBlock b) {
        return Map.of(
            "id",        b.getId(),
            "title",     b.getTitle(),
            "date",      b.getBlockDate() != null ? b.getBlockDate().toString() : "",
            "startTime", b.getStartTime() != null ? b.getStartTime().toString() : "",
            "endTime",   b.getEndTime()   != null ? b.getEndTime().toString()   : "",
            "color",     b.getColor()     != null ? b.getColor() : "#6366f1"
        );
    }

    private Map<String, Object> notifDto(AppNotification n) {
        return Map.of(
            "id",        n.getId(),
            "title",     n.getTitle()   != null ? n.getTitle()   : "",
            "body",      n.getMessage() != null ? n.getMessage() : "",
            "type",      n.getType()    != null ? n.getType().name() : "INFO",
            "read",      n.isRead(),
            "createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : ""
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"serialisation failed\"}"; }
    }

    private String error(Exception e) {
        log.error("Bridge error: {}", e.getMessage(), e);
        return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
    }

    private TaskFilter parseFilter(String filterJson) throws Exception {
        if (filterJson == null || filterJson.isBlank() || filterJson.equals("{}")) {
            return TaskFilter.builder().showArchived(false).build();
        }
        return json.readValue(filterJson, TaskFilter.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input DTOs (Jackson-deserialised from JS JSON strings)
    // ─────────────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TaskInput {
        public Long    id;
        public String  title;
        public String  description;
        public Long    categoryId;
        public String  priority;
        public String  status;
        public String  dueDate;
        public Integer estimatedMinutes;
        public Boolean urgent;
        public Boolean important;
        public Long    goalId;   // null = no change; -1 = clear link; >0 = link to goal
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GoalInput {
        public Long   id;
        public String title;
        public String description;
        public Long   categoryId;
        public String targetDate;
    }
}

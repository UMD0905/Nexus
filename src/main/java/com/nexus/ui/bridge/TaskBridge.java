package com.nexus.ui.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import com.nexus.model.*;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.RecurrenceType;
import com.nexus.model.enums.TaskStatus;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static com.nexus.db.Tables.*;

/**
 * Bridge methods for task-related operations.
 */
public class TaskBridge {

    private static final Logger log = LoggerFactory.getLogger(TaskBridge.class);

    private final AppContext ctx;
    private final ObjectMapper json;

    public TaskBridge(AppContext ctx) {
        this.ctx  = ctx;
        this.json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String getTasks(String filterJson) {
        try {
            TaskFilter filter = parseFilter(filterJson);
            filter.setShowArchived(false);
            List<Task> tasks = ctx.getTaskService().getTasks(filter);
            return toJson(tasks.stream().map(t -> BridgeDtos.taskDto(ctx, t)).toList());
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
                .map(t -> BridgeDtos.taskDto(ctx, t)).toList());
        } catch (Exception e) {
            return error(e);
        }
    }

    public String createTask(String taskJson) {
        try {
            TaskInput input = json.readValue(taskJson, TaskInput.class);
            final Task[] savedHolder = new Task[1];

            ctx.getDsl().transaction(cfg -> {
                DSLContext txDsl = org.jooq.impl.DSL.using(cfg);

                Task.TaskBuilder builder = Task.builder()
                    .title(input.title)
                    .description(input.description)
                    .categoryId(input.categoryId)
                    .projectId(input.projectId)
                    .priority(input.priority != null ? Priority.valueOf(input.priority) : Priority.MEDIUM)
                    .status(input.status != null ? TaskStatus.valueOf(input.status) : TaskStatus.TODO)
                    .lifecycle(input.lifecycle != null ? input.lifecycle : "ANYTIME")
                    .dueDate(parseDueDate(input.dueDate, input.dueTime))
                    .deferUntil(input.deferUntil != null && !input.deferUntil.isBlank()
                        ? LocalDateTime.parse(input.deferUntil.length() == 10
                            ? input.deferUntil + "T00:00:00" : input.deferUntil) : null)
                    .startTime(parseTime(input.startTime))
                    .estimatedMinutes(input.estimatedMinutes)
                    .urgent(Boolean.TRUE.equals(input.urgent))
                    .important(Boolean.TRUE.equals(input.important));

                if (input.recurrenceType != null && !input.recurrenceType.isBlank()) {
                    RecurrenceRule rule = RecurrenceRule.builder()
                        .type(RecurrenceType.valueOf(input.recurrenceType))
                        .daysOfWeek(input.recurrenceDays)
                        .intervalVal(1)
                        .endDate(input.recurrenceEndDate != null && !input.recurrenceEndDate.isBlank()
                            ? LocalDate.parse(input.recurrenceEndDate) : null)
                        .mode(input.recurrenceMode != null ? input.recurrenceMode : "FIXED")
                        .build();
                    rule = ctx.getRecurrenceRuleRepository().save(rule);
                    ctx.getStreakService().createStreakForRule(rule.getId(), input.title, input.categoryId);
                    builder.recurrenceRuleId(rule.getId());
                }

                Task saved = ctx.getTaskService().createTask(builder.build());

                if (input.goalId != null) {
                    ctx.getGoalService().linkTask(input.goalId, saved.getId());
                }

                if (input.categoryIds != null && !input.categoryIds.isEmpty()) {
                    ctx.getTaskRepository().setTaskCategories(saved.getId(), input.categoryIds);
                }

                savedHolder[0] = saved;
            });

            Task saved = savedHolder[0];
            if (saved.getRecurrenceRuleId() != null) {
                ctx.getRecurrenceService().generateUpcoming(14);
            }

            return toJson(BridgeDtos.taskDto(ctx, saved));
        } catch (Exception e) {
            return error(e);
        }
    }

    public String updateTask(String taskJson) {
        try {
            TaskInput input = json.readValue(taskJson, TaskInput.class);
            final Task[] savedHolder = new Task[1];

            ctx.getDsl().transaction(cfg -> {
                Task existing = ctx.getTaskService().getTaskById(input.id)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + input.id));
                if (input.title            != null) existing.setTitle(input.title);
                if (input.description      != null) existing.setDescription(input.description);
                if (input.categoryId       != null) existing.setCategoryId(input.categoryId);
                if (input.projectId        != null) existing.setProjectId(input.projectId == 0 ? null : input.projectId);
                if (input.priority         != null) existing.setPriority(Priority.valueOf(input.priority));
                if (input.status           != null) existing.setStatus(TaskStatus.valueOf(input.status));
                if (input.lifecycle        != null) existing.setLifecycle(input.lifecycle);
                if (input.dueDate          != null) existing.setDueDate(
                    input.dueDate.isBlank() ? null : parseDueDate(input.dueDate, input.dueTime));
                if (input.deferUntil       != null) existing.setDeferUntil(
                    input.deferUntil.isBlank() ? null : LocalDateTime.parse(
                        input.deferUntil.length() == 10 ? input.deferUntil + "T00:00:00" : input.deferUntil));
                if (input.startTime        != null) existing.setStartTime(
                    input.startTime.isBlank() ? null : parseTime(input.startTime));
                if (input.estimatedMinutes != null) existing.setEstimatedMinutes(input.estimatedMinutes);
                if (input.urgent           != null) existing.setUrgent(input.urgent);
                if (input.important        != null) existing.setImportant(input.important);

                Task saved = ctx.getTaskService().updateTask(existing);

                if (input.goalId != null) {
                    ctx.getGoalService().relinkTask(saved.getId(), input.goalId == -1 ? null : input.goalId);
                }
                if (input.categoryIds != null) {
                    ctx.getTaskRepository().setTaskCategories(saved.getId(), input.categoryIds);
                    if (!input.categoryIds.isEmpty()) {
                        existing.setCategoryId(input.categoryIds.get(0));
                        ctx.getTaskService().updateTask(existing);
                    }
                }

                // Propagate title/time changes to future recurring instances
                if (existing.getRecurrenceRuleId() != null) {
                    ctx.getRecurrenceService().propagateRuleChange(existing.getRecurrenceRuleId());
                }

                savedHolder[0] = saved;
            });

            return toJson(BridgeDtos.taskDto(ctx, savedHolder[0]));
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
        try { return toJson(BridgeDtos.taskDto(ctx, ctx.getTaskService().markDone(id))); }
        catch (Exception e) { return error(e); }
    }

    public void markInProgress(int id) {
        try { ctx.getTaskService().markInProgress(id); }
        catch (Exception e) { log.error("markInProgress failed", e); }
    }

    public String getSubtasks(long taskId) {
        try {
            return toJson(ctx.getSubtaskRepository().findByTaskId(taskId)
                .stream().map(BridgeDtos::subtaskDto).toList());
        } catch (Exception e) { return error(e); }
    }

    @SuppressWarnings("unchecked")
    public String createSubtask(String jsonStr) {
        try {
            Map<String, Object> input = json.readValue(jsonStr, Map.class);
            long taskId = ((Number) input.get("taskId")).longValue();
            String title = (String) input.get("title");
            List<Subtask> existing = ctx.getSubtaskRepository().findByTaskId(taskId);
            int pos = existing.stream().mapToInt(Subtask::getPosition).max().orElse(0) + 1;
            Subtask s = Subtask.builder()
                .taskId(taskId)
                .title(title)
                .completed(false)
                .position(pos)
                .build();
            Subtask saved = ctx.getSubtaskRepository().save(s);
            return toJson(BridgeDtos.subtaskDto(saved));
        } catch (Exception e) { return error(e); }
    }

    public String toggleSubtask(long subtaskId) {
        try {
            List<Subtask> all = ctx.getDsl()
                .selectFrom(SUBTASKS)
                .where(SUBTASKS.ID.eq(subtaskId))
                .fetch()
                .map(r -> Subtask.builder()
                    .id(r.getId())
                    .taskId(r.getTaskId())
                    .title(r.getTitle())
                    .completed(Boolean.TRUE.equals(r.getCompleted()))
                    .position(r.getPosition())
                    .createdAt(r.getCreatedAt())
                    .build());
            if (all.isEmpty()) return error(new IllegalArgumentException("Subtask not found: " + subtaskId));
            Subtask s = all.get(0);
            boolean newDone = !s.isCompleted();
            ctx.getSubtaskRepository().setCompleted(subtaskId, newDone);
            s.setCompleted(newDone);
            return toJson(BridgeDtos.subtaskDto(s));
        } catch (Exception e) { return error(e); }
    }

    public void deleteSubtask(long subtaskId) {
        try { ctx.getSubtaskRepository().delete(subtaskId); }
        catch (Exception e) { log.error("deleteSubtask failed", e); }
    }

    public void skipRecurringInstance(long taskId) {
        try {
            ctx.getTaskService().deleteTask(taskId);
        } catch (Exception e) { log.error("skipRecurringInstance failed", e); }
    }

    @SuppressWarnings("unchecked")
    public void setTaskCategories(long taskId, String categoryIdsJson) {
        try {
            List<Number> ids = json.readValue(categoryIdsJson, List.class);
            List<Long> catIds = ids.stream().map(Number::longValue).toList();
            ctx.getTaskRepository().setTaskCategories(taskId, catIds);
            // Keep primary category_id in sync with first selected area
            if (!catIds.isEmpty()) {
                ctx.getDsl().update(TASKS)
                    .set(TASKS.CATEGORY_ID, catIds.get(0))
                    .where(TASKS.ID.eq(taskId))
                    .execute();
            }
        } catch (Exception e) { log.error("setTaskCategories failed", e); }
    }

    public void snoozeTask(int taskId, int minutes) {
        try {
            ctx.getDsl().execute(
                "UPDATE TASKS SET SNOOZED_UNTIL = ? WHERE ID = ?",
                java.time.LocalDateTime.now().plusMinutes(minutes), (long) taskId);
            log.info("Snoozed task {} for {} minutes", taskId, minutes);
        } catch (Exception e) { log.error("snoozeTask failed", e); }
    }

    @SuppressWarnings("unchecked")
    public void reorderSubtasks(long taskId, String orderedIdsJson) {
        try {
            List<Number> ids = json.readValue(orderedIdsJson, List.class);
            for (int i = 0; i < ids.size(); i++) {
                long subtaskId = ids.get(i).longValue();
                ctx.getDsl().execute(
                    "UPDATE SUBTASKS SET POSITION = ? WHERE ID = ? AND TASK_ID = ?",
                    i + 1, subtaskId, taskId);
            }
        } catch (Exception e) { log.error("reorderSubtasks failed", e); }
    }

    @SuppressWarnings("unchecked")
    public String setTaskTags(long taskId, String tagIdsJson) {
        try {
            List<Number> ids = json.readValue(tagIdsJson, List.class);
            ctx.getTagRepository().removeAllTagsFromTask(taskId);
            for (Number id : ids) {
                ctx.getTagRepository().addTagToTask(taskId, id.longValue());
            }
            return toJson(ctx.getTagRepository().findByTaskId(taskId)
                .stream().map(BridgeDtos::tagDto).toList());
        } catch (Exception e) { return error(e); }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private LocalDateTime parseDueDate(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String datePart = dateStr.contains("T") ? dateStr.split("T")[0] : dateStr;
        LocalDate date  = LocalDate.parse(datePart);
        LocalTime time  = parseTime(timeStr);
        return date.atTime(time != null ? time : LocalTime.MIDNIGHT);
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return null;
        try {
            String t = timeStr.trim();
            if (t.length() == 5) t += ":00";
            return LocalTime.parse(t);
        } catch (Exception e) { return null; }
    }

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
}

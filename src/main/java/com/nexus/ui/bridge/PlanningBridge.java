package com.nexus.ui.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import com.nexus.model.*;
import com.nexus.model.enums.RecurrenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.nexus.db.Tables.*;

/**
 * Bridge methods for time blocks, categories, tags, recurrence rules, and Pomodoro helpers.
 */
public class PlanningBridge {

    private static final Logger log = LoggerFactory.getLogger(PlanningBridge.class);

    /** V10 columns not yet in JOOQ generated schema. */
    private static final org.jooq.Field<Integer> F_DAY_OF_MONTH  =
        org.jooq.impl.DSL.field("DAY_OF_MONTH",  Integer.class);
    private static final org.jooq.Field<Integer> F_MONTH_OF_YEAR =
        org.jooq.impl.DSL.field("MONTH_OF_YEAR", Integer.class);

    private final AppContext ctx;
    private final ObjectMapper json;

    public PlanningBridge(AppContext ctx) {
        this.ctx  = ctx;
        this.json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── Time blocks ───────────────────────────────────────────────────────────

    public String getTimeBlocks(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return toJson(ctx.getTimeBlockService().getBlocksForDate(date).stream()
                .map(BridgeDtos::timeBlockDto).toList());
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
            return toJson(BridgeDtos.timeBlockDto(ctx.getTimeBlockService().createBlock(block)));
        } catch (Exception e) { return error(e); }
    }

    public void deleteTimeBlock(int id) {
        try { ctx.getTimeBlockService().deleteBlock(id); }
        catch (Exception e) { log.error("deleteTimeBlock failed", e); }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    public String getCategories() {
        try {
            return toJson(ctx.getCategoryService().getAllCategories().stream()
                .map(BridgeDtos::categoryDto).toList());
        } catch (Exception e) { return error(e); }
    }

    @SuppressWarnings("unchecked")
    public String updateCategory(String catJson) {
        try {
            Map<String, Object> input = json.readValue(catJson, Map.class);
            long id = ((Number) input.get("id")).longValue();
            Category existing = ctx.getCategoryService().getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
            if (input.containsKey("name"))  existing.setName((String) input.get("name"));
            if (input.containsKey("color")) existing.setColor((String) input.get("color"));
            return toJson(BridgeDtos.categoryDto(ctx.getCategoryService().updateCategory(existing)));
        } catch (Exception e) { return error(e); }
    }

    /**
     * Deletes a category.  Linked tasks are orphaned (category_id SET NULL by the DB FK).
     * This matches the schema's ON DELETE SET NULL on tasks.category_id.
     */
    public void deleteCategory(long id) {
        try { ctx.getCategoryService().deleteCategory(id); }
        catch (Exception e) { log.error("deleteCategory {} failed", id, e); }
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
            return toJson(BridgeDtos.categoryDto(ctx.getCategoryService().createCategory(cat)));
        } catch (Exception e) { return error(e); }
    }

    // ── Tags ──────────────────────────────────────────────────────────────────

    public String getTags() {
        try {
            return toJson(ctx.getTagRepository().findAll().stream()
                .map(BridgeDtos::tagDto).toList());
        } catch (Exception e) { return error(e); }
    }

    @SuppressWarnings("unchecked")
    public String createTag(String jsonStr) {
        try {
            Map<String, Object> input = json.readValue(jsonStr, Map.class);
            Tag t = Tag.builder()
                .name((String) input.get("name"))
                .color((String) input.getOrDefault("color", "#6366f1"))
                .build();
            Tag saved = ctx.getTagRepository().save(t);
            return toJson(BridgeDtos.tagDto(saved));
        } catch (Exception e) { return error(e); }
    }

    public void deleteTag(long tagId) {
        try { ctx.getTagRepository().delete(tagId); }
        catch (Exception e) { log.error("deleteTag failed", e); }
    }

    // ── Recurrence rule editing ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public String updateRecurrenceRule(String jsonStr) {
        try {
            Map<String, Object> input = json.readValue(jsonStr, Map.class);
            long ruleId = ((Number) input.get("ruleId")).longValue();
            RecurrenceRule rule = ctx.getRecurrenceRuleRepository().findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("RecurrenceRule not found: " + ruleId));
            if (input.get("type") != null) {
                rule.setType(RecurrenceType.valueOf((String) input.get("type")));
            }
            if (input.get("daysOfWeek") != null) {
                rule.setDaysOfWeek((String) input.get("daysOfWeek"));
            }
            if (input.get("endDate") != null) {
                String ed = (String) input.get("endDate");
                rule.setEndDate(ed.isBlank() ? null : LocalDate.parse(ed));
            }
            if (input.get("dayOfMonth") != null) {
                rule.setDayOfMonth(((Number) input.get("dayOfMonth")).intValue());
            }
            if (input.get("monthOfYear") != null) {
                rule.setMonthOfYear(((Number) input.get("monthOfYear")).intValue());
            }
            if (input.get("mode") != null) {
                rule.setMode((String) input.get("mode"));
            }
            ctx.getDsl().update(RECURRENCE_RULES)
                .set(RECURRENCE_RULES.TYPE, rule.getType().name())
                .set(RECURRENCE_RULES.DAYS_OF_WEEK, rule.getDaysOfWeek())
                .set(RECURRENCE_RULES.END_DATE, rule.getEndDate())
                .set(F_DAY_OF_MONTH,  rule.getDayOfMonth())
                .set(F_MONTH_OF_YEAR, rule.getMonthOfYear())
                .set(org.jooq.impl.DSL.field("MODE", String.class),
                     rule.getMode() != null ? rule.getMode() : "FIXED")
                .where(RECURRENCE_RULES.ID.eq(ruleId))
                .execute();
            ctx.getRecurrenceService().propagateRuleChange(ruleId);
            ctx.getRecurrenceService().generateUpcoming(14);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("id",          rule.getId());
            result.put("type",        rule.getType().name());
            result.put("daysOfWeek",  rule.getDaysOfWeek());
            result.put("endDate",     rule.getEndDate() != null ? rule.getEndDate().toString() : null);
            result.put("dayOfMonth",  rule.getDayOfMonth());
            result.put("monthOfYear", rule.getMonthOfYear());
            result.put("mode",        rule.getMode() != null ? rule.getMode() : "FIXED");
            return toJson(result);
        } catch (Exception e) { return error(e); }
    }

    // ── Pomodoro ──────────────────────────────────────────────────────────────

    public void logPomodoro() {
        log.debug("logPomodoro called from JS (anonymous session, no-op stub)");
    }

    /**
     * Starts a new Pomodoro session optionally linked to a task.
     * @param taskId task to link (0 = anonymous session)
     * @param minutes planned duration
     * @return JSON {@code {"sessionId": <id>}}
     */
    public String startPomodoroSession(int taskId, int minutes) {
        try {
            com.nexus.model.PomodoroSession session;
            if (taskId > 0) {
                com.nexus.model.Task task = ctx.getTaskService().getTaskById((long) taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
                session = ctx.getPomodoroService().startSession(task, minutes);
            } else {
                com.nexus.model.PomodoroSession anon = com.nexus.model.PomodoroSession.builder()
                    .taskId(null)
                    .startedAt(java.time.LocalDateTime.now())
                    .durationMinutes(minutes)
                    .completed(false)
                    .build();
                ctx.getPomodoroSessionRepository().save(anon);
                session = anon;
            }
            return "{\"sessionId\":" + session.getId() + "}";
        } catch (Exception e) { return error(e); }
    }

    /** Marks a session as completed and credits time to the linked task. */
    public void completePomodoroSession(long sessionId) {
        try {
            ctx.getPomodoroSessionRepository().findById(sessionId)
                .ifPresent(ctx.getPomodoroService()::completeSession);
        } catch (Exception e) { log.error("completePomodoroSession {} failed", sessionId, e); }
    }

    /** Abandons an in-progress session (no time credited to the task). */
    public void abandonPomodoroSession(long sessionId) {
        try {
            ctx.getPomodoroSessionRepository().findById(sessionId)
                .ifPresent(ctx.getPomodoroService()::abandonSession);
        } catch (Exception e) { log.error("abandonPomodoroSession {} failed", sessionId, e); }
    }

    /** Returns the count of completed Pomodoro sessions for a task. */
    public int getPomodoroCount(long taskId) {
        try {
            return (int) ctx.getPomodoroSessionRepository().findByTaskId(taskId).stream()
                .filter(com.nexus.model.PomodoroSession::isCompleted)
                .count();
        } catch (Exception e) { return 0; }
    }

    /**
     * Plays a short alarm tone on a daemon thread.
     * Called from JS when the Pomodoro timer reaches zero.
     * JavaFX WebView does not support the Web Audio API, so all sound is synthesised here.
     *
     * @param type "work" → ascending 3-note arpeggio (C5-E5-G5);
     *             "break" → descending 2-note cue (E5-C5)
     */
    public void playAlarm(String type) {
        Thread t = new Thread(() -> {
            try {
                float[] freqs = "work".equals(type)
                    ? new float[]{523.25f, 659.25f, 783.99f}   // C5, E5, G5
                    : new float[]{659.25f, 523.25f};             // E5, C5

                AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt, 8192);
                line.start();

                for (float freq : freqs) {
                    int samples = (int)(44100 * 0.45);   // ~450 ms per note
                    byte[] buf = new byte[samples * 2];
                    for (int i = 0; i < samples; i++) {
                        double angle = 2 * Math.PI * freq * i / 44100.0;
                        // simple trapezoid envelope: 20 ms attack, 60 ms release
                        double attack  = Math.min(1.0, i / 882.0);
                        double release = Math.min(1.0, (samples - i) / 2646.0);
                        short s = (short)(Short.MAX_VALUE * 0.55 * Math.sin(angle) * attack * release);
                        buf[i * 2]     = (byte)(s & 0xFF);
                        buf[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
                    }
                    line.write(buf, 0, buf.length);
                    // 80 ms silence between notes
                    line.write(new byte[44100 / 1000 * 80 * 2], 0, 44100 / 1000 * 80 * 2);
                }

                line.drain();
                line.close();
            } catch (Exception e) {
                log.warn("playAlarm failed: {}", e.getMessage());
            }
        }, "pomodoro-alarm");
        t.setDaemon(true);
        t.start();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"serialisation failed\"}"; }
    }

    private String error(Exception e) {
        log.error("Bridge error: {}", e.getMessage(), e);
        return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
    }
}

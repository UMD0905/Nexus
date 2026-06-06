package com.nexus.ui.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import com.nexus.model.Goal;
import com.nexus.service.GoalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

import static com.nexus.db.Tables.*;

/**
 * Bridge methods for goal-related operations.
 */
public class GoalBridge {

    private static final Logger log = LoggerFactory.getLogger(GoalBridge.class);

    private final AppContext ctx;
    private final ObjectMapper json;

    public GoalBridge(AppContext ctx) {
        this.ctx  = ctx;
        this.json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String getGoals() {
        try {
            GoalService gs = ctx.getGoalService();
            return toJson(gs.getAllGoals().stream()
                .map(g -> BridgeDtos.goalDto(ctx, g, gs.getProgress(g))).toList());
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
            if (input.categoryIds != null && !input.categoryIds.isEmpty()) {
                ctx.getGoalRepository().setGoalCategories(saved.getId(), input.categoryIds);
            }
            return toJson(BridgeDtos.goalDto(ctx, saved, 0.0));
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
            if (input.categoryIds != null) {
                ctx.getGoalRepository().setGoalCategories(existing.getId(), input.categoryIds);
            }
            return toJson(BridgeDtos.goalDto(ctx, existing, gs.getProgress(existing)));
        } catch (Exception e) { return error(e); }
    }

    public void updateGoalStatus(int id, String status) {
        try {
            GoalService gs = ctx.getGoalService();
            switch (status) {
                case "COMPLETED" -> gs.completeGoal(id);
                case "ABANDONED" -> gs.abandonGoal(id);
                case "ACTIVE"    -> gs.reactivateGoal(id);
                default -> log.warn("Unknown goal status: {}", status);
            }
        } catch (Exception e) { log.error("updateGoalStatus failed", e); }
    }

    public void deleteGoal(int id) {
        try { ctx.getGoalService().deleteGoal(id); }
        catch (Exception e) { log.error("deleteGoal failed", e); }
    }

    @SuppressWarnings("unchecked")
    public void setGoalCategories(long goalId, String categoryIdsJson) {
        try {
            List<Number> ids = json.readValue(categoryIdsJson, List.class);
            List<Long> catIds = ids.stream().map(Number::longValue).toList();
            ctx.getGoalRepository().setGoalCategories(goalId, catIds);
            if (!catIds.isEmpty()) {
                ctx.getDsl().update(GOALS)
                    .set(GOALS.CATEGORY_ID, catIds.get(0))
                    .where(GOALS.ID.eq(goalId))
                    .execute();
            }
        } catch (Exception e) { log.error("setGoalCategories failed", e); }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"serialisation failed\"}"; }
    }

    private String error(Exception e) {
        log.error("Bridge error: {}", e.getMessage(), e);
        try {
            return json.writeValueAsString(java.util.Map.of("error",
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        } catch (Exception ex) {
            return "{\"error\":\"unknown error\"}";
        }
    }
}

package com.nexus.ui.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.config.AppContext;
import com.nexus.model.Project;
import com.nexus.model.enums.ProjectStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Bridge methods for Project CRUD.
 * Accessible from JS as {@code nexusBridge.projects}.
 */
public class ProjectBridge {

    private static final Logger log = LoggerFactory.getLogger(ProjectBridge.class);

    private final AppContext ctx;
    private final ObjectMapper json;

    public ProjectBridge(AppContext ctx) {
        this.ctx  = ctx;
        this.json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String getProjects() {
        try {
            return toJson(ctx.getProjectService().getAllProjects().stream()
                .map(BridgeDtos::projectDto).toList());
        } catch (Exception e) { return error(e); }
    }

    public String getProjectsByCategory(long categoryId) {
        try {
            return toJson(ctx.getProjectService().getProjectsByCategory(categoryId).stream()
                .map(BridgeDtos::projectDto).toList());
        } catch (Exception e) { return error(e); }
    }

    @SuppressWarnings("unchecked")
    public String createProject(String projJson) {
        try {
            Map<String, Object> input = json.readValue(projJson, Map.class);
            Project p = Project.builder()
                .name((String) input.get("name"))
                .description((String) input.getOrDefault("description", ""))
                .categoryId(input.get("categoryId") instanceof Number n ? n.longValue() : null)
                .color((String) input.getOrDefault("color", "#4A90D9"))
                .startDate(input.get("startDate") instanceof String s && !s.isBlank()
                    ? LocalDate.parse(s) : null)
                .dueDate(input.get("dueDate") instanceof String s && !s.isBlank()
                    ? LocalDate.parse(s) : null)
                .status(ProjectStatus.ACTIVE)
                .build();
            return toJson(BridgeDtos.projectDto(ctx.getProjectService().createProject(p)));
        } catch (Exception e) { return error(e); }
    }

    @SuppressWarnings("unchecked")
    public String updateProject(String projJson) {
        try {
            Map<String, Object> input = json.readValue(projJson, Map.class);
            long id = ((Number) input.get("id")).longValue();
            Project existing = ctx.getProjectService().getProjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
            if (input.containsKey("name"))        existing.setName((String) input.get("name"));
            if (input.containsKey("description")) existing.setDescription((String) input.get("description"));
            if (input.containsKey("categoryId"))  existing.setCategoryId(
                input.get("categoryId") instanceof Number n ? n.longValue() : null);
            if (input.containsKey("color"))       existing.setColor((String) input.get("color"));
            if (input.containsKey("status"))      existing.setStatus(ProjectStatus.valueOf((String) input.get("status")));
            if (input.get("dueDate") instanceof String s)
                existing.setDueDate(s.isBlank() ? null : LocalDate.parse(s));
            if (input.get("startDate") instanceof String s)
                existing.setStartDate(s.isBlank() ? null : LocalDate.parse(s));
            return toJson(BridgeDtos.projectDto(ctx.getProjectService().updateProject(existing)));
        } catch (Exception e) { return error(e); }
    }

    public void deleteProject(long id) {
        try { ctx.getProjectService().deleteProject(id); }
        catch (Exception e) { log.error("deleteProject {} failed", id, e); }
    }

    /** Returns the count of tasks linked to a project (active only). */
    public int getProjectTaskCount(long projectId) {
        try {
            return (int) ctx.getTaskService()
                .getTasks(com.nexus.model.TaskFilter.builder().showArchived(false).build())
                .stream()
                .filter(t -> projectId == (t.getProjectId() != null ? t.getProjectId() : -1))
                .count();
        } catch (Exception e) { return 0; }
    }

    /** Returns the % of tasks that are DONE for a project (0–100). */
    public int getProjectProgress(long projectId) {
        try {
            List<com.nexus.model.Task> tasks = ctx.getTaskService()
                .getTasks(com.nexus.model.TaskFilter.builder().showArchived(false).build())
                .stream()
                .filter(t -> projectId == (t.getProjectId() != null ? t.getProjectId() : -1))
                .toList();
            if (tasks.isEmpty()) return 0;
            long done = tasks.stream()
                .filter(t -> t.getStatus() == com.nexus.model.enums.TaskStatus.DONE)
                .count();
            return (int) (done * 100 / tasks.size());
        } catch (Exception e) { return 0; }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"serialisation failed\"}"; }
    }

    private String error(Exception e) {
        log.error("ProjectBridge error: {}", e.getMessage(), e);
        return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
    }
}

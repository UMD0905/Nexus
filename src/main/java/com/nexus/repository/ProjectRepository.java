package com.nexus.repository;

import com.nexus.model.Project;
import com.nexus.model.enums.ProjectStatus;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.nexus.db.Tables.PROJECTS;

/** Data-access layer for {@link Project}. */
public class ProjectRepository {

    private final DSLContext dsl;

    public ProjectRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<Project> findAll() {
        return dsl.selectFrom(PROJECTS)
            .orderBy(PROJECTS.NAME.asc())
            .fetch()
            .map(this::recordToProject);
    }

    public List<Project> findByCategoryId(long categoryId) {
        return dsl.selectFrom(PROJECTS)
            .where(PROJECTS.CATEGORY_ID.eq(categoryId))
            .orderBy(PROJECTS.NAME.asc())
            .fetch()
            .map(this::recordToProject);
    }

    public Optional<Project> findById(long id) {
        return dsl.selectFrom(PROJECTS)
            .where(PROJECTS.ID.eq(id))
            .fetchOptional()
            .map(this::recordToProject);
    }

    public Project save(Project project) {
        var record = dsl.newRecord(PROJECTS);
        record.setName(project.getName());
        record.setDescription(project.getDescription());
        record.setCategoryId(project.getCategoryId());
        record.setColor(project.getColor());
        record.setStartDate(project.getStartDate());
        record.setDueDate(project.getDueDate());
        record.setStatus(project.getStatus().name());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.store();
        project.setId(record.getId());
        return project;
    }

    public Project update(Project project) {
        dsl.update(PROJECTS)
            .set(PROJECTS.NAME, project.getName())
            .set(PROJECTS.DESCRIPTION, project.getDescription())
            .set(PROJECTS.CATEGORY_ID, project.getCategoryId())
            .set(PROJECTS.COLOR, project.getColor())
            .set(PROJECTS.START_DATE, project.getStartDate())
            .set(PROJECTS.DUE_DATE, project.getDueDate())
            .set(PROJECTS.STATUS, project.getStatus().name())
            .set(PROJECTS.UPDATED_AT, LocalDateTime.now())
            .where(PROJECTS.ID.eq(project.getId()))
            .execute();
        return project;
    }

    public void delete(long id) {
        dsl.deleteFrom(PROJECTS).where(PROJECTS.ID.eq(id)).execute();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Project recordToProject(org.jooq.Record r) {
        return Project.builder()
            .id(r.get(PROJECTS.ID))
            .name(r.get(PROJECTS.NAME))
            .description(r.get(PROJECTS.DESCRIPTION))
            .categoryId(r.get(PROJECTS.CATEGORY_ID))
            .color(r.get(PROJECTS.COLOR))
            .startDate(r.get(PROJECTS.START_DATE))
            .dueDate(r.get(PROJECTS.DUE_DATE))
            .status(ProjectStatus.valueOf(r.get(PROJECTS.STATUS)))
            .createdAt(r.get(PROJECTS.CREATED_AT))
            .updatedAt(r.get(PROJECTS.UPDATED_AT))
            .build();
    }
}

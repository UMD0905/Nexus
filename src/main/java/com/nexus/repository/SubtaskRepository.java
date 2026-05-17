package com.nexus.repository;

import com.nexus.model.Subtask;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;

import static com.nexus.db.Tables.SUBTASKS;

/** Data-access layer for {@link Subtask} checklist items. */
public class SubtaskRepository {

    private final DSLContext dsl;

    public SubtaskRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<Subtask> findByTaskId(long taskId) {
        return dsl.selectFrom(SUBTASKS)
            .where(SUBTASKS.TASK_ID.eq(taskId))
            .orderBy(SUBTASKS.POSITION.asc())
            .fetch()
            .map(r -> Subtask.builder()
                .id(r.getId())
                .taskId(r.getTaskId())
                .title(r.getTitle())
                .completed(Boolean.TRUE.equals(r.getCompleted()))
                .position(r.getPosition())
                .createdAt(r.getCreatedAt())
                .build());
    }

    public Subtask save(Subtask subtask) {
        var record = dsl.newRecord(SUBTASKS);
        record.setTaskId(subtask.getTaskId());
        record.setTitle(subtask.getTitle());
        record.setCompleted(subtask.isCompleted());
        record.setPosition(subtask.getPosition());
        record.setCreatedAt(LocalDateTime.now());
        record.store();
        subtask.setId(record.getId());
        return subtask;
    }

    public void setCompleted(long subtaskId, boolean completed) {
        dsl.update(SUBTASKS)
            .set(SUBTASKS.COMPLETED, completed)
            .where(SUBTASKS.ID.eq(subtaskId))
            .execute();
    }

    public void delete(long subtaskId) {
        dsl.deleteFrom(SUBTASKS).where(SUBTASKS.ID.eq(subtaskId)).execute();
    }

    public void deleteByTaskId(long taskId) {
        dsl.deleteFrom(SUBTASKS).where(SUBTASKS.TASK_ID.eq(taskId)).execute();
    }
}

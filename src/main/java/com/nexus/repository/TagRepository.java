package com.nexus.repository;

import com.nexus.model.Tag;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static com.nexus.db.Tables.TAGS;
import static com.nexus.db.Tables.TASK_TAGS;

/** Data-access layer for {@link Tag}. */
public class TagRepository {

    private final DSLContext dsl;

    public TagRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<Tag> findAll() {
        return dsl.selectFrom(TAGS)
            .orderBy(TAGS.NAME.asc())
            .fetch()
            .map(r -> Tag.builder()
                .id(r.getId())
                .name(r.getName())
                .color(r.getColor())
                .build());
    }

    public List<Tag> findByTaskId(long taskId) {
        return dsl.select(TAGS.fields())
            .from(TAGS)
            .join(TASK_TAGS).on(TAGS.ID.eq(TASK_TAGS.TAG_ID))
            .where(TASK_TAGS.TASK_ID.eq(taskId))
            .fetch()
            .map(r -> Tag.builder()
                .id(r.get(TAGS.ID))
                .name(r.get(TAGS.NAME))
                .color(r.get(TAGS.COLOR))
                .build());
    }

    public Optional<Tag> findById(long id) {
        return dsl.selectFrom(TAGS)
            .where(TAGS.ID.eq(id))
            .fetchOptional()
            .map(r -> Tag.builder()
                .id(r.getId())
                .name(r.getName())
                .color(r.getColor())
                .build());
    }

    public Tag save(Tag tag) {
        var record = dsl.newRecord(TAGS);
        record.setName(tag.getName());
        record.setColor(tag.getColor());
        record.store();
        tag.setId(record.getId());
        return tag;
    }

    public void addTagToTask(long taskId, long tagId) {
        // INSERT IGNORE equivalent — skip if already linked
        dsl.insertInto(TASK_TAGS)
            .set(TASK_TAGS.TASK_ID, taskId)
            .set(TASK_TAGS.TAG_ID, tagId)
            .onDuplicateKeyIgnore()
            .execute();
    }

    public void removeTagFromTask(long taskId, long tagId) {
        dsl.deleteFrom(TASK_TAGS)
            .where(TASK_TAGS.TASK_ID.eq(taskId).and(TASK_TAGS.TAG_ID.eq(tagId)))
            .execute();
    }

    public void removeAllTagsFromTask(long taskId) {
        dsl.deleteFrom(TASK_TAGS).where(TASK_TAGS.TASK_ID.eq(taskId)).execute();
    }

    public void delete(long id) {
        dsl.deleteFrom(TAGS).where(TAGS.ID.eq(id)).execute();
    }
}

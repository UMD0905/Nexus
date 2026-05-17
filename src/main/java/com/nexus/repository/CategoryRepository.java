package com.nexus.repository;

import com.nexus.model.Category;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.nexus.db.Tables.CATEGORIES;

/**
 * Data-access layer for {@link Category} (life areas).
 * Uses the JOOQ-generated {@code CATEGORIES} table reference.
 *
 * <p><b>Note for first-time setup:</b> if IntelliJ shows red errors on
 * {@code com.nexus.db.Tables}, run {@code mvn generate-sources} once.
 */
public class CategoryRepository {

    private static final Logger log = LoggerFactory.getLogger(CategoryRepository.class);

    private final DSLContext dsl;

    public CategoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Returns all categories ordered by their display position. */
    public List<Category> findAll() {
        return dsl.selectFrom(CATEGORIES)
            .orderBy(CATEGORIES.POSITION.asc())
            .fetch()
            .map(this::recordToCategory);
    }

    public Optional<Category> findById(long id) {
        return dsl.selectFrom(CATEGORIES)
            .where(CATEGORIES.ID.eq(id))
            .fetchOptional()
            .map(this::recordToCategory);
    }

    public Category save(Category category) {
        var record = dsl.newRecord(CATEGORIES);
        record.setName(category.getName());
        record.setColor(category.getColor());
        record.setIcon(category.getIcon());
        record.setPosition(category.getPosition());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.store();
        category.setId(record.getId());
        log.debug("Saved category '{}' with id={}", category.getName(), category.getId());
        return category;
    }

    public Category update(Category category) {
        dsl.update(CATEGORIES)
            .set(CATEGORIES.NAME, category.getName())
            .set(CATEGORIES.COLOR, category.getColor())
            .set(CATEGORIES.ICON, category.getIcon())
            .set(CATEGORIES.POSITION, category.getPosition())
            .set(CATEGORIES.UPDATED_AT, LocalDateTime.now())
            .where(CATEGORIES.ID.eq(category.getId()))
            .execute();
        return category;
    }

    public void delete(long id) {
        dsl.deleteFrom(CATEGORIES).where(CATEGORIES.ID.eq(id)).execute();
        log.debug("Deleted category id={}", id);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Category recordToCategory(org.jooq.Record r) {
        return Category.builder()
            .id(r.get(CATEGORIES.ID))
            .name(r.get(CATEGORIES.NAME))
            .color(r.get(CATEGORIES.COLOR))
            .icon(r.get(CATEGORIES.ICON))
            .position(r.get(CATEGORIES.POSITION))
            .createdAt(r.get(CATEGORIES.CREATED_AT))
            .updatedAt(r.get(CATEGORIES.UPDATED_AT))
            .build();
    }
}

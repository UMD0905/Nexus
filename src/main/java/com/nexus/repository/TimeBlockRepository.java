package com.nexus.repository;

import com.nexus.model.TimeBlock;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.nexus.db.Tables.TIME_BLOCKS;

/**
 * Data-access layer for {@link TimeBlock}.
 */
public class TimeBlockRepository {

    private static final Logger log = LoggerFactory.getLogger(TimeBlockRepository.class);

    private final DSLContext dsl;

    public TimeBlockRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<TimeBlock> findByDate(LocalDate date) {
        return dsl.selectFrom(TIME_BLOCKS)
            .where(TIME_BLOCKS.BLOCK_DATE.eq(date))
            .orderBy(TIME_BLOCKS.START_TIME.asc())
            .fetch()
            .map(this::recordToBlock);
    }

    public List<TimeBlock> findByDateRange(LocalDate from, LocalDate to) {
        return dsl.selectFrom(TIME_BLOCKS)
            .where(TIME_BLOCKS.BLOCK_DATE.between(from, to))
            .orderBy(TIME_BLOCKS.BLOCK_DATE.asc(), TIME_BLOCKS.START_TIME.asc())
            .fetch()
            .map(this::recordToBlock);
    }

    public Optional<TimeBlock> findById(long id) {
        return dsl.selectFrom(TIME_BLOCKS)
            .where(TIME_BLOCKS.ID.eq(id))
            .fetchOptional()
            .map(this::recordToBlock);
    }

    public TimeBlock save(TimeBlock block) {
        var record = dsl.newRecord(TIME_BLOCKS);
        applyToRecord(block, record);
        record.setCreatedAt(LocalDateTime.now());
        record.store();
        block.setId(record.getId());
        log.debug("Saved time block id={}", block.getId());
        return block;
    }

    public void update(TimeBlock block) {
        dsl.update(TIME_BLOCKS)
            .set(TIME_BLOCKS.TASK_ID,    block.getTaskId())
            .set(TIME_BLOCKS.TITLE,      block.getTitle())
            .set(TIME_BLOCKS.BLOCK_DATE, block.getBlockDate())
            .set(TIME_BLOCKS.START_TIME, block.getStartTime())
            .set(TIME_BLOCKS.END_TIME,   block.getEndTime())
            .set(TIME_BLOCKS.COLOR,      block.getColor())
            .where(TIME_BLOCKS.ID.eq(block.getId()))
            .execute();
    }

    public void delete(long id) {
        dsl.deleteFrom(TIME_BLOCKS).where(TIME_BLOCKS.ID.eq(id)).execute();
    }

    private void applyToRecord(TimeBlock block, org.jooq.Record record) {
        record.set(TIME_BLOCKS.TASK_ID,    block.getTaskId());
        record.set(TIME_BLOCKS.TITLE,      block.getTitle());
        record.set(TIME_BLOCKS.BLOCK_DATE, block.getBlockDate());
        record.set(TIME_BLOCKS.START_TIME, block.getStartTime());
        record.set(TIME_BLOCKS.END_TIME,   block.getEndTime());
        record.set(TIME_BLOCKS.COLOR,      block.getColor());
    }

    private TimeBlock recordToBlock(org.jooq.Record r) {
        return TimeBlock.builder()
            .id(r.get(TIME_BLOCKS.ID))
            .taskId(r.get(TIME_BLOCKS.TASK_ID))
            .title(r.get(TIME_BLOCKS.TITLE))
            .blockDate(r.get(TIME_BLOCKS.BLOCK_DATE))
            .startTime(r.get(TIME_BLOCKS.START_TIME))
            .endTime(r.get(TIME_BLOCKS.END_TIME))
            .color(r.get(TIME_BLOCKS.COLOR))
            .createdAt(r.get(TIME_BLOCKS.CREATED_AT))
            .build();
    }
}

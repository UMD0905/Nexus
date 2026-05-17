package com.nexus.repository;

import com.nexus.model.Streak;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.nexus.db.Tables.STREAKS;

/**
 * Data-access layer for {@link Streak}.
 */
public class StreakRepository {

    private static final Logger log = LoggerFactory.getLogger(StreakRepository.class);

    private final DSLContext dsl;

    public StreakRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<Streak> findAll() {
        return dsl.selectFrom(STREAKS)
            .orderBy(STREAKS.CURRENT_STREAK.desc())
            .fetch()
            .map(this::recordToStreak);
    }

    public Optional<Streak> findByRuleId(long recurrenceRuleId) {
        return dsl.selectFrom(STREAKS)
            .where(STREAKS.RECURRENCE_RULE_ID.eq(recurrenceRuleId))
            .fetchOptional()
            .map(this::recordToStreak);
    }

    public Streak save(Streak streak) {
        var record = dsl.newRecord(STREAKS);
        applyToRecord(streak, record);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.store();
        streak.setId(record.getId());
        return streak;
    }

    public void update(Streak streak) {
        dsl.update(STREAKS)
            .set(STREAKS.CURRENT_STREAK,      streak.getCurrentStreak())
            .set(STREAKS.LONGEST_STREAK,      streak.getLongestStreak())
            .set(STREAKS.LAST_COMPLETED_DATE, streak.getLastCompletedDate())
            .set(STREAKS.UPDATED_AT,          LocalDateTime.now())
            .where(STREAKS.ID.eq(streak.getId()))
            .execute();
    }

    private void applyToRecord(Streak streak, org.jooq.Record record) {
        record.set(STREAKS.RECURRENCE_RULE_ID,  streak.getRecurrenceRuleId());
        record.set(STREAKS.TITLE,               streak.getTitle());
        record.set(STREAKS.CATEGORY_ID,         streak.getCategoryId());
        record.set(STREAKS.CURRENT_STREAK,      streak.getCurrentStreak());
        record.set(STREAKS.LONGEST_STREAK,      streak.getLongestStreak());
        record.set(STREAKS.LAST_COMPLETED_DATE, streak.getLastCompletedDate());
    }

    private Streak recordToStreak(org.jooq.Record r) {
        return Streak.builder()
            .id(r.get(STREAKS.ID))
            .recurrenceRuleId(r.get(STREAKS.RECURRENCE_RULE_ID))
            .title(r.get(STREAKS.TITLE))
            .categoryId(r.get(STREAKS.CATEGORY_ID))
            .currentStreak(r.get(STREAKS.CURRENT_STREAK) != null ? r.get(STREAKS.CURRENT_STREAK) : 0)
            .longestStreak(r.get(STREAKS.LONGEST_STREAK) != null ? r.get(STREAKS.LONGEST_STREAK) : 0)
            .lastCompletedDate(r.get(STREAKS.LAST_COMPLETED_DATE))
            .createdAt(r.get(STREAKS.CREATED_AT))
            .updatedAt(r.get(STREAKS.UPDATED_AT))
            .build();
    }
}

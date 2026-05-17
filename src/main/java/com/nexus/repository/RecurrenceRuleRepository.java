package com.nexus.repository;

import com.nexus.model.RecurrenceRule;
import com.nexus.model.enums.RecurrenceType;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.nexus.db.Tables.RECURRENCE_RULES;

/**
 * Data-access layer for {@link RecurrenceRule}.
 */
public class RecurrenceRuleRepository {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceRuleRepository.class);

    private final DSLContext dsl;

    public RecurrenceRuleRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<RecurrenceRule> findAll() {
        return dsl.selectFrom(RECURRENCE_RULES)
            .fetch()
            .map(this::recordToRule);
    }

    public Optional<RecurrenceRule> findById(long id) {
        return dsl.selectFrom(RECURRENCE_RULES)
            .where(RECURRENCE_RULES.ID.eq(id))
            .fetchOptional()
            .map(this::recordToRule);
    }

    private RecurrenceRule recordToRule(org.jooq.Record r) {
        return RecurrenceRule.builder()
            .id(r.get(RECURRENCE_RULES.ID))
            .type(RecurrenceType.valueOf(r.get(RECURRENCE_RULES.TYPE)))
            .daysOfWeek(r.get(RECURRENCE_RULES.DAYS_OF_WEEK))
            .intervalVal(r.get(RECURRENCE_RULES.INTERVAL_VAL) != null
                ? r.get(RECURRENCE_RULES.INTERVAL_VAL) : 1)
            .endDate(r.get(RECURRENCE_RULES.END_DATE))
            .build();
    }
}

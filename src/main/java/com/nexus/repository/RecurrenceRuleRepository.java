package com.nexus.repository;

import com.nexus.model.RecurrenceRule;
import com.nexus.model.enums.RecurrenceType;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
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

    /** Raw typed fields for columns added in V10 (not yet in JOOQ generated schema). */
    private static final Field<Integer> DAY_OF_MONTH  = DSL.field("DAY_OF_MONTH",  Integer.class);
    private static final Field<Integer> MONTH_OF_YEAR = DSL.field("MONTH_OF_YEAR", Integer.class);
    /** mode column added in V12 (FIXED | AFTER_COMPLETION). */
    private static final Field<String>  MODE           = DSL.field("MODE", String.class);

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

    public RecurrenceRule save(RecurrenceRule rule) {
        var record = dsl.newRecord(RECURRENCE_RULES);
        record.set(RECURRENCE_RULES.TYPE,         rule.getType().name());
        record.set(RECURRENCE_RULES.DAYS_OF_WEEK, rule.getDaysOfWeek());
        record.set(RECURRENCE_RULES.INTERVAL_VAL, rule.getIntervalVal() > 0 ? rule.getIntervalVal() : 1);
        record.set(RECURRENCE_RULES.END_DATE,     rule.getEndDate());
        record.store();
        rule.setId(record.getId());
        // DAY_OF_MONTH / MONTH_OF_YEAR / MODE added in V10/V12 — not in generated schema yet
        dsl.update(RECURRENCE_RULES)
            .set(DAY_OF_MONTH,  rule.getDayOfMonth())
            .set(MONTH_OF_YEAR, rule.getMonthOfYear())
            .set(MODE, rule.getMode() != null ? rule.getMode() : "FIXED")
            .where(RECURRENCE_RULES.ID.eq(rule.getId()))
            .execute();
        log.debug("Saved recurrence rule type={} days={} id={}",
            rule.getType(), rule.getDaysOfWeek(), rule.getId());
        return rule;
    }

    public void delete(long id) {
        dsl.deleteFrom(RECURRENCE_RULES).where(RECURRENCE_RULES.ID.eq(id)).execute();
    }

    private RecurrenceRule recordToRule(org.jooq.Record r) {
        Integer dayOfMonth  = null;
        Integer monthOfYear = null;
        String  mode        = "FIXED";
        try { dayOfMonth  = r.get(DAY_OF_MONTH);  } catch (Exception ignored) {}
        try { monthOfYear = r.get(MONTH_OF_YEAR); } catch (Exception ignored) {}
        try {
            String m = r.get(MODE);
            if (m != null) mode = m;
        } catch (Exception ignored) {}

        return RecurrenceRule.builder()
            .id(r.get(RECURRENCE_RULES.ID))
            .type(RecurrenceType.valueOf(r.get(RECURRENCE_RULES.TYPE)))
            .daysOfWeek(r.get(RECURRENCE_RULES.DAYS_OF_WEEK))
            .intervalVal(r.get(RECURRENCE_RULES.INTERVAL_VAL) != null
                ? r.get(RECURRENCE_RULES.INTERVAL_VAL) : 1)
            .endDate(r.get(RECURRENCE_RULES.END_DATE))
            .dayOfMonth(dayOfMonth)
            .monthOfYear(monthOfYear)
            .mode(mode)
            .build();
    }
}

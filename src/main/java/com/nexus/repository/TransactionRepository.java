package com.nexus.repository;

import com.nexus.model.Transaction;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data-access layer for {@link Transaction}.
 * Uses raw DSL fields — no generated JOOQ schema for the TRANSACTIONS table.
 */
public class TransactionRepository {

    private static final Logger log = LoggerFactory.getLogger(TransactionRepository.class);

    private static final org.jooq.Table<?> TXN       = DSL.table("TRANSACTIONS");
    private static final org.jooq.Field<Long>        F_ID          = DSL.field("ID",          Long.class);
    private static final org.jooq.Field<String>      F_TYPE        = DSL.field("TYPE",        String.class);
    private static final org.jooq.Field<BigDecimal>  F_AMOUNT      = DSL.field("AMOUNT",      BigDecimal.class);
    private static final org.jooq.Field<String>      F_CURRENCY    = DSL.field("CURRENCY",    String.class);
    private static final org.jooq.Field<String>      F_CATEGORY    = DSL.field("CATEGORY",    String.class);
    private static final org.jooq.Field<String>      F_DESCRIPTION = DSL.field("DESCRIPTION", String.class);
    private static final org.jooq.Field<LocalDate>   F_TXN_DATE    = DSL.field("TXN_DATE",    LocalDate.class);
    private static final org.jooq.Field<LocalDateTime> F_CREATED_AT = DSL.field("CREATED_AT", LocalDateTime.class);

    private final DSLContext dsl;

    public TransactionRepository(DSLContext dsl) { this.dsl = dsl; }

    /** All transactions ordered by txn_date desc, id desc. */
    public List<Transaction> findAll() {
        return dsl.selectFrom(TXN)
                  .orderBy(F_TXN_DATE.desc(), F_ID.desc())
                  .fetch()
                  .map(this::recordToTransaction);
    }

    /** Filter by year/month of txn_date. */
    public List<Transaction> findByMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.plusMonths(1).minusDays(1);
        return dsl.selectFrom(TXN)
                  .where(F_TXN_DATE.between(start, end))
                  .orderBy(F_TXN_DATE.desc(), F_ID.desc())
                  .fetch()
                  .map(this::recordToTransaction);
    }

    /** INSERT — returns the transaction with generated id. */
    public Transaction save(Transaction t) {
        var rec = dsl.insertInto(TXN)
                     .set(F_TYPE,        t.getType())
                     .set(F_AMOUNT,      t.getAmount())
                     .set(F_CURRENCY,    t.getCurrency())
                     .set(F_CATEGORY,    t.getCategory())
                     .set(F_DESCRIPTION, t.getDescription())
                     .set(F_TXN_DATE,    t.getTxnDate())
                     .returning(F_ID)
                     .fetchOne();
        if (rec != null) {
            t.setId(rec.get(F_ID));
        }
        log.debug("Saved transaction id={} type={} amount={}", t.getId(), t.getType(), t.getAmount());
        return t;
    }

    /** UPDATE an existing transaction by id. */
    public Transaction update(Transaction t) {
        dsl.update(TXN)
           .set(F_TYPE,        t.getType())
           .set(F_AMOUNT,      t.getAmount())
           .set(F_CURRENCY,    t.getCurrency())
           .set(F_CATEGORY,    t.getCategory())
           .set(F_DESCRIPTION, t.getDescription())
           .set(F_TXN_DATE,    t.getTxnDate())
           .where(F_ID.eq(t.getId()))
           .execute();
        log.debug("Updated transaction id={}", t.getId());
        return t;
    }

    /** DELETE by id. */
    public void delete(long id) {
        dsl.deleteFrom(TXN).where(F_ID.eq(id)).execute();
        log.debug("Deleted transaction id={}", id);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Transaction recordToTransaction(Record r) {
        return Transaction.builder()
                .id(r.get(F_ID))
                .type(r.get(F_TYPE))
                .amount(r.get(F_AMOUNT))
                .currency(r.get(F_CURRENCY))
                .category(r.get(F_CATEGORY))
                .description(r.get(F_DESCRIPTION))
                .txnDate(r.get(F_TXN_DATE))
                .createdAt(r.get(F_CREATED_AT))
                .build();
    }
}

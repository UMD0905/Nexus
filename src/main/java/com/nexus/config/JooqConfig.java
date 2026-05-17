package com.nexus.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;

/**
 * Produces a JOOQ {@link DSLContext} configured for H2.
 *
 * <p>The DSLContext is a lightweight, thread-safe factory for SQL statements.
 * It holds no connection itself — it borrows one from the pool per operation.
 */
public class JooqConfig {

    private JooqConfig() {}

    /**
     * Creates a DSLContext that uses the supplied DataSource for all queries.
     * {@link SQLDialect#H2} enables H2-specific functions (e.g. DATEADD, AUTO_INCREMENT).
     */
    public static DSLContext createDslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.H2);
    }
}

package com.nexus.service;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Provides get/set access to the APP_SETTINGS table.
 *
 * <p>Uses plain JOOQ DSL (no generated code) because the table is created by a
 * Flyway migration that runs after code-generation would have been executed.
 */
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private static final org.jooq.Table<?> TABLE = DSL.table("APP_SETTINGS");
    private static final org.jooq.Field<String>        KEY        = DSL.field("setting_key",   String.class);
    private static final org.jooq.Field<String>        VALUE      = DSL.field("setting_value", String.class);
    private static final org.jooq.Field<LocalDateTime> UPDATED_AT = DSL.field("updated_at",    LocalDateTime.class);

    private final DSLContext dsl;

    public SettingsService(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Returns the raw string value for a key, or empty if not found. */
    public Optional<String> get(String key) {
        String val = dsl.select(VALUE)
                .from(TABLE)
                .where(KEY.eq(key))
                .fetchOne(VALUE);
        return Optional.ofNullable(val);
    }

    /** Returns the value for a key, falling back to {@code defaultVal}. */
    public String getString(String key, String defaultVal) {
        return get(key).orElse(defaultVal);
    }

    /** Returns the value parsed as an int, falling back to {@code defaultVal}. */
    public int getInt(String key, int defaultVal) {
        return get(key).map(v -> {
            try { return Integer.parseInt(v); }
            catch (NumberFormatException e) { return defaultVal; }
        }).orElse(defaultVal);
    }

    /** Returns the value parsed as a boolean, falling back to {@code defaultVal}. */
    public boolean getBoolean(String key, boolean defaultVal) {
        return get(key).map(v -> {
            if ("true".equalsIgnoreCase(v))  return true;
            if ("false".equalsIgnoreCase(v)) return false;
            return defaultVal;
        }).orElse(defaultVal);
    }

    /**
     * Upserts a key/value pair.  H2's MERGE syntax is used for an atomic upsert.
     */
    public void set(String key, String value) {
        // H2-compatible upsert: INSERT … ON DUPLICATE KEY UPDATE or MERGE
        dsl.mergeInto(TABLE, KEY, VALUE, UPDATED_AT)
                .key(KEY)
                .values(key, value, LocalDateTime.now())
                .execute();
        log.debug("Setting saved: {}={}", key, value);
    }

    /** Deletes a setting key entirely (clears any override). */
    public void delete(String key) {
        dsl.deleteFrom(TABLE).where(KEY.eq(key)).execute();
        log.debug("Setting deleted: {}", key);
    }

    /** Returns all settings as an immutable key→value map. */
    public Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();
        dsl.select(KEY, VALUE)
                .from(TABLE)
                .fetch()
                .forEach(r -> result.put(r.get(KEY), r.get(VALUE)));
        return result;
    }
}

package com.nexus.service;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link SettingsService} using a real in-memory H2 database.
 *
 * <p>SettingsService uses JOOQ's mergeInto which cannot be meaningfully mocked;
 * an in-memory H2 instance gives us a faithful execution environment without
 * touching the filesystem.
 */
class SettingsServiceTest {

    private DSLContext dsl;
    private SettingsService service;

    @BeforeEach
    void setUp() throws Exception {
        Connection conn = DriverManager.getConnection(
            "jdbc:h2:mem:settings_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1", "sa", "");

        dsl = DSL.using(conn, SQLDialect.H2);

        // Create the APP_SETTINGS table exactly as the Flyway migration does
        dsl.execute("""
            CREATE TABLE APP_SETTINGS (
                setting_key   VARCHAR(100) PRIMARY KEY,
                setting_value VARCHAR(4000),
                updated_at    TIMESTAMP
            )
            """);

        service = new SettingsService(dsl);
    }

    // ── get / getString ───────────────────────────────────────────────────────

    @Test
    @DisplayName("get returns empty Optional when key does not exist")
    void get_missingKey_returnsEmpty() {
        assertThat(service.get("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("getString returns defaultVal when key does not exist")
    void getString_missingKey_returnsDefault() {
        assertThat(service.getString("missing", "fallback")).isEqualTo("fallback");
    }

    // ── set ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("set stores a value that can be retrieved with get")
    void set_thenGet_returnsStoredValue() {
        service.set("pomodoro_work_min", "25");

        assertThat(service.get("pomodoro_work_min")).contains("25");
    }

    @Test
    @DisplayName("set is an upsert — subsequent set overwrites the value")
    void set_twice_overwritesValue() {
        service.set("week_start_day", "MONDAY");
        service.set("week_start_day", "SUNDAY");

        assertThat(service.get("week_start_day")).contains("SUNDAY");
    }

    // ── getInt ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getInt parses stored integer correctly")
    void getInt_validStoredValue_returnsParsedInt() {
        service.set("reminder_minutes", "30");

        assertThat(service.getInt("reminder_minutes", 15)).isEqualTo(30);
    }

    @Test
    @DisplayName("getInt returns defaultVal for a non-numeric stored value")
    void getInt_nonNumericValue_returnsDefault() {
        service.set("bad_int", "not-a-number");

        assertThat(service.getInt("bad_int", 42)).isEqualTo(42);
    }

    @Test
    @DisplayName("getInt returns defaultVal when key is absent")
    void getInt_missingKey_returnsDefault() {
        assertThat(service.getInt("absent", 10)).isEqualTo(10);
    }

    // ── getBoolean ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBoolean returns true for stored 'true'")
    void getBoolean_storedTrue_returnsTrue() {
        service.set("auto_backup_enabled", "true");

        assertThat(service.getBoolean("auto_backup_enabled", false)).isTrue();
    }

    @Test
    @DisplayName("getBoolean returns false for stored 'false'")
    void getBoolean_storedFalse_returnsFalse() {
        service.set("auto_backup_enabled", "false");

        assertThat(service.getBoolean("auto_backup_enabled", true)).isFalse();
    }

    @Test
    @DisplayName("getBoolean is case-insensitive")
    void getBoolean_upperCaseTrue_returnsTrue() {
        service.set("flag", "TRUE");

        assertThat(service.getBoolean("flag", false)).isTrue();
    }

    @Test
    @DisplayName("getBoolean returns defaultVal when key is absent")
    void getBoolean_missingKey_returnsDefault() {
        assertThat(service.getBoolean("absent", true)).isTrue();
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll returns all stored key-value pairs")
    void getAll_multipleEntries_returnsAll() {
        service.set("key_a", "value_a");
        service.set("key_b", "value_b");

        Map<String, String> all = service.getAll();

        assertThat(all).containsEntry("key_a", "value_a")
                       .containsEntry("key_b", "value_b");
    }

    @Test
    @DisplayName("getAll returns empty map when no settings have been stored")
    void getAll_empty_returnsEmptyMap() {
        assertThat(service.getAll()).isEmpty();
    }
}

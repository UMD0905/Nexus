package com.nexus.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Bootstraps the database connection pool and runs Flyway migrations.
 *
 * <p>Data is stored in {@code ~/.nexus/data/nexus.mv.db} (H2 file format).
 * The directory is created automatically on first launch.
 */
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private DatabaseConfig() {}

    /**
     * Creates and returns a configured HikariCP {@link DataSource}.
     * The H2 database file is created in the user's home directory under {@code .nexus/data/}.
     */
    public static DataSource createDataSource() {
        Path dbDir = resolveDbDirectory();
        String jdbcUrl = buildJdbcUrl(dbDir);

        log.info("Database location: {}", dbDir.resolve("nexus.mv.db"));

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setDriverClassName("org.h2.Driver");

        // Small pool — this is a single-user desktop app
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(30_000);
        cfg.setIdleTimeout(600_000);
        cfg.setMaxLifetime(1_800_000);
        cfg.setPoolName("NexusPool");

        // H2 trace logging off
        cfg.addDataSourceProperty("TRACE_LEVEL_SYSTEM_OUT", "0");

        return new HikariDataSource(cfg);
    }

    /**
     * Runs all pending Flyway migrations against the given data source.
     * Idempotent: already-applied migrations are skipped.
     */
    public static void runMigrations(DataSource dataSource) {
        log.info("Running Flyway migrations...");
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(false)
            .validateOnMigrate(true)
            .load();

        var result = flyway.migrate();
        log.info("Flyway applied {} migration(s)", result.migrationsExecuted);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static Path resolveDbDirectory() {
        Path dir = Path.of(System.getProperty("user.home"), ".nexus", "data");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create database directory: " + dir, e);
        }
        return dir;
    }

    /**
     * Builds the JDBC URL for an H2 file database.
     * Forward slashes are used to keep H2 happy on Windows.
     */
    private static String buildJdbcUrl(Path dbDir) {
        String path = dbDir.resolve("nexus")
                           .toAbsolutePath()
                           .toString()
                           .replace("\\", "/");
        // AUTO_SERVER=FALSE — single process, no TCP mode needed for a desktop app
        return "jdbc:h2:file:" + path + ";DB_CLOSE_ON_EXIT=FALSE";
    }
}

package com.nexus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled auto-backup service.
 *
 * <p>Checks every minute whether an auto-backup is due (enabled + time matches).
 * Rotates the backup directory to keep at most 30 {@code nexus-backup-*.json} files.
 * Also supports an immediate {@link #backupNow()} call from the Settings bridge.
 */
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final int    MAX_BACKUPS = 30;

    private final ExportService   exportService;
    private final SettingsService settingsService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "backup-scheduler");
        t.setDaemon(true);
        return t;
    });

    /** Tracks the last minute a scheduled backup was attempted to avoid double-fire. */
    private int lastScheduledMinute = -1;

    public BackupService(ExportService exportService, SettingsService settingsService) {
        this.exportService   = exportService;
        this.settingsService = settingsService;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::maybeBackup, 60, 60, TimeUnit.SECONDS);
        log.info("Backup service started (checking every 60 s).");
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    /** Triggers an immediate backup regardless of the schedule. */
    public void backupNow() {
        scheduler.submit(() -> doBackup(resolveDir()));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void maybeBackup() {
        try {
            if (!settingsService.getBoolean("auto_backup_enabled", false)) return;

            String timeStr = settingsService.getString("auto_backup_time", "02:00");
            LocalTime target;
            try { target = LocalTime.parse(timeStr); }
            catch (Exception e) { target = LocalTime.of(2, 0); }

            LocalTime now = LocalTime.now();
            int nowMinute = now.getHour() * 60 + now.getMinute();
            int targetMinute = target.getHour() * 60 + target.getMinute();

            if (nowMinute == targetMinute && nowMinute != lastScheduledMinute) {
                lastScheduledMinute = nowMinute;
                doBackup(resolveDir());
            }
        } catch (Exception e) {
            log.error("Backup check failed", e);
        }
    }

    private String resolveDir() {
        String dir = settingsService.getString("auto_backup_dir", "");
        if (dir == null || dir.isBlank()) {
            dir = System.getProperty("user.home") + "/.nexus/backups";
        }
        return dir;
    }

    private void doBackup(String dirPath) {
        try {
            File dir = new File(dirPath);
            dir.mkdirs();
            File out = exportService.exportTo(dir);
            log.info("Backup created: {}", out.getAbsolutePath());
            rotate(dir);
        } catch (Exception e) {
            log.error("Backup failed: {}", e.getMessage(), e);
        }
    }

    /** Keeps only the {@value MAX_BACKUPS} most-recently-modified backups. */
    private void rotate(File dir) {
        File[] backups = dir.listFiles(
            (d, name) -> name.startsWith("nexus-backup-") && name.endsWith(".json"));
        if (backups == null || backups.length <= MAX_BACKUPS) return;
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < backups.length - MAX_BACKUPS; i++) {
            if (backups[i].delete()) {
                log.debug("Rotated old backup: {}", backups[i].getName());
            }
        }
    }
}

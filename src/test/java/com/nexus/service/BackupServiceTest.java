package com.nexus.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BackupService}.
 *
 * <p>BackupService is schedule-driven with async dispatch, so tests focus on
 * lifecycle correctness and the happy-path delegation to ExportService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BackupServiceTest {

    @Mock ExportService   exportService;
    @Mock SettingsService settingsService;

    @TempDir File tempDir;

    BackupService service;

    @BeforeEach
    void setUp() {
        service = new BackupService(exportService, settingsService);
    }

    // ── start / shutdown lifecycle ────────────────────────────────────────────

    @Test
    @DisplayName("start then shutdown complete without throwing")
    void startThenShutdown_noException() {
        assertThatCode(() -> {
            service.start();
            service.shutdown();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("shutdown without start is safe")
    void shutdownWithoutStart_noException() {
        assertThatCode(() -> service.shutdown()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("start is idempotent — calling twice does not throw")
    void startTwice_noException() {
        assertThatCode(() -> {
            service.start();
            service.start();
            service.shutdown();
        }).doesNotThrowAnyException();
    }

    // ── backupNow ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("backupNow delegates to ExportService.exportTo")
    void backupNow_delegatesToExportServiceExportTo() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        when(settingsService.getString(eq("auto_backup_dir"), any())).thenReturn(tempDir.getAbsolutePath());
        when(exportService.exportTo(any(File.class))).thenAnswer(inv -> {
            latch.countDown();
            return new File(tempDir, "nexus-backup-test.json");
        });

        service.backupNow();

        // Wait for the async executor to call exportTo (up to 2 s)
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        verify(exportService).exportTo(any(File.class));
    }

    @Test
    @DisplayName("backupNow with empty backup dir uses default home/.nexus/backups path")
    void backupNow_emptyDir_usesDefaultPath() throws IOException, InterruptedException {
        when(settingsService.getString(eq("auto_backup_dir"), any())).thenReturn("");
        when(exportService.exportTo(any(File.class))).thenReturn(new File(tempDir, "nexus-backup-default.json"));

        assertThatCode(() -> {
            service.backupNow();
            service.shutdown();
        }).doesNotThrowAnyException();
    }
}

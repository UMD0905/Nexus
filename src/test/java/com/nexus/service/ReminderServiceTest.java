package com.nexus.service;

import com.nexus.model.TaskFilter;
import com.nexus.repository.NotificationRepository;
import com.nexus.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ReminderService}.
 *
 * <p>ReminderService uses a ScheduledExecutorService internally.
 * Tests verify callback wiring and service lifecycle; the reminder
 * scan itself is stubbed to return an empty task list so the JavaFX
 * Platform (not initialised in tests) is never reached.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReminderServiceTest {

    @Mock TaskRepository         taskRepository;
    @Mock NotificationService    notificationService;
    @Mock NotificationRepository notificationRepository;

    ReminderService service;

    @BeforeEach
    void setUp() {
        // Return no tasks so the immediate scan does not trigger reminders
        // (which would try to reach JavaFX Platform in a headless test)
        when(taskRepository.findAll(any(TaskFilter.class))).thenReturn(List.of());
        service = new ReminderService(taskRepository, notificationService, notificationRepository);
    }

    // ── setOnNotificationCreated ──────────────────────────────────────────────

    @Test
    @DisplayName("setOnNotificationCreated accepts a non-null callback without throwing")
    void setOnNotificationCreated_nonNullCallback_accepted() {
        assertThatCode(() ->
            service.setOnNotificationCreated(() -> {})).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("setOnNotificationCreated accepts null (clears the callback)")
    void setOnNotificationCreated_null_accepted() {
        assertThatCode(() ->
            service.setOnNotificationCreated(null)).doesNotThrowAnyException();
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

    // ── no notifications when no tasks have reminders configured ──────────────

    @Test
    @DisplayName("no notifications are created when task list is empty")
    void start_emptyTaskList_noNotificationsCreated() {
        service.start();
        service.shutdown();

        verify(notificationService, never()).createReminder(any(), any(), any());
    }
}

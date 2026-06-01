package com.nexus.service;

import com.nexus.model.PomodoroSession;
import com.nexus.model.Task;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.PomodoroSessionRepository;
import com.nexus.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PomodoroServiceTest {

    @Mock PomodoroSessionRepository sessionRepository;
    @Mock TaskRepository            taskRepository;

    PomodoroService service;

    private Task sampleTask() {
        return Task.builder()
            .id(1L).title("Deep work").priority(Priority.HIGH)
            .status(TaskStatus.IN_PROGRESS).actualMinutes(0)
            .build();
    }

    @BeforeEach
    void setUp() {
        service = new PomodoroService(sessionRepository, taskRepository);
    }

    // ── startSession ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("startSession saves a session with the correct task id and duration")
    void startSession_validTask_sessionSavedWithCorrectFields() {
        Task task = sampleTask();
        // save mutates the passed session (sets id from DB) and returns it
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            PomodoroSession s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        PomodoroSession result = service.startSession(task, 25);

        assertThat(result.getTaskId()).isEqualTo(1L);
        assertThat(result.getDurationMinutes()).isEqualTo(25);
        assertThat(result.isCompleted()).isFalse();
        verify(sessionRepository).save(any(PomodoroSession.class));
    }

    @Test
    @DisplayName("startSession stamps startedAt with current time")
    void startSession_stampsStartedAt() {
        Task task = sampleTask();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PomodoroSession result = service.startSession(task, 25);

        assertThat(result.getStartedAt()).isAfterOrEqualTo(before);
    }

    // ── completeSession ───────────────────────────────────────────────────────

    @Test
    @DisplayName("completeSession calls markCompleted on the repository")
    void completeSession_callsMarkCompleted() {
        Task task = sampleTask();
        PomodoroSession session = PomodoroSession.builder()
            .id(2L).taskId(1L).durationMinutes(25)
            .startedAt(LocalDateTime.now().minusMinutes(25))
            .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeSession(session);

        verify(sessionRepository).markCompleted(eq(2L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("completeSession updates the task's actual minutes")
    void completeSession_addsSessionDurationToTask() {
        Task task = Task.builder()
            .id(1L).title("Work").priority(Priority.HIGH)
            .status(TaskStatus.IN_PROGRESS).actualMinutes(50)
            .build();
        PomodoroSession session = PomodoroSession.builder()
            .id(3L).taskId(1L).durationMinutes(25)
            .startedAt(LocalDateTime.now().minusMinutes(25))
            .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeSession(session);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).update(captor.capture());
        assertThat(captor.getValue().getActualMinutes()).isEqualTo(75); // 50 + 25
    }

    @Test
    @DisplayName("completeSession sets completed=true and endedAt on the in-memory session object")
    void completeSession_updatesSessionInMemory() {
        Task task = sampleTask();
        PomodoroSession session = PomodoroSession.builder()
            .id(4L).taskId(1L).durationMinutes(25)
            .startedAt(LocalDateTime.now().minusMinutes(25))
            .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeSession(session);

        assertThat(session.isCompleted()).isTrue();
        assertThat(session.getEndedAt()).isNotNull();
    }

    // ── abandonSession ────────────────────────────────────────────────────────

    @Test
    @DisplayName("abandonSession calls markCompleted without crediting time to task")
    void abandonSession_callsMarkCompletedButDoesNotUpdateTask() {
        PomodoroSession session = PomodoroSession.builder()
            .id(5L).taskId(1L).durationMinutes(25)
            .startedAt(LocalDateTime.now().minusMinutes(10))
            .build();

        service.abandonSession(session);

        verify(sessionRepository).markCompleted(eq(5L), any(LocalDateTime.class));
        verify(taskRepository, never()).update(any());
    }

    // ── getSessionsForTask ────────────────────────────────────────────────────

    @Test
    @DisplayName("getSessionsForTask delegates to repository with task id")
    void getSessionsForTask_delegatesToRepository() {
        PomodoroSession s = PomodoroSession.builder().id(1L).taskId(42L).build();
        when(sessionRepository.findByTaskId(42L)).thenReturn(List.of(s));

        List<PomodoroSession> result = service.getSessionsForTask(42L);

        assertThat(result).containsExactly(s);
        verify(sessionRepository).findByTaskId(42L);
    }
}

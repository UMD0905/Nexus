package com.nexus.service;

import com.nexus.model.Goal;
import com.nexus.model.Task;
import com.nexus.model.enums.TaskStatus;
import com.nexus.model.enums.Priority;
import com.nexus.repository.CategoryRepository;
import com.nexus.repository.GoalRepository;
import com.nexus.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock GoalRepository     goalRepo;
    @Mock TaskRepository     taskRepo;
    @Mock CategoryRepository categoryRepo;

    GoalService service;

    @BeforeEach
    void setUp() {
        service = new GoalService(goalRepo, taskRepo, categoryRepo);
    }

    // ── createGoal ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createGoal rejects blank title")
    void createGoal_blankTitle_throws() {
        Goal g = Goal.builder().title("  ").build();
        assertThatThrownBy(() -> service.createGoal(g))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createGoal sets status ACTIVE and saves")
    void createGoal_valid_savedAsActive() {
        Goal input  = Goal.builder().title("Ship Nexus").build();
        Goal saved  = Goal.builder().id(1L).title("Ship Nexus").status("ACTIVE").build();
        when(goalRepo.save(any())).thenReturn(saved);

        Goal result = service.createGoal(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(goalRepo).save(any(Goal.class));
    }

    // ── checkAutoComplete ─────────────────────────────────────────────────────

    @Test
    @DisplayName("checkAutoComplete completes goal when all tasks are DONE")
    void checkAutoComplete_allDone_completesGoal() {
        long taskId  = 10L;
        long goalId  = 1L;

        Goal goal = Goal.builder().id(goalId).title("Get fit").status("ACTIVE").build();
        Task doneA = task(10L, TaskStatus.DONE);
        Task doneB = task(11L, TaskStatus.DONE);

        when(goalRepo.findGoalIdByTaskId(taskId)).thenReturn(Optional.of(goalId));
        when(goalRepo.findById(goalId)).thenReturn(Optional.of(goal));
        when(goalRepo.findLinkedTaskIds(goalId)).thenReturn(List.of(10L, 11L));
        when(taskRepo.findById(10L)).thenReturn(Optional.of(doneA));
        when(taskRepo.findById(11L)).thenReturn(Optional.of(doneB));

        service.checkAutoComplete(taskId);

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepo).update(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("checkAutoComplete does NOT complete goal when a task is still TODO")
    void checkAutoComplete_notAllDone_doesNotComplete() {
        long taskId = 10L;
        long goalId = 2L;

        Goal goal = Goal.builder().id(goalId).title("Run marathon").status("ACTIVE").build();
        Task done = task(10L, TaskStatus.DONE);
        Task todo = task(12L, TaskStatus.TODO);

        when(goalRepo.findGoalIdByTaskId(taskId)).thenReturn(Optional.of(goalId));
        when(goalRepo.findById(goalId)).thenReturn(Optional.of(goal));
        when(goalRepo.findLinkedTaskIds(goalId)).thenReturn(List.of(10L, 12L));
        when(taskRepo.findById(10L)).thenReturn(Optional.of(done));
        when(taskRepo.findById(12L)).thenReturn(Optional.of(todo));

        service.checkAutoComplete(taskId);

        verify(goalRepo, never()).update(any());
    }

    @Test
    @DisplayName("checkAutoComplete skips already-completed goals")
    void checkAutoComplete_goalAlreadyCompleted_noOp() {
        long taskId = 5L;
        long goalId = 3L;

        Goal goal = Goal.builder().id(goalId).title("Already done").status("COMPLETED").build();
        when(goalRepo.findGoalIdByTaskId(taskId)).thenReturn(Optional.of(goalId));
        when(goalRepo.findById(goalId)).thenReturn(Optional.of(goal));

        service.checkAutoComplete(taskId);

        verify(goalRepo, never()).findLinkedTaskIds(anyLong());
        verify(goalRepo, never()).update(any());
    }

    @Test
    @DisplayName("checkAutoComplete ignores soft-deleted recurring instances")
    void checkAutoComplete_softDeletedInstanceIgnored_completesGoal() {
        long taskId = 20L;
        long goalId = 4L;

        Goal goal = Goal.builder().id(goalId).title("Daily habit").status("ACTIVE").build();
        Task done     = task(20L, TaskStatus.DONE);
        // Soft-deleted recurring tombstone — must not count as "not done"
        Task tombstone = Task.builder().id(21L).title("old instance")
            .priority(Priority.LOW)
            .status(TaskStatus.CANCELLED)
            .archived(true)
            .recurrenceRuleId(99L)
            .build();

        when(goalRepo.findGoalIdByTaskId(taskId)).thenReturn(Optional.of(goalId));
        when(goalRepo.findById(goalId)).thenReturn(Optional.of(goal));
        when(goalRepo.findLinkedTaskIds(goalId)).thenReturn(List.of(20L, 21L));
        when(taskRepo.findById(20L)).thenReturn(Optional.of(done));
        when(taskRepo.findById(21L)).thenReturn(Optional.of(tombstone));

        service.checkAutoComplete(taskId);

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepo).update(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("COMPLETED");
    }

    // ── getProgress ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProgress returns 0 for a goal with no tasks")
    void getProgress_noTasks_zero() {
        Goal g = Goal.builder().id(1L).title("Empty").build();
        g.setTasks(List.of());
        assertThat(service.getProgress(g)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getProgress returns correct ratio")
    void getProgress_partiallyDone_correctRatio() {
        Goal g = Goal.builder().id(1L).title("Partial").build();
        g.setTasks(List.of(
            task(1L, TaskStatus.DONE),
            task(2L, TaskStatus.TODO),
            task(3L, TaskStatus.TODO),
            task(4L, TaskStatus.DONE)
        ));
        assertThat(service.getProgress(g)).isEqualTo(0.5);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Task task(long id, TaskStatus status) {
        return Task.builder()
            .id(id)
            .title("task-" + id)
            .priority(Priority.MEDIUM)
            .status(status)
            .build();
    }
}

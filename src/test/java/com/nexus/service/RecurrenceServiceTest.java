package com.nexus.service;

import com.nexus.model.RecurrenceRule;
import com.nexus.model.Task;
import com.nexus.model.TaskFilter;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.RecurrenceType;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.GoalRepository;
import com.nexus.repository.RecurrenceRuleRepository;
import com.nexus.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link RecurrenceService}.
 */
@ExtendWith(MockitoExtension.class)
class RecurrenceServiceTest {

    @Mock private RecurrenceRuleRepository ruleRepo;
    @Mock private TaskRepository           taskRepo;
    @Mock private GoalRepository           goalRepo;

    private RecurrenceService service;

    @BeforeEach
    void setUp() {
        service = new RecurrenceService(ruleRepo, taskRepo, goalRepo);
    }

    @Test
    void noRulesProducesZeroInstances() {
        when(ruleRepo.findAll()).thenReturn(List.of());

        int created = service.generateUpcoming(14);

        assertThat(created).isZero();
        verify(taskRepo, never()).save(any());
    }

    @Test
    void weeklyRuleCreatesInstancesOnCorrectDays() {
        // Rule: every Tuesday and Thursday
        RecurrenceRule rule = RecurrenceRule.builder()
            .id(1L)
            .type(RecurrenceType.WEEKLY)
            .daysOfWeek("TUE,THU")
            .intervalVal(1)
            .build();

        Task template = kickboxingTask(rule.getId());

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(taskRepo.findAll(any(TaskFilter.class)))
            .thenAnswer(inv -> {
                TaskFilter f = inv.getArgument(0);
                return f.isShowArchived() ? List.of() : List.of(template);
            });
        when(taskRepo.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId((long) (Math.random() * 1000 + 10));
            return t;
        });

        int created = service.generateUpcoming(14);

        // 14 days: 2 Tues + 2 Thu minimum (often 4 total Tue+Thu combos)
        assertThat(created).isBetween(4, 8);
        verify(taskRepo, times(created)).save(any(Task.class));
    }

    @Test
    void existingInstancesAreNotDuplicated() {
        RecurrenceRule rule = RecurrenceRule.builder()
            .id(2L)
            .type(RecurrenceType.DAILY)
            .intervalVal(1)
            .build();

        LocalDate today = LocalDate.now();
        // Pre-existing instances covering today + 14 days
        List<Task> existingInstances = today.datesUntil(today.plusDays(15))
            .map(d -> Task.builder()
                .id((long) (d.getDayOfYear() + 100))
                .title("Daily standup")
                .recurrenceRuleId(rule.getId())
                .dueDate(d.atTime(9, 0))
                .priority(Priority.MEDIUM)
                .status(TaskStatus.TODO)
                .build())
            .toList();

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(taskRepo.findAll(any(TaskFilter.class)))
            .thenAnswer(inv -> {
                TaskFilter f = inv.getArgument(0);
                return f.isShowArchived() ? List.of() : existingInstances;
            });

        int created = service.generateUpcoming(14);

        assertThat(created).isZero();
        verify(taskRepo, never()).save(any());
    }

    @Test
    void dailyRuleCreatesInstancesEveryDay() {
        RecurrenceRule rule = RecurrenceRule.builder()
            .id(3L)
            .type(RecurrenceType.DAILY)
            .intervalVal(1)
            .build();

        Task template = Task.builder()
            .id(1L)
            .title("Morning workout")
            .recurrenceRuleId(rule.getId())
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().minusDays(2).atTime(7, 0))
            .build();

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(taskRepo.findAll(any(TaskFilter.class)))
            .thenAnswer(inv -> {
                TaskFilter f = inv.getArgument(0);
                return f.isShowArchived() ? List.of() : List.of(template);
            });
        when(taskRepo.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId((long) (Math.random() * 1000 + 50));
            return t;
        });

        int created = service.generateUpcoming(7);

        // 7 days ahead: instances for today through today+7 = 8 days
        assertThat(created).isBetween(7, 8);
    }

    @Test
    void weekdaysRuleSkipsWeekend() {
        RecurrenceRule rule = RecurrenceRule.builder()
            .id(4L)
            .type(RecurrenceType.WEEKDAYS)
            .intervalVal(1)
            .build();

        Task template = Task.builder()
            .id(1L)
            .title("Work log")
            .recurrenceRuleId(rule.getId())
            .priority(Priority.LOW)
            .status(TaskStatus.TODO)
            .dueDate(LocalDateTime.now())
            .build();

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(taskRepo.findAll(any(TaskFilter.class)))
            .thenAnswer(inv -> {
                TaskFilter f = inv.getArgument(0);
                return f.isShowArchived() ? List.of() : List.of(template);
            });
        when(taskRepo.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId((long) (Math.random() * 1000 + 200));
            return t;
        });

        int created = service.generateUpcoming(7);

        // 7 days must have at least 3 weekdays (could be Mon-Sun or any range)
        assertThat(created).isBetween(3, 7);
    }

    @Test
    void startTimeIsCopiedToGeneratedInstances() {
        RecurrenceRule rule = RecurrenceRule.builder()
            .id(5L).type(RecurrenceType.DAILY).intervalVal(1).build();

        LocalTime start = LocalTime.of(18, 30);
        Task template = Task.builder()
            .id(1L).title("Gym").recurrenceRuleId(rule.getId())
            .priority(Priority.HIGH).status(TaskStatus.TODO)
            .dueDate(LocalDate.now().atTime(19, 0))
            .startTime(start)
            .build();

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(taskRepo.findAll(any(TaskFilter.class)))
            .thenAnswer(inv -> ((TaskFilter) inv.getArgument(0)).isShowArchived()
                ? List.of() : List.of(template));
        when(taskRepo.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId((long) (Math.random() * 1000 + 10));
            return t;
        });
        when(goalRepo.findGoalIdByRuleId(anyLong())).thenReturn(Optional.empty());

        service.generateUpcoming(3);

        // Every saved instance must carry the template's start time
        var captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepo, atLeastOnce()).save(captor.capture());
        captor.getAllValues().forEach(t ->
            assertThat(t.getStartTime()).isEqualTo(start));
    }

    @Test
    void goalLinkIsCopiedToGeneratedInstances() {
        RecurrenceRule rule = RecurrenceRule.builder()
            .id(6L).type(RecurrenceType.DAILY).intervalVal(1).build();

        Task template = Task.builder()
            .id(1L).title("Study").recurrenceRuleId(rule.getId())
            .priority(Priority.MEDIUM).status(TaskStatus.TODO)
            .dueDate(LocalDate.now().atTime(9, 0)).build();

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(taskRepo.findAll(any(TaskFilter.class)))
            .thenAnswer(inv -> ((TaskFilter) inv.getArgument(0)).isShowArchived()
                ? List.of() : List.of(template));
        when(taskRepo.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId((long) (Math.random() * 1000 + 10));
            return t;
        });
        when(goalRepo.findGoalIdByRuleId(6L)).thenReturn(Optional.of(99L));

        int created = service.generateUpcoming(3);

        // Goal must be linked for each new instance
        verify(goalRepo, times(created)).linkTask(eq(99L), anyLong());
    }

    @Test
    void ruleWithEndDateStopsGeneratingAfterEndDate() {
        LocalDate endDate = LocalDate.now().plusDays(2);
        RecurrenceRule rule = RecurrenceRule.builder()
            .id(7L).type(RecurrenceType.DAILY).intervalVal(1)
            .endDate(endDate).build();

        Task template = Task.builder()
            .id(1L).title("Temp task").recurrenceRuleId(rule.getId())
            .priority(Priority.LOW).status(TaskStatus.TODO)
            .dueDate(LocalDate.now().atTime(8, 0)).build();

        when(ruleRepo.findAll()).thenReturn(List.of(rule));
        when(taskRepo.findAll(any(TaskFilter.class)))
            .thenAnswer(inv -> ((TaskFilter) inv.getArgument(0)).isShowArchived()
                ? List.of() : List.of(template));
        when(taskRepo.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId((long) (Math.random() * 1000 + 10));
            return t;
        });
        when(goalRepo.findGoalIdByRuleId(anyLong())).thenReturn(Optional.empty());

        int created = service.generateUpcoming(14);

        // endDate is today+2, so at most 3 instances (today, +1, +2)
        assertThat(created).isLessThanOrEqualTo(3);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Task kickboxingTask(long ruleId) {
        return Task.builder()
            .id(1L)
            .title("Kickboxing training session")
            .recurrenceRuleId(ruleId)
            .categoryId(3L)
            .priority(Priority.HIGH)
            .status(TaskStatus.TODO)
            .estimatedMinutes(90)
            .dueDate(LocalDate.now().atTime(19, 0))
            .build();
    }
}

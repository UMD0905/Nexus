package com.nexus.service;

import com.nexus.model.Category;
import com.nexus.model.Streak;
import com.nexus.model.Task;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.CategoryRepository;
import com.nexus.repository.StreakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock StreakRepository    streakRepository;
    @Mock CategoryRepository  categoryRepository;

    StreakService service;

    @BeforeEach
    void setUp() {
        service = new StreakService(streakRepository, categoryRepository);
    }

    // ── createStreakForRule ───────────────────────────────────────────────────

    @Test
    @DisplayName("createStreakForRule persists and returns the saved streak")
    void createStreakForRule_validArgs_savedAndReturned() {
        Streak saved = Streak.builder()
            .id(1L).recurrenceRuleId(10L).title("Kickboxing")
            .categoryId(2L).currentStreak(0)
            .build();
        when(streakRepository.save(any())).thenReturn(saved);

        Streak result = service.createStreakForRule(10L, "Kickboxing", 2L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Kickboxing");
        assertThat(result.getRecurrenceRuleId()).isEqualTo(10L);
        verify(streakRepository).save(any(Streak.class));
    }

    // ── getAllStreaks ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllStreaks returns all streaks from repository")
    void getAllStreaks_returnsAllStreaks() {
        Streak s1 = Streak.builder().id(1L).title("Gym").build();
        Streak s2 = Streak.builder().id(2L).title("Coding").build();
        when(streakRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Streak> result = service.getAllStreaks();

        assertThat(result).hasSize(2);
        verify(streakRepository).findAll();
    }

    // ── recordCompletion ──────────────────────────────────────────────────────

    @Test
    @DisplayName("recordCompletion increments streak for a recurring task completed today")
    void recordCompletion_recurringTask_incrementsStreak() {
        Task task = Task.builder()
            .id(1L).title("Kickboxing").priority(Priority.HIGH)
            .status(TaskStatus.DONE).recurrenceRuleId(5L)
            .build();

        Streak existing = Streak.builder()
            .id(1L).recurrenceRuleId(5L).title("Kickboxing")
            .currentStreak(3).longestStreak(5)
            .lastCompletedDate(LocalDate.now().minusDays(1))
            .build();

        when(streakRepository.findByRuleId(5L)).thenReturn(Optional.of(existing));
        doNothing().when(streakRepository).update(any());

        service.recordCompletion(task);

        verify(streakRepository).update(any(Streak.class));
    }

    @Test
    @DisplayName("recordCompletion does nothing for non-recurring tasks")
    void recordCompletion_nonRecurringTask_noStreakUpdate() {
        Task task = Task.builder()
            .id(1L).title("One-off").priority(Priority.MEDIUM)
            .status(TaskStatus.DONE)
            .build();  // no recurrenceRuleId

        service.recordCompletion(task);

        verify(streakRepository, never()).update(any());
        verify(streakRepository, never()).findByRuleId(anyLong());
    }

    // ── Streak.isActive ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Streak.isActive returns true when last completed yesterday")
    void streakIsActive_lastCompletedYesterday_returnsTrue() {
        Streak streak = Streak.builder()
            .lastCompletedDate(LocalDate.now().minusDays(1))
            .build();

        assertThat(streak.isActive()).isTrue();
    }

    @Test
    @DisplayName("Streak.isActive returns false when last completed two days ago")
    void streakIsActive_lastCompletedTwoDaysAgo_returnsFalse() {
        Streak streak = Streak.builder()
            .lastCompletedDate(LocalDate.now().minusDays(2))
            .build();

        assertThat(streak.isActive()).isFalse();
    }

    @Test
    @DisplayName("Streak.isActive returns false when lastCompletedDate is null")
    void streakIsActive_nullDate_returnsFalse() {
        Streak streak = Streak.builder().build();

        assertThat(streak.isActive()).isFalse();
    }
}

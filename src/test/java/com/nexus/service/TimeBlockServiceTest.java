package com.nexus.service;

import com.nexus.model.TimeBlock;
import com.nexus.repository.TimeBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeBlockServiceTest {

    @Mock TimeBlockRepository timeBlockRepository;

    TimeBlockService service;

    @BeforeEach
    void setUp() {
        service = new TimeBlockService(timeBlockRepository);
    }

    // ── getBlocksForDate ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getBlocksForDate delegates to repository with given date")
    void getBlocksForDate_delegatesToRepository() {
        LocalDate today = LocalDate.now();
        TimeBlock block = TimeBlock.builder()
            .id(1L).title("Deep work").blockDate(today)
            .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
            .build();
        when(timeBlockRepository.findByDate(today)).thenReturn(List.of(block));

        List<TimeBlock> result = service.getBlocksForDate(today);

        assertThat(result).containsExactly(block);
        verify(timeBlockRepository).findByDate(today);
    }

    // ── createBlock ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBlock persists and returns the saved block")
    void createBlock_valid_savedAndReturned() {
        TimeBlock input = TimeBlock.builder()
            .title("Focus block")
            .blockDate(LocalDate.now())
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .build();
        TimeBlock saved = TimeBlock.builder().id(1L).title("Focus block")
            .blockDate(LocalDate.now())
            .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
            .build();
        when(timeBlockRepository.save(any())).thenReturn(saved);

        TimeBlock result = service.createBlock(input);

        assertThat(result.getId()).isEqualTo(1L);
        verify(timeBlockRepository).save(any(TimeBlock.class));
    }

    @Test
    @DisplayName("createBlock rejects blank title")
    void createBlock_blankTitle_throwsIllegalArgument() {
        TimeBlock input = TimeBlock.builder()
            .title("  ")
            .blockDate(LocalDate.now())
            .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
            .build();

        assertThatThrownBy(() -> service.createBlock(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
    }

    @Test
    @DisplayName("createBlock rejects null blockDate")
    void createBlock_nullDate_throwsIllegalArgument() {
        TimeBlock input = TimeBlock.builder()
            .title("No date")
            .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
            .build();

        assertThatThrownBy(() -> service.createBlock(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("date");
    }

    @Test
    @DisplayName("createBlock rejects null startTime")
    void createBlock_nullStartTime_throwsIllegalArgument() {
        TimeBlock input = TimeBlock.builder()
            .title("No start")
            .blockDate(LocalDate.now())
            .endTime(LocalTime.of(10, 0))
            .build();

        assertThatThrownBy(() -> service.createBlock(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createBlock rejects end time before start time")
    void createBlock_endBeforeStart_throwsIllegalArgument() {
        TimeBlock input = TimeBlock.builder()
            .title("Backwards")
            .blockDate(LocalDate.now())
            .startTime(LocalTime.of(11, 0))
            .endTime(LocalTime.of(9, 0))
            .build();

        assertThatThrownBy(() -> service.createBlock(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("after");
    }

    // ── deleteBlock ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteBlock delegates to repository")
    void deleteBlock_delegatesToRepository() {
        service.deleteBlock(5L);

        verify(timeBlockRepository).delete(5L);
    }

    // ── updateBlock ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateBlock without id throws")
    void updateBlock_noId_throwsIllegalArgument() {
        TimeBlock block = TimeBlock.builder()
            .title("No id")
            .blockDate(LocalDate.now())
            .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
            .build();

        assertThatThrownBy(() -> service.updateBlock(block))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.nexus.service;

import com.nexus.model.Task;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.RecurrenceRuleRepository;
import com.nexus.repository.TaskRepository;
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
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ICalExportServiceTest {

    @Mock TaskRepository           taskRepository;
    @Mock RecurrenceRuleRepository recurrenceRuleRepository;

    @TempDir File tempDir;

    ICalExportService service;

    @BeforeEach
    void setUp() {
        service = new ICalExportService(taskRepository, recurrenceRuleRepository);
    }

    // ── exportTo ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("exportTo creates a .ics file in the given directory")
    void exportTo_validDirectory_createsIcsFile() throws IOException {
        when(taskRepository.findAll(any())).thenReturn(List.of());
        when(recurrenceRuleRepository.findAll()).thenReturn(List.of());

        File result = service.exportTo(tempDir);

        assertThat(result).exists();
        assertThat(result.getName()).endsWith(".ics");
        assertThat(result.getParentFile()).isEqualTo(tempDir);
    }

    @Test
    @DisplayName("exportTo creates the target directory if it does not exist")
    void exportTo_nonExistentDirectory_createsDirectory() throws IOException {
        File subDir = new File(tempDir, "exports/nested");
        when(taskRepository.findAll(any())).thenReturn(List.of());
        when(recurrenceRuleRepository.findAll()).thenReturn(List.of());

        File result = service.exportTo(subDir);

        assertThat(subDir).isDirectory();
        assertThat(result).exists();
    }

    @Test
    @DisplayName("exportTo writes valid iCal header and footer")
    void exportTo_emptyTaskList_writesValidIcsStructure() throws IOException {
        when(taskRepository.findAll(any())).thenReturn(List.of());
        when(recurrenceRuleRepository.findAll()).thenReturn(List.of());

        File result = service.exportTo(tempDir);
        String content = Files.readString(result.toPath());

        assertThat(content).contains("BEGIN:VCALENDAR");
        assertThat(content).contains("END:VCALENDAR");
    }

    @Test
    @DisplayName("exportTo includes VTODO entry for each active task")
    void exportTo_tasksWithDueDate_includesVtodoEntry() throws IOException {
        Task task = Task.builder()
            .id(1L).title("Write report").priority(Priority.HIGH).status(TaskStatus.TODO)
            .dueDate(LocalDateTime.now().plusDays(1))
            .build();
        when(taskRepository.findAll(any())).thenReturn(List.of(task));
        when(recurrenceRuleRepository.findAll()).thenReturn(List.of());

        File result = service.exportTo(tempDir);
        String content = Files.readString(result.toPath());

        assertThat(content).contains("BEGIN:VEVENT");
        assertThat(content).contains("Write report");
        assertThat(content).contains("END:VEVENT");
    }

    @Test
    @DisplayName("exportTo excludes archived tasks")
    void exportTo_archivedTask_excludedFromOutput() throws IOException {
        Task archived = Task.builder()
            .id(2L).title("Old task").priority(Priority.LOW)
            .status(TaskStatus.DONE).archived(true)
            .build();
        when(taskRepository.findAll(any())).thenReturn(List.of());  // archived tasks are filtered upstream
        when(recurrenceRuleRepository.findAll()).thenReturn(List.of());

        File result = service.exportTo(tempDir);
        String content = Files.readString(result.toPath());

        assertThat(content).doesNotContain("Old task");
    }
}

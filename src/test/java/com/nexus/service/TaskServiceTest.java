package com.nexus.service;

import com.nexus.model.Category;
import com.nexus.model.Tag;
import com.nexus.model.Task;
import com.nexus.model.TaskFilter;
import com.nexus.model.enums.Priority;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.CategoryRepository;
import com.nexus.repository.SubtaskRepository;
import com.nexus.repository.TagRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TaskService}.
 *
 * <p>Repositories are mocked so these tests are pure business-logic tests
 * with no database involved.  The integration of the actual SQL queries is
 * tested separately at the repository level with an in-memory H2 DB.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository         taskRepository;
    @Mock CategoryRepository     categoryRepository;
    @Mock TagRepository          tagRepository;
    @Mock SubtaskRepository      subtaskRepository;
    @Mock StreakService          streakService;

    TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, categoryRepository,
                                  tagRepository, subtaskRepository, streakService);
    }

    // ── createTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTask persists and returns the saved task")
    void createTask_validTask_savedAndReturned() {
        Task input = Task.builder()
            .title("Write tests")
            .priority(Priority.HIGH)
            .status(TaskStatus.TODO)
            .build();

        Task saved = Task.builder()
            .id(1L)
            .title("Write tests")
            .priority(Priority.HIGH)
            .status(TaskStatus.TODO)
            .build();

        when(taskRepository.save(any())).thenReturn(saved);

        Task result = service.createTask(input);

        assertThat(result.getId()).isEqualTo(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask rejects blank title")
    void createTask_blankTitle_throwsException() {
        Task task = Task.builder()
            .title("   ")
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .build();

        assertThatThrownBy(() -> service.createTask(task))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
    }

    @Test
    @DisplayName("createTask rejects null title")
    void createTask_nullTitle_throwsException() {
        Task task = Task.builder()
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .build();

        assertThatThrownBy(() -> service.createTask(task))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── markDone ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markDone stamps completedAt and sets status to DONE")
    void markDone_existingTask_completedAtStamped() {
        Task task = Task.builder()
            .id(42L)
            .title("Kickboxing session")
            .priority(Priority.HIGH)
            .status(TaskStatus.TODO)
            .build();

        when(taskRepository.findById(42L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = service.markDone(42L);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("markDone is idempotent — already-done tasks are not re-stamped")
    void markDone_alreadyDone_idempotent() {
        LocalDateTime originalCompletion = LocalDateTime.now().minusHours(2);
        Task task = Task.builder()
            .id(7L)
            .title("Gym session")
            .priority(Priority.MEDIUM)
            .status(TaskStatus.DONE)
            .completedAt(originalCompletion)
            .build();

        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));

        Task result = service.markDone(7L);

        assertThat(result.getCompletedAt()).isEqualTo(originalCompletion);
        verify(taskRepository, never()).update(any());  // no DB write needed
    }

    @Test
    @DisplayName("markDone throws when task not found")
    void markDone_taskNotFound_throws() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markDone(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("999");
    }

    // ── archiveTask ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("archiveTask sets isArchived=true and stamps archivedAt")
    void archiveTask_activeTask_getsArchived() {
        Task task = Task.builder()
            .id(5L).title("Landing page").priority(Priority.HIGH).status(TaskStatus.TODO)
            .build();

        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = service.archiveTask(5L);

        assertThat(result.isArchived()).isTrue();
        assertThat(result.getArchivedAt()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    @DisplayName("archiving already-done task preserves DONE status")
    void archiveTask_doneTask_remainsDone() {
        LocalDateTime completedAt = LocalDateTime.now().minusDays(1);
        Task task = Task.builder()
            .id(6L).title("Done task").priority(Priority.LOW).status(TaskStatus.DONE)
            .completedAt(completedAt).build();

        when(taskRepository.findById(6L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = service.archiveTask(6L);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(result.isArchived()).isTrue();
    }

    // ── restoreTask ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("restoreTask clears archive flags and resets to TODO")
    void restoreTask_archivedTask_restoredToActive() {
        Task task = Task.builder()
            .id(8L).title("Restore me").priority(Priority.MEDIUM)
            .status(TaskStatus.DONE).archived(true)
            .archivedAt(LocalDateTime.now().minusDays(1))
            .completedAt(LocalDateTime.now().minusDays(1))
            .build();

        when(taskRepository.findById(8L)).thenReturn(Optional.of(task));
        when(taskRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = service.restoreTask(8L);

        assertThat(result.isArchived()).isFalse();
        assertThat(result.getArchivedAt()).isNull();
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(result.getCompletedAt()).isNull();
    }

    // ── getTasks ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTasks enriches returned tasks with category and tags")
    void getTasks_enrichesCategoryAndTags() {
        Category cat = Category.builder().id(1L).name("Work").color("#3B82F6").build();
        Tag tag      = Tag.builder().id(1L).name("focus").color("#3B82F6").build();
        Task task    = Task.builder().id(1L).title("Write report")
                           .categoryId(1L).priority(Priority.HIGH).status(TaskStatus.TODO).build();

        when(taskRepository.findAll(any(TaskFilter.class))).thenReturn(List.of(task));
        when(categoryRepository.findAll()).thenReturn(List.of(cat));
        when(tagRepository.findByTaskId(1L)).thenReturn(List.of(tag));

        List<Task> result = service.getTasks(TaskFilter.allActive());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(cat);
        assertThat(result.get(0).getTags()).containsExactly(tag);
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteTask delegates to the repository")
    void deleteTask_callsRepository() {
        service.deleteTask(99L);
        verify(taskRepository).delete(99L);
    }

    // ── updateTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTask without id throws")
    void updateTask_noId_throws() {
        Task task = Task.builder().title("No id").priority(Priority.LOW)
                        .status(TaskStatus.TODO).build();
        assertThatThrownBy(() -> service.updateTask(task))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

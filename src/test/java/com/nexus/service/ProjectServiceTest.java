package com.nexus.service;

import com.nexus.model.Category;
import com.nexus.model.Project;
import com.nexus.model.enums.ProjectStatus;
import com.nexus.repository.CategoryRepository;
import com.nexus.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ProjectRepository  projectRepository;
    @Mock CategoryRepository categoryRepository;

    ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, categoryRepository);
    }

    // ── getAllProjects ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllProjects delegates to repository")
    void getAllProjects_delegatesToRepository() {
        Project p = Project.builder().id(1L).name("Nexus App").build();
        when(projectRepository.findAll()).thenReturn(List.of(p));

        List<Project> result = service.getAllProjects();

        assertThat(result).containsExactly(p);
        verify(projectRepository).findAll();
    }

    // ── getProjectsByCategory ─────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectsByCategory filters by category id")
    void getProjectsByCategory_returnsMatchingProjects() {
        Project p = Project.builder().id(1L).name("Side project").categoryId(3L).build();
        when(projectRepository.findByCategoryId(3L)).thenReturn(List.of(p));

        List<Project> result = service.getProjectsByCategory(3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryId()).isEqualTo(3L);
    }

    // ── getProjectById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectById returns present Optional when found")
    void getProjectById_found_returnsPresent() {
        Project p = Project.builder().id(10L).name("Health").build();
        when(projectRepository.findById(10L)).thenReturn(Optional.of(p));

        assertThat(service.getProjectById(10L)).contains(p);
    }

    @Test
    @DisplayName("getProjectById returns empty Optional when not found")
    void getProjectById_notFound_returnsEmpty() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(service.getProjectById(999L)).isEmpty();
    }

    // ── createProject ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProject persists and returns the saved project")
    void createProject_validName_savedAndReturned() {
        Project input = Project.builder().name("Nexus v2").build();
        Project saved = Project.builder().id(1L).name("Nexus v2").status(ProjectStatus.ACTIVE).build();
        when(projectRepository.save(any())).thenReturn(saved);

        Project result = service.createProject(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("createProject rejects blank name")
    void createProject_blankName_throwsIllegalArgument() {
        Project input = Project.builder().name("  ").build();

        assertThatThrownBy(() -> service.createProject(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("createProject rejects null name")
    void createProject_nullName_throwsIllegalArgument() {
        Project input = Project.builder().build();

        assertThatThrownBy(() -> service.createProject(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── updateProject ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProject persists and returns the updated project")
    void updateProject_validUpdate_savedAndReturned() {
        Project input = Project.builder().id(2L).name("Updated Project").build();
        Project updated = Project.builder().id(2L).name("Updated Project").build();
        when(projectRepository.update(any())).thenReturn(updated);

        Project result = service.updateProject(input);

        assertThat(result.getName()).isEqualTo("Updated Project");
        verify(projectRepository).update(any(Project.class));
    }

    @Test
    @DisplayName("updateProject without id throws")
    void updateProject_noId_throwsIllegalArgument() {
        Project input = Project.builder().name("No id").build();

        assertThatThrownBy(() -> service.updateProject(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deleteProject ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProject delegates to repository")
    void deleteProject_delegatesToRepository() {
        service.deleteProject(42L);

        verify(projectRepository).delete(42L);
    }

    // ── Category enrichment ───────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectById enriches returned project with its category")
    void getProjectById_enrichesCategory() {
        Category cat = Category.builder().id(1L).name("Work").color("#3B82F6").build();
        Project p = Project.builder().id(1L).name("Nexus").categoryId(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        Optional<Project> result = service.getProjectById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getCategory()).isEqualTo(cat);
    }
}

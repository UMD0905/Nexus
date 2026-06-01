package com.nexus.service;

import com.nexus.model.Category;
import com.nexus.repository.CategoryRepository;
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
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;

    CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository);
    }

    // ── getAllCategories ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCategories delegates to repository")
    void getAllCategories_delegatesToRepository() {
        Category cat = Category.builder().id(1L).name("Work").color("#3B82F6").build();
        when(categoryRepository.findAll()).thenReturn(List.of(cat));

        List<Category> result = service.getAllCategories();

        assertThat(result).containsExactly(cat);
        verify(categoryRepository).findAll();
    }

    // ── getCategoryById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getCategoryById returns present Optional when found")
    void getCategoryById_found_returnsPresent() {
        Category cat = Category.builder().id(5L).name("Gym").build();
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(cat));

        Optional<Category> result = service.getCategoryById(5L);

        assertThat(result).contains(cat);
    }

    @Test
    @DisplayName("getCategoryById returns empty Optional when not found")
    void getCategoryById_notFound_returnsEmpty() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.getCategoryById(99L)).isEmpty();
    }

    // ── createCategory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createCategory persists and returns the saved category")
    void createCategory_validName_savedAndReturned() {
        Category input = Category.builder().name("Kickboxing").color("#EF4444").build();
        Category saved = Category.builder().id(1L).name("Kickboxing").color("#EF4444").build();
        when(categoryRepository.save(any())).thenReturn(saved);

        Category result = service.createCategory(input);

        assertThat(result.getId()).isEqualTo(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory rejects blank name")
    void createCategory_blankName_throwsIllegalArgument() {
        Category input = Category.builder().name("  ").build();

        assertThatThrownBy(() -> service.createCategory(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("createCategory rejects null name")
    void createCategory_nullName_throwsIllegalArgument() {
        Category input = Category.builder().build();

        assertThatThrownBy(() -> service.createCategory(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── updateCategory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCategory persists changes and returns updated category")
    void updateCategory_validUpdate_savedAndReturned() {
        Category input = Category.builder().id(2L).name("Fitness").color("#10B981").build();
        Category updated = Category.builder().id(2L).name("Fitness").color("#10B981").build();
        when(categoryRepository.update(any())).thenReturn(updated);

        Category result = service.updateCategory(input);

        assertThat(result.getName()).isEqualTo("Fitness");
        verify(categoryRepository).update(any(Category.class));
    }

    @Test
    @DisplayName("updateCategory without id throws")
    void updateCategory_noId_throwsIllegalArgument() {
        Category input = Category.builder().name("NoId").build();

        assertThatThrownBy(() -> service.updateCategory(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateCategory with blank name throws")
    void updateCategory_blankName_throwsIllegalArgument() {
        Category input = Category.builder().id(3L).name("").build();

        assertThatThrownBy(() -> service.updateCategory(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deleteCategory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCategory delegates to repository")
    void deleteCategory_delegatesToRepository() {
        service.deleteCategory(7L);

        verify(categoryRepository).delete(7L);
    }
}

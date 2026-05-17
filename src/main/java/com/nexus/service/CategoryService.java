package com.nexus.service;

import com.nexus.model.Category;
import com.nexus.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for {@link Category} (life areas).
 * Validates input and delegates persistence to the repository.
 */
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(long id) {
        return categoryRepository.findById(id);
    }

    public Category createCategory(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        if (category.getColor() == null) {
            category.setColor("#4A90D9");
        }
        if (category.getIcon() == null) {
            category.setIcon("mdi2b-briefcase-outline");
        }
        Category saved = categoryRepository.save(category);
        log.info("Created category '{}' (id={})", saved.getName(), saved.getId());
        return saved;
    }

    public Category updateCategory(Category category) {
        if (category.getId() == null) {
            throw new IllegalArgumentException("Cannot update a category without an id");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        return categoryRepository.update(category);
    }

    public void deleteCategory(long id) {
        categoryRepository.delete(id);
        log.info("Deleted category id={}", id);
    }
}

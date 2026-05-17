package com.nexus.viewmodel;

import com.nexus.model.Category;
import com.nexus.model.Goal;
import com.nexus.service.CategoryService;
import com.nexus.service.GoalService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ViewModel for the Goals view.
 */
public class GoalsViewModel {

    private static final Logger log = LoggerFactory.getLogger(GoalsViewModel.class);

    private final GoalService     goalService;
    private final CategoryService categoryService;

    private final ObservableList<Goal>     goals      = FXCollections.observableArrayList();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObjectProperty<Goal>     selected   = new SimpleObjectProperty<>();
    private final StringProperty           errorMsg   = new SimpleStringProperty("");

    public GoalsViewModel(GoalService goalService, CategoryService categoryService) {
        this.goalService     = goalService;
        this.categoryService = categoryService;
    }

    public void initialize() {
        loadCategories();
        loadGoals();
    }

    public void reload() { loadGoals(); }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public boolean createGoal(String title, String description,
                               Category category, java.time.LocalDate targetDate) {
        if (title == null || title.isBlank()) {
            errorMsg.set("Title is required.");
            return false;
        }
        try {
            Goal g = Goal.builder()
                .title(title.trim())
                .description(description)
                .categoryId(category != null ? category.getId() : null)
                .targetDate(targetDate)
                .build();
            goalService.createGoal(g);
            loadGoals();
            return true;
        } catch (Exception e) {
            errorMsg.set(e.getMessage());
            return false;
        }
    }

    public void deleteGoal(Goal goal) {
        goalService.deleteGoal(goal.getId());
        selected.set(null);
        loadGoals();
    }

    public void completeGoal(Goal goal) {
        goalService.completeGoal(goal.getId());
        loadGoals();
    }

    public void abandonGoal(Goal goal) {
        goalService.abandonGoal(goal.getId());
        loadGoals();
    }

    public double getProgress(Goal goal) {
        return goalService.getProgress(goal);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void loadGoals() {
        try {
            goals.setAll(goalService.getAllGoals());
        } catch (Exception e) {
            log.error("Failed to load goals", e);
        }
    }

    private void loadCategories() {
        try {
            categories.setAll(categoryService.getAllCategories());
        } catch (Exception e) {
            log.error("Failed to load categories for goals", e);
        }
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public ObservableList<Goal>     getGoals()        { return goals; }
    public ObservableList<Category> getCategories()   { return categories; }
    public ObjectProperty<Goal>     selectedProperty(){ return selected; }
    public StringProperty           errorMsgProperty(){ return errorMsg; }
    public GoalService              getGoalService()  { return goalService; }
}

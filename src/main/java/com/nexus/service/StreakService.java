package com.nexus.service;

import com.nexus.model.Category;
import com.nexus.model.Streak;
import com.nexus.model.Task;
import com.nexus.repository.CategoryRepository;
import com.nexus.repository.StreakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * Manages completion streaks for recurring task series.
 *
 * <p>Call {@link #recordCompletion(Task)} whenever a recurring task is marked done.
 * The streak is incremented if the task was completed on a new day (not already counted today),
 * or reset to 1 if more than one day has passed since the last completion.
 */
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);

    private final StreakRepository   streakRepo;
    private final CategoryRepository categoryRepo;

    public StreakService(StreakRepository streakRepo, CategoryRepository categoryRepo) {
        this.streakRepo   = streakRepo;
        this.categoryRepo = categoryRepo;
    }

    // ── Creation ──────────────────────────────────────────────────────────────

    /**
     * Creates a new streak row for a freshly created recurrence rule.
     * Called when the user creates a recurring task via the UI.
     */
    public Streak createStreakForRule(long ruleId, String title, Long categoryId) {
        Streak streak = Streak.builder()
            .recurrenceRuleId(ruleId)
            .title(title)
            .categoryId(categoryId)
            .currentStreak(0)
            .longestStreak(0)
            .build();
        Streak saved = streakRepo.save(streak);
        log.info("Created streak '{}' for rule id={}", title, ruleId);
        return saved;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns all streaks, enriched with their category. */
    public List<Streak> getAllStreaks() {
        List<Streak> streaks = streakRepo.findAll();
        streaks.forEach(this::enrich);
        return streaks;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Updates a streak's editable fields (title, categoryId, currentStreak, longestStreak).
     * Resets {@code lastCompletedDate} to null when currentStreak is set to 0.
     */
    public Streak updateStreak(Streak patch) {
        if (patch.getId() == null) throw new IllegalArgumentException("Cannot update streak without id");
        if (patch.getTitle() == null || patch.getTitle().isBlank())
            throw new IllegalArgumentException("Streak title must not be blank");

        Streak existing = streakRepo.findById(patch.getId())
            .orElseThrow(() -> new IllegalArgumentException("Streak not found: " + patch.getId()));

        existing.setTitle(patch.getTitle());
        existing.setCategoryId(patch.getCategoryId());
        existing.setCurrentStreak(Math.max(0, patch.getCurrentStreak()));
        existing.setLongestStreak(Math.max(existing.getLongestStreak(), existing.getCurrentStreak()));
        if (existing.getCurrentStreak() == 0) existing.setLastCompletedDate(null);

        streakRepo.update(existing);
        log.info("Updated streak id={} title='{}' current={}", existing.getId(), existing.getTitle(), existing.getCurrentStreak());
        return existing;
    }

    /** Permanently deletes a streak row. */
    public void deleteStreak(long id) {
        streakRepo.delete(id);
        log.info("Deleted streak id={}", id);
    }

    // ── Update logic ──────────────────────────────────────────────────────────

    /**
     * Updates the streak for the given task if it belongs to a recurrence rule.
     * Safe to call for non-recurring tasks — it simply does nothing.
     */
    public void recordCompletion(Task task) {
        if (task.getRecurrenceRuleId() == null) return;

        streakRepo.findByRuleId(task.getRecurrenceRuleId()).ifPresent(streak -> {
            LocalDate today = LocalDate.now();
            LocalDate last  = streak.getLastCompletedDate();

            if (last != null && last.equals(today)) {
                return; // already counted today
            }

            // Extend or reset
            if (last == null || last.isBefore(today.minusDays(1))) {
                // Gap → reset to 1
                streak.setCurrentStreak(1);
            } else {
                // Consecutive → extend
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            }

            if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                streak.setLongestStreak(streak.getCurrentStreak());
            }

            streak.setLastCompletedDate(today);
            streakRepo.update(streak);
            log.info("Streak '{}' updated: current={} longest={}",
                streak.getTitle(), streak.getCurrentStreak(), streak.getLongestStreak());
        });
    }

    /**
     * Checks all active streaks and resets any that have gone more than one day
     * without a completion. Call once on app startup.
     */
    public void expireStaleStreaks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Streak> streaks = streakRepo.findAll();
        for (Streak streak : streaks) {
            if (streak.getCurrentStreak() > 0
                    && streak.getLastCompletedDate() != null
                    && streak.getLastCompletedDate().isBefore(yesterday)) {
                streak.setCurrentStreak(0);
                streakRepo.update(streak);
                log.info("Streak '{}' expired (last completed: {})", streak.getTitle(),
                    streak.getLastCompletedDate());
            }
        }
    }

    private void enrich(Streak streak) {
        if (streak.getCategoryId() != null) {
            categoryRepo.findById(streak.getCategoryId())
                .ifPresent(streak::setCategory);
        }
    }
}

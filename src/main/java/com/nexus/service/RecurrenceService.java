package com.nexus.service;

import com.nexus.model.RecurrenceRule;
import com.nexus.model.Task;
import com.nexus.model.enums.RecurrenceType;
import com.nexus.repository.RecurrenceRuleRepository;
import com.nexus.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates upcoming task instances for all recurring tasks.
 *
 * <p>Call {@link #generateUpcoming(int)} on startup and once a day.
 * It looks ahead {@code daysAhead} days, checks each recurrence rule,
 * and creates a task instance for each day that does not already have one.
 * De-duplication is done via {@code recurrence_rule_id + DATE(due_date)}.
 */
public class RecurrenceService {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceService.class);

    private final RecurrenceRuleRepository ruleRepo;
    private final TaskRepository           taskRepo;

    public RecurrenceService(RecurrenceRuleRepository ruleRepo, TaskRepository taskRepo) {
        this.ruleRepo = ruleRepo;
        this.taskRepo = taskRepo;
    }

    /**
     * Generates upcoming instances for every active recurrence rule.
     *
     * @param daysAhead how many days into the future to generate (e.g. 14)
     * @return number of new task instances created
     */
    public int generateUpcoming(int daysAhead) {
        List<RecurrenceRule> rules = ruleRepo.findAll();
        if (rules.isEmpty()) return 0;

        // Load existing recurring tasks to avoid duplicates
        // We collect (ruleId, date) pairs that already exist
        Set<String> existing = taskRepo.findAll(
            com.nexus.model.TaskFilter.builder().showArchived(false).build()
        ).stream()
            .filter(t -> t.getRecurrenceRuleId() != null && t.getDueDate() != null)
            .map(t -> t.getRecurrenceRuleId() + ":" + t.getDueDate().toLocalDate())
            .collect(Collectors.toSet());

        // Also check archived for the same date window
        Set<String> existingArchived = taskRepo.findAll(
            com.nexus.model.TaskFilter.builder().showArchived(true).build()
        ).stream()
            .filter(t -> t.getRecurrenceRuleId() != null && t.getDueDate() != null)
            .map(t -> t.getRecurrenceRuleId() + ":" + t.getDueDate().toLocalDate())
            .collect(Collectors.toSet());
        existing.addAll(existingArchived);

        LocalDate today = LocalDate.now();
        int created = 0;

        for (RecurrenceRule rule : rules) {
            // Find a template task for this rule (first one by id)
            Task template = taskRepo.findAll(
                com.nexus.model.TaskFilter.builder().showArchived(false).build()
            ).stream()
                .filter(t -> rule.getId().equals(t.getRecurrenceRuleId()))
                .findFirst()
                .orElse(null);

            if (template == null) continue; // no base task to clone

            List<LocalDate> dates = getDatesForRule(rule, today, daysAhead);
            for (LocalDate date : dates) {
                String key = rule.getId() + ":" + date;
                if (existing.contains(key)) continue;

                // Clone the template
                LocalTime time = template.getDueDate() != null
                    ? template.getDueDate().toLocalTime()
                    : LocalTime.of(8, 0);

                Task instance = Task.builder()
                    .title(template.getTitle())
                    .description(template.getDescription())
                    .categoryId(template.getCategoryId())
                    .priority(template.getPriority())
                    .status(com.nexus.model.enums.TaskStatus.TODO)
                    .dueDate(date.atTime(time))
                    .estimatedMinutes(template.getEstimatedMinutes())
                    .recurrenceRuleId(rule.getId())
                    .reminderMinutesBefore(template.getReminderMinutesBefore())
                    .important(template.isImportant())
                    .urgent(template.isUrgent())
                    .build();

                taskRepo.save(instance);
                existing.add(key); // prevent double-insert in same run
                created++;
                log.debug("Generated recurring task '{}' for {}", template.getTitle(), date);
            }
        }

        log.info("Recurrence generation: {} new instance(s) created (lookahead={}d)", created, daysAhead);
        return created;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<LocalDate> getDatesForRule(RecurrenceRule rule, LocalDate from, int daysAhead) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate end = from.plusDays(daysAhead);
        if (rule.getEndDate() != null && rule.getEndDate().isBefore(end)) {
            end = rule.getEndDate();
        }

        LocalDate cursor = from;
        while (!cursor.isAfter(end)) {
            if (matchesRule(rule, cursor)) {
                result.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private boolean matchesRule(RecurrenceRule rule, LocalDate date) {
        return switch (rule.getType()) {
            case DAILY    -> true;
            case WEEKDAYS -> date.getDayOfWeek() != DayOfWeek.SATURDAY
                          && date.getDayOfWeek() != DayOfWeek.SUNDAY;
            case WEEKLY   -> {
                if (rule.getDaysOfWeek() == null || rule.getDaysOfWeek().isBlank()) {
                    yield false;
                }
                String abbr = dayAbbrev(date.getDayOfWeek());
                yield List.of(rule.getDaysOfWeek().split(",")).contains(abbr);
            }
        };
    }

    private String dayAbbrev(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> "MON";
            case TUESDAY   -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY  -> "THU";
            case FRIDAY    -> "FRI";
            case SATURDAY  -> "SAT";
            case SUNDAY    -> "SUN";
        };
    }
}

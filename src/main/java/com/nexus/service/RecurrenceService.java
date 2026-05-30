package com.nexus.service;

import com.nexus.model.RecurrenceRule;
import com.nexus.model.Task;
import com.nexus.model.enums.RecurrenceType;
import com.nexus.repository.GoalRepository;
import com.nexus.repository.RecurrenceRuleRepository;
import com.nexus.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final GoalRepository           goalRepo;

    public RecurrenceService(RecurrenceRuleRepository ruleRepo,
                              TaskRepository taskRepo,
                              GoalRepository goalRepo) {
        this.ruleRepo = ruleRepo;
        this.taskRepo = taskRepo;
        this.goalRepo = goalRepo;
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
            // AFTER_COMPLETION rules are handled in TaskService.markDone, not here
            if ("AFTER_COMPLETION".equals(rule.getMode())) continue;

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
                    .startTime(template.getStartTime())   // preserve start time from template
                    .estimatedMinutes(template.getEstimatedMinutes())
                    .recurrenceRuleId(rule.getId())
                    .reminderMinutesBefore(template.getReminderMinutesBefore())
                    .important(template.isImportant())
                    .urgent(template.isUrgent())
                    .build();

                taskRepo.save(instance);
                existing.add(key); // prevent double-insert in same run
                created++;

                // Copy goal link: if ANY existing task in this rule is linked to a goal,
                // link the new instance to the same goal so progress counts it.
                goalRepo.findGoalIdByRuleId(rule.getId())
                    .ifPresent(goalId -> goalRepo.linkTask(goalId, instance.getId()));
                log.debug("Generated recurring task '{}' for {}", template.getTitle(), date);
            }
        }

        log.info("Recurrence generation: {} new instance(s) created (lookahead={}d)", created, daysAhead);
        return created;
    }

    /**
     * Propagates a rule or template-task change to all future TODO instances.
     *
     * <p>Finds the earliest TODO instance as the template (so the user's most recent
     * edits are reflected), then overwrites title, description, category, priority,
     * estimated minutes, reminder offset, urgent, important, and time-of-day on every
     * future TODO instance.  Each instance's date is preserved.
     *
     * @param ruleId the recurrence rule whose instances should be updated
     */
    public void propagateRuleChange(long ruleId) {
        List<com.nexus.model.Task> allActive = taskRepo.findAll(
            com.nexus.model.TaskFilter.builder().showArchived(false).build());

        // Earliest TODO instance acts as the template (carries the user's latest edits)
        com.nexus.model.Task template = allActive.stream()
            .filter(t -> ruleId == t.getRecurrenceRuleId()
                         && t.getStatus() == com.nexus.model.enums.TaskStatus.TODO)
            .min(Comparator.comparing(t -> t.getDueDate() != null ? t.getDueDate() : LocalDateTime.MAX))
            .orElse(null);

        if (template == null) return;

        LocalDate today = LocalDate.now();
        LocalTime templateTime = template.getDueDate() != null
            ? template.getDueDate().toLocalTime()
            : LocalTime.of(8, 0);

        int updated = 0;
        for (com.nexus.model.Task instance : allActive) {
            if (ruleId != instance.getRecurrenceRuleId()) continue;
            if (instance.getStatus() != com.nexus.model.enums.TaskStatus.TODO) continue;
            if (instance.getDueDate() == null) continue;
            if (instance.getDueDate().toLocalDate().isBefore(today)) continue;
            if (instance.getId().equals(template.getId())) continue; // skip template itself

            instance.setTitle(template.getTitle());
            instance.setDescription(template.getDescription());
            instance.setCategoryId(template.getCategoryId());
            instance.setPriority(template.getPriority());
            instance.setEstimatedMinutes(template.getEstimatedMinutes());
            instance.setReminderMinutesBefore(template.getReminderMinutesBefore());
            instance.setImportant(template.isImportant());
            instance.setUrgent(template.isUrgent());
            instance.setStartTime(template.getStartTime());
            // Preserve instance date, apply template's time-of-day
            instance.setDueDate(instance.getDueDate().toLocalDate().atTime(templateTime));
            taskRepo.update(instance);
            updated++;
        }

        log.info("propagateRuleChange ruleId={}: updated {} future TODO instance(s)", ruleId, updated);
    }

    /**
     * Called from {@code TaskService.markDone} when a task that belongs to an
     * AFTER_COMPLETION rule is completed.  Generates exactly one new instance
     * due {@code completionDate + rule.intervalVal} units from the completed task's date.
     */
    public void generateAfterCompletion(Task completedTask) {
        if (completedTask.getRecurrenceRuleId() == null) return;
        RecurrenceRule rule = ruleRepo.findById(completedTask.getRecurrenceRuleId()).orElse(null);
        if (rule == null || !"AFTER_COMPLETION".equals(rule.getMode())) return;
        if (rule.getEndDate() != null && rule.getEndDate().isBefore(LocalDate.now())) return;

        LocalDate completionDate = completedTask.getCompletedAt() != null
            ? completedTask.getCompletedAt().toLocalDate() : LocalDate.now();
        int interval = Math.max(1, rule.getIntervalVal());

        // Compute the next due date based on rule type
        LocalDate nextDate = switch (rule.getType()) {
            case DAILY    -> completionDate.plusDays(interval);
            case WEEKDAYS -> {
                LocalDate d = completionDate.plusDays(1);
                while (d.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                       d.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                    d = d.plusDays(1);
                }
                yield d;
            }
            case WEEKLY   -> completionDate.plusWeeks(interval);
            case MONTHLY  -> completionDate.plusMonths(interval);
            case YEARLY   -> completionDate.plusYears(interval);
        };

        if (rule.getEndDate() != null && nextDate.isAfter(rule.getEndDate())) return;

        // Clone the completed task as the new instance
        LocalTime time = completedTask.getDueDate() != null
            ? completedTask.getDueDate().toLocalTime() : LocalTime.of(8, 0);

        Task instance = Task.builder()
            .title(completedTask.getTitle())
            .description(completedTask.getDescription())
            .categoryId(completedTask.getCategoryId())
            .priority(completedTask.getPriority())
            .status(com.nexus.model.enums.TaskStatus.TODO)
            .dueDate(nextDate.atTime(time))
            .startTime(completedTask.getStartTime())
            .estimatedMinutes(completedTask.getEstimatedMinutes())
            .recurrenceRuleId(rule.getId())
            .reminderMinutesBefore(completedTask.getReminderMinutesBefore())
            .important(completedTask.isImportant())
            .urgent(completedTask.isUrgent())
            .build();

        taskRepo.save(instance);
        goalRepo.findGoalIdByRuleId(rule.getId())
            .ifPresent(goalId -> goalRepo.linkTask(goalId, instance.getId()));
        log.info("AFTER_COMPLETION: generated next instance '{}' for {}", instance.getTitle(), nextDate);
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
            // For DAILY with intervalVal > 1, step by that interval instead of 1 day
            if (rule.getType() == RecurrenceType.DAILY
                    && rule.getIntervalVal() > 1) {
                cursor = cursor.plusDays(rule.getIntervalVal());
            } else {
                cursor = cursor.plusDays(1);
            }
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
            case MONTHLY  -> {
                int dom = date.getDayOfMonth();
                if (rule.getDayOfMonth() == null) yield false;
                if (rule.getDayOfMonth() == -1) {
                    // Last day of the month
                    yield dom == date.getMonth().length(date.isLeapYear());
                }
                // Graceful fallback for 29/30/31 in shorter months
                int target = Math.min(rule.getDayOfMonth(),
                    date.getMonth().length(date.isLeapYear()));
                yield dom == target;
            }
            case YEARLY   -> {
                if (rule.getDayOfMonth() == null || rule.getMonthOfYear() == null) yield false;
                if (date.getMonthValue() != rule.getMonthOfYear()) yield false;
                int targetDom;
                if (rule.getDayOfMonth() == -1) {
                    targetDom = date.getMonth().length(date.isLeapYear());
                } else {
                    targetDom = Math.min(rule.getDayOfMonth(),
                        date.getMonth().length(date.isLeapYear()));
                }
                yield date.getDayOfMonth() == targetDom;
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

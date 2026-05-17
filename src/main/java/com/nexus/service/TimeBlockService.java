package com.nexus.service;

import com.nexus.model.TimeBlock;
import com.nexus.repository.TimeBlockRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for day-planner {@link TimeBlock} objects.
 */
public class TimeBlockService {

    private final TimeBlockRepository repo;

    public TimeBlockService(TimeBlockRepository repo) { this.repo = repo; }

    public List<TimeBlock> getBlocksForDate(LocalDate date) {
        return repo.findByDate(date);
    }

    public List<TimeBlock> getBlocksForWeek(LocalDate monday) {
        return repo.findByDateRange(monday, monday.plusDays(6));
    }

    public TimeBlock createBlock(TimeBlock block) {
        if (block.getTitle() == null || block.getTitle().isBlank()) {
            throw new IllegalArgumentException("Time block must have a title.");
        }
        if (block.getBlockDate() == null) {
            throw new IllegalArgumentException("Time block must have a date.");
        }
        if (block.getStartTime() == null || block.getEndTime() == null) {
            throw new IllegalArgumentException("Time block must have start and end times.");
        }
        if (!block.getEndTime().isAfter(block.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        return repo.save(block);
    }

    public void updateBlock(TimeBlock block) {
        if (block.getId() == null) throw new IllegalArgumentException("Cannot update block without id.");
        repo.update(block);
    }

    public void deleteBlock(long id) {
        repo.delete(id);
    }
}

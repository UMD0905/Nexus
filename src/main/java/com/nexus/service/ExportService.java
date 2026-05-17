package com.nexus.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexus.model.*;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.CategoryRepository;
import com.nexus.repository.GoalRepository;
import com.nexus.repository.TagRepository;
import com.nexus.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON backup/export service.
 *
 * <p>Exports the full database snapshot (tasks, categories, tags, goals) to a
 * human-readable JSON file.  The output is a single JSON object so it can be
 * imported back later.
 *
 * <p>File name pattern: {@code nexus-backup-YYYY-MM-DD.json}
 */
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TaskRepository     taskRepo;
    private final CategoryRepository categoryRepo;
    private final TagRepository      tagRepo;
    private final GoalRepository     goalRepo;

    private final ObjectMapper mapper;

    public ExportService(TaskRepository taskRepo,
                         CategoryRepository categoryRepo,
                         TagRepository tagRepo,
                         GoalRepository goalRepo) {
        this.taskRepo     = taskRepo;
        this.categoryRepo = categoryRepo;
        this.tagRepo      = tagRepo;
        this.goalRepo     = goalRepo;

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * Exports a full backup to the given directory.
     *
     * @param directory target directory (created if absent)
     * @return the file written
     * @throws IOException on write failure
     */
    public File exportTo(File directory) throws IOException {
        if (!directory.exists()) directory.mkdirs();
        String filename = "nexus-backup-" + LocalDate.now().format(FILE_DATE) + ".json";
        File output = new File(directory, filename);

        Map<String, Object> snapshot = buildSnapshot();
        mapper.writeValue(output, snapshot);
        log.info("Exported backup to {}", output.getAbsolutePath());
        return output;
    }

    /**
     * Returns a quick summary string without writing to disk — used for the
     * dashboard stats card.
     */
    public String buildSummaryJson() {
        try {
            Map<String, Object> snapshot = buildSnapshot();
            return mapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Builds the full data snapshot map. */
    private Map<String, Object> buildSnapshot() {
        List<Task>     allTasks      = taskRepo.findAll(TaskFilter.builder().showArchived(false).build());
        List<Task>     archivedTasks = taskRepo.findAll(TaskFilter.builder().showArchived(true).build());
        List<Category> categories    = categoryRepo.findAll();
        List<Tag>      tags          = tagRepo.findAll();
        List<Goal>     goals         = goalRepo.findAll();

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("exportedAt",    LocalDateTime.now().toString());
        snapshot.put("version",       "3.0");
        snapshot.put("categories",    categories);
        snapshot.put("tags",          tags);
        snapshot.put("tasks",         allTasks);
        snapshot.put("archivedTasks", archivedTasks);
        snapshot.put("goals",         goals);
        snapshot.put("stats", Map.of(
            "activeTasks",   allTasks.size(),
            "archivedTasks", archivedTasks.size(),
            "completedTasks", allTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count(),
            "categories",    categories.size(),
            "goals",         goals.size()
        ));
        return snapshot;
    }
}

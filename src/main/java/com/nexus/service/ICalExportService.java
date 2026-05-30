package com.nexus.service;

import com.nexus.model.RecurrenceRule;
import com.nexus.model.Task;
import com.nexus.model.TaskFilter;
import com.nexus.model.enums.RecurrenceType;
import com.nexus.model.enums.TaskStatus;
import com.nexus.repository.RecurrenceRuleRepository;
import com.nexus.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates an iCal (.ics) file from all active tasks.
 *
 * <p>RRULE mapping:
 * <ul>
 *   <li>DAILY     → FREQ=DAILY</li>
 *   <li>WEEKDAYS  → FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR</li>
 *   <li>WEEKLY    → FREQ=WEEKLY;BYDAY=&lt;comma-separated days&gt;</li>
 * </ul>
 *
 * <p>Format uses CRLF line endings per RFC 5545.
 */
public class ICalExportService {

    private static final Logger log = LoggerFactory.getLogger(ICalExportService.class);

    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DT_UTC   = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FILE_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TaskRepository            taskRepo;
    private final RecurrenceRuleRepository  recurrenceRuleRepo;

    public ICalExportService(TaskRepository taskRepo, RecurrenceRuleRepository recurrenceRuleRepo) {
        this.taskRepo           = taskRepo;
        this.recurrenceRuleRepo = recurrenceRuleRepo;
    }

    /**
     * Exports all active tasks to {@code nexus-export-<yyyy-MM-dd>.ics} in the given directory.
     *
     * @param directory target directory (must exist or will be created)
     * @return the written file
     */
    public File exportTo(File directory) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String today    = LocalDateTime.now().format(FILE_FMT);
        File   outFile  = new File(directory, "nexus-export-" + today + ".ics");
        String dtstamp  = LocalDateTime.now(ZoneOffset.UTC).format(DT_UTC);

        List<Task> tasks = taskRepo.findAll(TaskFilter.builder().showArchived(false).build());

        StringBuilder sb = new StringBuilder();
        crlf(sb, "BEGIN:VCALENDAR");
        crlf(sb, "VERSION:2.0");
        crlf(sb, "PRODID:-//Nexus//Nexus Productivity//EN");
        crlf(sb, "CALSCALE:GREGORIAN");
        crlf(sb, "METHOD:PUBLISH");

        for (Task task : tasks) {
            sb.append(buildVEvent(task, dtstamp));
        }

        crlf(sb, "END:VCALENDAR");

        // Write with CRLF line endings (RFC 5545 §3.1)
        Files.writeString(outFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
        log.info("iCal exported {} tasks → {}", tasks.size(), outFile.getAbsolutePath());
        return outFile;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildVEvent(Task task, String dtstamp) {
        StringBuilder sb = new StringBuilder();
        crlf(sb, "BEGIN:VEVENT");
        crlf(sb, "UID:" + task.getId() + "@nexus");
        crlf(sb, fold("SUMMARY:" + escapeText(task.getTitle())));
        crlf(sb, "DTSTAMP:" + dtstamp);

        // DTSTART
        if (task.getDueDate() != null) {
            LocalDateTime dt = task.getDueDate();
            // If the time component is midnight we treat it as a DATE-only value
            if (dt.getHour() == 0 && dt.getMinute() == 0 && dt.getSecond() == 0) {
                crlf(sb, "DTSTART;VALUE=DATE:" + dt.format(DATE_FMT));
            } else {
                crlf(sb, "DTSTART;TZID=UTC:" + dt.format(DT_FMT));
            }
        }

        // DESCRIPTION
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            crlf(sb, fold("DESCRIPTION:" + escapeText(task.getDescription())));
        }

        // RRULE
        if (task.getRecurrenceRuleId() != null) {
            recurrenceRuleRepo.findById(task.getRecurrenceRuleId())
                .ifPresent(rule -> crlf(sb, "RRULE:" + toRRule(rule)));
        }

        // STATUS
        if (task.getStatus() == TaskStatus.DONE) {
            crlf(sb, "STATUS:COMPLETED");
        }

        crlf(sb, "END:VEVENT");
        return sb.toString();
    }

    /** Appends {@code line} followed by CRLF. */
    private static void crlf(StringBuilder sb, String line) {
        sb.append(line).append("\r\n");
    }

    /**
     * RFC 5545 §3.1 — long content lines must be folded at 75 octets.
     * Each continuation line begins with a SPACE character.
     */
    private static String fold(String line) {
        if (line.length() <= 75) return line;
        StringBuilder out = new StringBuilder();
        // Work on bytes because RFC 5545 counts octets (UTF-8 bytes)
        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        int    start = 0;
        boolean first = true;
        while (start < bytes.length) {
            int end = Math.min(start + (first ? 75 : 74), bytes.length);
            // Step back to avoid splitting a multi-byte UTF-8 sequence
            while (end < bytes.length && (bytes[end] & 0xC0) == 0x80) {
                end--;
            }
            if (!first) out.append(" ");
            out.append(new String(bytes, start, end - start, StandardCharsets.UTF_8));
            if (end < bytes.length) out.append("\r\n");
            start = end;
            first = false;
        }
        return out.toString();
    }

    /** Escapes special characters per RFC 5545 §3.3.11. */
    private static String escapeText(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(";",  "\\;")
                .replace(",",  "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    /** Maps a {@link RecurrenceRule} to an iCal RRULE value. */
    private static String toRRule(RecurrenceRule rule) {
        if (rule.getType() == RecurrenceType.DAILY) {
            return "FREQ=DAILY";
        }
        if (rule.getType() == RecurrenceType.WEEKDAYS) {
            return "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR";
        }
        // WEEKLY — map stored day abbreviations ("MON,WED,FRI") to iCal 2-letter codes
        String byday = rule.getDaysOfWeek() != null
            ? mapDays(rule.getDaysOfWeek()) : "";
        return "FREQ=WEEKLY" + (byday.isEmpty() ? "" : ";BYDAY=" + byday);
    }

    /**
     * Converts stored day abbreviations (e.g. "MON,TUE,FRI") to iCal format ("MO,TU,FR").
     */
    private static String mapDays(String daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) return "";
        List<String> result = new ArrayList<>();
        for (String day : daysOfWeek.split(",")) {
            result.add(switch (day.trim().toUpperCase()) {
                case "MON", "MO" -> "MO";
                case "TUE", "TU" -> "TU";
                case "WED", "WE" -> "WE";
                case "THU", "TH" -> "TH";
                case "FRI", "FR" -> "FR";
                case "SAT", "SA" -> "SA";
                case "SUN", "SU" -> "SU";
                default           -> day.trim().toUpperCase().substring(0, Math.min(2, day.trim().length()));
            });
        }
        return String.join(",", result);
    }
}

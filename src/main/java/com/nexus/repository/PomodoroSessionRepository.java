package com.nexus.repository;

import com.nexus.model.PomodoroSession;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

import static com.nexus.db.Tables.POMODORO_SESSIONS;

/**
 * Data-access layer for {@link PomodoroSession}.
 */
public class PomodoroSessionRepository {

    private static final Logger log = LoggerFactory.getLogger(PomodoroSessionRepository.class);

    private final DSLContext dsl;

    public PomodoroSessionRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<PomodoroSession> findByTaskId(long taskId) {
        return dsl.selectFrom(POMODORO_SESSIONS)
            .where(POMODORO_SESSIONS.TASK_ID.eq(taskId))
            .orderBy(POMODORO_SESSIONS.STARTED_AT.desc())
            .fetch()
            .map(this::recordToSession);
    }

    /** All sessions for today (for the Pomodoro view's session counter). */
    public List<PomodoroSession> findToday() {
        LocalDateTime start = java.time.LocalDate.now().atStartOfDay();
        LocalDateTime end   = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        return dsl.selectFrom(POMODORO_SESSIONS)
            .where(POMODORO_SESSIONS.STARTED_AT.between(start, end))
            .orderBy(POMODORO_SESSIONS.STARTED_AT.desc())
            .fetch()
            .map(this::recordToSession);
    }

    public PomodoroSession save(PomodoroSession session) {
        var record = dsl.newRecord(POMODORO_SESSIONS);
        record.setTaskId(session.getTaskId());
        record.setStartedAt(session.getStartedAt());
        record.setEndedAt(session.getEndedAt());
        record.setDurationMinutes(session.getDurationMinutes());
        record.setCompleted(session.isCompleted());
        record.setNotes(session.getNotes());
        record.setCreatedAt(LocalDateTime.now());
        record.store();
        session.setId(record.getId());
        log.debug("Saved pomodoro session id={} taskId={}", session.getId(), session.getTaskId());
        return session;
    }

    public void markCompleted(long id, LocalDateTime endedAt) {
        dsl.update(POMODORO_SESSIONS)
            .set(POMODORO_SESSIONS.ENDED_AT,   endedAt)
            .set(POMODORO_SESSIONS.COMPLETED,  true)
            .where(POMODORO_SESSIONS.ID.eq(id))
            .execute();
    }

    private PomodoroSession recordToSession(org.jooq.Record r) {
        return PomodoroSession.builder()
            .id(r.get(POMODORO_SESSIONS.ID))
            .taskId(r.get(POMODORO_SESSIONS.TASK_ID))
            .startedAt(r.get(POMODORO_SESSIONS.STARTED_AT))
            .endedAt(r.get(POMODORO_SESSIONS.ENDED_AT))
            .durationMinutes(r.get(POMODORO_SESSIONS.DURATION_MINUTES) != null
                ? r.get(POMODORO_SESSIONS.DURATION_MINUTES) : 25)
            .completed(Boolean.TRUE.equals(r.get(POMODORO_SESSIONS.COMPLETED)))
            .notes(r.get(POMODORO_SESSIONS.NOTES))
            .createdAt(r.get(POMODORO_SESSIONS.CREATED_AT))
            .build();
    }
}

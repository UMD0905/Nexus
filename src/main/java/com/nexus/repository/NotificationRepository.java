package com.nexus.repository;

import com.nexus.model.AppNotification;
import com.nexus.model.enums.NotificationType;
import org.jooq.DSLContext;

import java.time.LocalDateTime;
import java.util.List;

import static com.nexus.db.Tables.NOTIFICATIONS;

/** Data-access layer for {@link AppNotification} (in-app notification centre). */
public class NotificationRepository {

    private final DSLContext dsl;

    public NotificationRepository(DSLContext dsl) { this.dsl = dsl; }

    /** Returns all unread notifications, newest first. */
    public List<AppNotification> findUnread() {
        return dsl.selectFrom(NOTIFICATIONS)
            .where(NOTIFICATIONS.IS_READ.eq(false))
            .orderBy(NOTIFICATIONS.CREATED_AT.desc())
            .fetch()
            .map(this::recordToNotification);
    }

    /** Returns all notifications (read + unread), newest first. */
    public List<AppNotification> findAll() {
        return dsl.selectFrom(NOTIFICATIONS)
            .orderBy(NOTIFICATIONS.CREATED_AT.desc())
            .limit(100)   // cap display to the last 100
            .fetch()
            .map(this::recordToNotification);
    }

    /** Counts unread notifications — used for the bell badge. */
    public int countUnread() {
        return dsl.fetchCount(NOTIFICATIONS, NOTIFICATIONS.IS_READ.eq(false));
    }

    public AppNotification save(AppNotification n) {
        var record = dsl.newRecord(NOTIFICATIONS);
        record.setTitle(n.getTitle());
        record.setMessage(n.getMessage());
        record.setType(n.getType().name());
        record.setTaskId(n.getTaskId());
        record.setIsRead(false);
        record.setCreatedAt(LocalDateTime.now());
        record.store();
        n.setId(record.getId());
        return n;
    }

    public void markRead(long id) {
        dsl.update(NOTIFICATIONS)
            .set(NOTIFICATIONS.IS_READ, true)
            .where(NOTIFICATIONS.ID.eq(id))
            .execute();
    }

    public void markAllRead() {
        dsl.update(NOTIFICATIONS)
            .set(NOTIFICATIONS.IS_READ, true)
            .where(NOTIFICATIONS.IS_READ.eq(false))
            .execute();
    }

    public void delete(long id) {
        dsl.deleteFrom(NOTIFICATIONS).where(NOTIFICATIONS.ID.eq(id)).execute();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private AppNotification recordToNotification(org.jooq.Record r) {
        return AppNotification.builder()
            .id(r.get(NOTIFICATIONS.ID))
            .title(r.get(NOTIFICATIONS.TITLE))
            .message(r.get(NOTIFICATIONS.MESSAGE))
            .type(NotificationType.valueOf(r.get(NOTIFICATIONS.TYPE)))
            .taskId(r.get(NOTIFICATIONS.TASK_ID))
            .read(Boolean.TRUE.equals(r.get(NOTIFICATIONS.IS_READ)))
            .createdAt(r.get(NOTIFICATIONS.CREATED_AT))
            .build();
    }
}

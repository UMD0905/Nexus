package com.nexus.service;

import com.nexus.model.AppNotification;
import com.nexus.model.enums.NotificationType;
import com.nexus.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Creates and manages in-app {@link AppNotification} records.
 */
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    public List<AppNotification> getUnread() {
        return repo.findUnread();
    }

    public List<AppNotification> getAll() {
        return repo.findAll();
    }

    public AppNotification createReminder(String title, String message, Long taskId) {
        return create(title, message, NotificationType.REMINDER, taskId);
    }

    public AppNotification createInfo(String title, String message) {
        return create(title, message, NotificationType.SYSTEM, null);
    }

    public void markRead(long id) {
        repo.markRead(id);
    }

    public void markAllRead() {
        repo.markAllRead();
    }

    public int countUnread() {
        return repo.countUnread();
    }

    private AppNotification create(String title, String message,
                                   NotificationType type, Long taskId) {
        AppNotification n = AppNotification.builder()
            .title(title)
            .message(message)
            .type(type)
            .taskId(taskId)
            .read(false)
            .createdAt(java.time.LocalDateTime.now())
            .build();
        repo.save(n);
        log.debug("Created notification '{}' type={}", title, type);
        return n;
    }
}

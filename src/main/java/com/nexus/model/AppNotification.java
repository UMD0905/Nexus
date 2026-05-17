package com.nexus.model;

import com.nexus.model.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An in-app notification stored in the database.
 * Named {@code AppNotification} to avoid clashing with
 * {@code javafx.scene.control.Notification} and the OS-level
 * ControlsFX notification API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppNotification {

    private Long             id;
    private String           title;
    private String           message;
    @Builder.Default
    private NotificationType type    = NotificationType.REMINDER;
    /** The task this notification relates to; may be null for system messages. */
    private Long             taskId;
    private boolean          read;
    private LocalDateTime    createdAt;
}

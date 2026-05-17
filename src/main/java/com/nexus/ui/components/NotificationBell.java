package com.nexus.ui.components;

import atlantafx.base.theme.Styles;
import com.nexus.model.AppNotification;
import com.nexus.service.NotificationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.stage.Popup;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bell icon button with an unread-count badge.
 *
 * <p>Clicking opens a {@link Popup} listing the latest notifications.
 * The badge disappears once all notifications are marked read.
 */
public class NotificationBell extends StackPane {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationService notificationService;
    private final Label               badge;
    private final Popup               popup;

    public NotificationBell(NotificationService notificationService) {
        this.notificationService = notificationService;

        // Bell icon button
        Button bellBtn = new Button();
        bellBtn.setGraphic(new FontIcon(MaterialDesignB.BELL_OUTLINE));
        bellBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);

        // Red badge circle
        badge = new Label();
        badge.getStyleClass().add("notification-badge");
        badge.setStyle("-fx-background-color: -color-danger-emphasis; " +
                       "-fx-background-radius: 8; " +
                       "-fx-text-fill: white; " +
                       "-fx-font-size: 9px; " +
                       "-fx-font-weight: bold; " +
                       "-fx-padding: 1 4 1 4;");
        badge.setVisible(false);

        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-4, -4, 0, 0));

        getChildren().addAll(bellBtn, badge);
        getStyleClass().add("notification-bell");

        // Popup panel
        popup = buildPopup();

        bellBtn.setOnAction(e -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                var bounds = bellBtn.localToScreen(bellBtn.getBoundsInLocal());
                popup.show(bellBtn, bounds.getMinX() - 260, bounds.getMaxY() + 4);
                markAllRead();
            }
        });

        refresh();
    }

    /** Refreshes the unread count badge — call after new notifications arrive. */
    public void refresh() {
        try {
            int count = notificationService.countUnread();
            if (count > 0) {
                badge.setText(count > 9 ? "9+" : String.valueOf(count));
                badge.setVisible(true);
            } else {
                badge.setVisible(false);
            }
        } catch (Exception e) {
            badge.setVisible(false);
        }
    }

    // ── Popup ─────────────────────────────────────────────────────────────────

    private Popup buildPopup() {
        Popup p = new Popup();
        p.setAutoHide(true);

        VBox content = buildPopupContent();
        content.setStyle("-fx-background-color: -color-bg-subtle; " +
                         "-fx-border-color: -color-border-muted; " +
                         "-fx-border-radius: 8; " +
                         "-fx-background-radius: 8; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);");
        content.setPrefWidth(320);
        content.setMaxHeight(400);

        p.getContent().add(content);

        // Rebuild on each show
        p.setOnShowing(e -> {
            p.getContent().clear();
            p.getContent().add(buildPopupContent());
        });

        return p;
    }

    private VBox buildPopupContent() {
        Label heading = new Label("Notifications");
        heading.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " +
                         "-fx-text-fill: -color-fg-default;");
        heading.setPadding(new Insets(12, 16, 8, 16));

        Button markAllBtn = new Button("Mark all read");
        markAllBtn.getStyleClass().addAll(Styles.SMALL, Styles.FLAT);
        markAllBtn.setOnAction(e -> {
            markAllRead();
            popup.hide();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(4, heading, spacer, markAllBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(4, 8, 4, 0));

        Separator sep = new Separator();

        List<AppNotification> notifications;
        try {
            notifications = notificationService.getAll();
        } catch (Exception e) {
            notifications = List.of();
        }

        VBox items = new VBox(0);
        if (notifications.isEmpty()) {
            Label empty = new Label("No notifications yet.");
            empty.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");
            empty.setPadding(new Insets(16));
            items.getChildren().add(empty);
        } else {
            for (AppNotification n : notifications) {
                items.getChildren().add(buildNotificationRow(n));
                items.getChildren().add(new Separator());
            }
        }

        ScrollPane scroll = new ScrollPane(items);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPrefHeight(Math.min(notifications.size() * 60.0 + 16, 320));

        VBox content = new VBox(0, topRow, sep, scroll);
        content.setStyle("-fx-background-color: -color-bg-subtle; " +
                         "-fx-border-color: -color-border-muted; " +
                         "-fx-border-radius: 8; " +
                         "-fx-background-radius: 8; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);");
        content.setPrefWidth(320);
        return content;
    }

    private HBox buildNotificationRow(AppNotification n) {
        Circle dot = new Circle(4, n.isRead() ? Color.TRANSPARENT : Color.web("#3B82F6"));
        dot.setStroke(n.isRead() ? Color.web("#6B7280", 0.5) : Color.TRANSPARENT);

        Label titleLbl = new Label(n.getTitle());
        titleLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; " +
                          (n.isRead() ? "-fx-text-fill: -color-fg-muted;" : "-fx-text-fill: -color-fg-default;"));
        titleLbl.setWrapText(true);

        Label msgLbl = new Label(n.getMessage());
        msgLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
        msgLbl.setWrapText(true);

        String timeStr = n.getCreatedAt() != null ? n.getCreatedAt().format(TIME_FMT) : "";
        Label timeLbl = new Label(timeStr);
        timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-fg-subtle;");

        VBox text = new VBox(2, titleLbl, msgLbl);
        VBox.setVgrow(text, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, dot, text, spacer, timeLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        return row;
    }

    private void markAllRead() {
        try {
            notificationService.markAllRead();
            badge.setVisible(false);
        } catch (Exception e) {
            // non-fatal
        }
    }
}

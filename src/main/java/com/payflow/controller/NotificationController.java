
package com.payflow.controller;

import com.payflow.entity.Notification;
import com.payflow.repository.NotificationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null) {
            throw new IllegalArgumentException(
                    "Authentication is required"
            );
        }

        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") String filter
    ) {
        try {
            UUID userId = currentUserId(authentication);

            List<Notification> notifications;

            if ("UNREAD".equalsIgnoreCase(filter)) {
                notifications =
                        repository.findByUserIdAndIsReadOrderByCreatedAtDesc(
                                userId, false
                        );
            } else {
                notifications =
                        repository.findByUserIdOrderByCreatedAtDesc(userId);
            }

            return ResponseEntity.ok(notifications);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required or invalid"));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            Authentication authentication
    ) {
        try {
            UUID userId = currentUserId(authentication);
            long count = repository.countByUserIdAndIsRead(
                    userId, false
            );

            return ResponseEntity.ok(Map.of("unreadCount", count));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required or invalid"));
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        try {
            UUID userId = currentUserId(authentication);

            Notification notification = repository
                    .findByIdAndUserId(id, userId)
                    .orElse(null);

            if (notification == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Notification not found"));
            }

            notification.setRead(true);
            return ResponseEntity.ok(repository.save(notification));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required or invalid"));
        }
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            Authentication authentication
    ) {
        try {
            UUID userId = currentUserId(authentication);

            List<Notification> notifications =
                    repository.findByUserIdAndIsReadOrderByCreatedAtDesc(
                            userId, false
                    );

            notifications.forEach(notification ->
                    notification.setRead(true)
            );

            repository.saveAll(notifications);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "All notifications marked as read",
                            "updatedCount", notifications.size()
                    )
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required or invalid"));
        }
    }
}

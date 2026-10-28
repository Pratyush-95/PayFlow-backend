
package com.payflow.repository;

import com.payflow.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(
            UUID userId,
            boolean isRead
    );

    long countByUserIdAndIsRead(UUID userId, boolean isRead);

    Optional<Notification> findByIdAndUserId(
            UUID id,
            UUID userId
    );
}

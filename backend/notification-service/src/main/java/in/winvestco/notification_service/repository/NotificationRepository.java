package in.winvestco.notification_service.repository;

import in.winvestco.notification_service.model.Notification;
import in.winvestco.notification_service.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find notifications for a user, ordered by creation date descending.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find notifications for a user with specific status.
     */
    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, NotificationStatus status, Pageable pageable);

    /**
     * Find unread notifications for a user.
     */
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);

    /**
     * Count unread notifications for a user.
     */
    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    /**
     * Mark all unread notifications as read for a user.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllAsReadForUser(@Param("userId") Long userId);

    /**
     * Delete old archived notifications.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status = 'ARCHIVED' " +
           "AND n.createdAt < :cutoffTime")
    int deleteOldArchivedNotifications(@Param("cutoffTime") java.time.Instant cutoffTime);
}

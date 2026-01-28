package in.winvestco.notification_service.repository;

import in.winvestco.notification_service.model.NotificationChannel;
import in.winvestco.notification_service.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for NotificationChannel entity.
 */
@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {

    /**
     * Find channel preferences for a user and notification type.
     */
    Optional<NotificationChannel> findByUserIdAndNotificationType(Long userId, NotificationType notificationType);

    /**
     * Find all channel preferences for a user.
     */
    List<NotificationChannel> findByUserId(Long userId);

    /**
     * Find all users who have enabled a specific channel for a notification type.
     */
    @Query("SELECT nc FROM NotificationChannel nc WHERE nc.notificationType = :type " +
            "AND ((:channel = 'WEBSOCKET' AND nc.websocketEnabled = true) OR " +
            "(:channel = 'PUSH' AND nc.pushEnabled = true) OR " +
            "(:channel = 'EMAIL' AND nc.emailEnabled = true) OR " +
            "(:channel = 'SMS' AND nc.smsEnabled = true))")
    List<NotificationChannel> findByNotificationTypeAndChannelEnabled(
            @Param("type") NotificationType type,
            @Param("channel") String channel);

    /**
     * Find all users with push notifications enabled (have FCM token).
     */
    @Query("SELECT nc FROM NotificationChannel nc WHERE nc.pushEnabled = true AND nc.fcmToken IS NOT NULL")
    List<NotificationChannel> findAllWithPushEnabled();

    /**
     * Find all users with email notifications enabled.
     */
    @Query("SELECT nc FROM NotificationChannel nc WHERE nc.emailEnabled = true AND nc.emailAddress IS NOT NULL")
    List<NotificationChannel> findAllWithEmailEnabled();

    /**
     * Check if user has any channel configured for a notification type.
     */
    boolean existsByUserIdAndNotificationType(Long userId, NotificationType notificationType);

    /**
     * Delete all channel preferences for a user.
     */
    void deleteByUserId(Long userId);

    /**
     * Update FCM token for a user across all notification types.
     */
    @Query("UPDATE NotificationChannel nc SET nc.fcmToken = :fcmToken WHERE nc.userId = :userId")
    void updateFcmTokenForUser(@Param("userId") Long userId, @Param("fcmToken") String fcmToken);
}

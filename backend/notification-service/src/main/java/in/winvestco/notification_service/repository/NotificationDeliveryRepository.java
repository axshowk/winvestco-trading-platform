package in.winvestco.notification_service.repository;

import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.DeliveryStatus;
import in.winvestco.notification_service.model.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for NotificationDelivery entity.
 */
@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    /**
     * Find delivery record for a specific notification and channel.
     */
    Optional<NotificationDelivery> findByNotificationIdAndChannel(Long notificationId, DeliveryChannel channel);

    /**
     * Find all delivery records for a notification.
     */
    List<NotificationDelivery> findByNotificationId(Long notificationId);

    /**
     * Find deliveries ready for retry (status is retryable and next_retry_at has
     * passed).
     */
    @Query("SELECT d FROM NotificationDelivery d WHERE d.status IN ('PENDING', 'RETRYING') " +
            "AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now) " +
            "ORDER BY d.createdAt ASC")
    List<NotificationDelivery> findReadyForRetry(@Param("now") Instant now, Pageable pageable);

    /**
     * Find pending deliveries for a specific channel.
     */
    @Query("SELECT d FROM NotificationDelivery d WHERE d.channel = :channel " +
            "AND d.status IN ('PENDING', 'RETRYING') " +
            "AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)")
    List<NotificationDelivery> findPendingByChannel(
            @Param("channel") DeliveryChannel channel,
            @Param("now") Instant now,
            Pageable pageable);

    /**
     * Find pending deliveries for a user (via notification).
     */
    @Query("SELECT d FROM NotificationDelivery d " +
            "WHERE d.notification.userId = :userId " +
            "AND d.status IN ('PENDING', 'RETRYING')")
    List<NotificationDelivery> findPendingByUserId(@Param("userId") Long userId);

    /**
     * Find failed deliveries for monitoring/alerting.
     */
    @Query("SELECT d FROM NotificationDelivery d WHERE d.status = 'FAILED' " +
            "AND d.lastAttemptedAt >= :since ORDER BY d.lastAttemptedAt DESC")
    List<NotificationDelivery> findRecentFailures(@Param("since") Instant since, Pageable pageable);

    /**
     * Find dead letter deliveries.
     */
    Page<NotificationDelivery> findByStatus(DeliveryStatus status, Pageable pageable);

    /**
     * Count pending deliveries by channel.
     */
    @Query("SELECT d.channel, COUNT(d) FROM NotificationDelivery d " +
            "WHERE d.status IN ('PENDING', 'RETRYING') GROUP BY d.channel")
    List<Object[]> countPendingByChannel();

    /**
     * Count deliveries by status for a time range.
     */
    @Query("SELECT d.status, COUNT(d) FROM NotificationDelivery d " +
            "WHERE d.createdAt >= :since GROUP BY d.status")
    List<Object[]> countByStatusSince(@Param("since") Instant since);

    /**
     * Get delivery stats for a notification.
     */
    @Query("SELECT d.channel, d.status, d.attemptCount FROM NotificationDelivery d " +
            "WHERE d.notification.id = :notificationId")
    List<Object[]> getDeliveryStats(@Param("notificationId") Long notificationId);

    /**
     * Move old failed deliveries to dead letter.
     */
    @Modifying
    @Query("UPDATE NotificationDelivery d SET d.status = 'DEAD_LETTER' " +
            "WHERE d.status = 'FAILED' AND d.lastAttemptedAt < :cutoff")
    int moveToDeadLetter(@Param("cutoff") Instant cutoff);

    /**
     * Delete old dead letter entries.
     */
    @Modifying
    @Query("DELETE FROM NotificationDelivery d WHERE d.status = 'DEAD_LETTER' " +
            "AND d.lastAttemptedAt < :cutoff")
    int deleteOldDeadLetters(@Param("cutoff") Instant cutoff);

    /**
     * Find stale in-progress deliveries (likely crashed during processing).
     */
    @Query("SELECT d FROM NotificationDelivery d WHERE d.status = 'IN_PROGRESS' " +
            "AND d.lastAttemptedAt < :staleThreshold")
    List<NotificationDelivery> findStaleInProgress(@Param("staleThreshold") Instant staleThreshold);
}

package in.winvestco.notification_service.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDeliveryTest {

    @Test
    void markDelivered_shouldSetStatusAndTimestamp() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.PENDING)
                .build();

        delivery.markDelivered();

        assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
        assertNotNull(delivery.getDeliveredAt());
        assertNotNull(delivery.getLastAttemptedAt());
        assertNull(delivery.getErrorMessage());
        assertNull(delivery.getErrorCode());
    }

    @Test
    void recordFailure_whenUnderMaxAttempts_shouldScheduleRetry() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(3)
                .build();

        boolean willRetry = delivery.recordFailure("Connection timeout", "TIMEOUT", 1000L);

        assertTrue(willRetry);
        assertEquals(DeliveryStatus.RETRYING, delivery.getStatus());
        assertEquals(1, delivery.getAttemptCount());
        assertEquals("Connection timeout", delivery.getErrorMessage());
        assertEquals("TIMEOUT", delivery.getErrorCode());
        assertNotNull(delivery.getNextRetryAt());
        assertNotNull(delivery.getFirstAttemptedAt());
    }

    @Test
    void recordFailure_whenMaxAttemptsReached_shouldMarkFailed() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.SMS)
                .status(DeliveryStatus.RETRYING)
                .attemptCount(2)
                .maxAttempts(3)
                .build();

        boolean willRetry = delivery.recordFailure("Service unavailable", "503", 2000L);

        assertFalse(willRetry);
        assertEquals(DeliveryStatus.FAILED, delivery.getStatus());
        assertEquals(3, delivery.getAttemptCount());
    }

    @Test
    void moveToDeadLetter_shouldSetStatusAndReason() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.PUSH)
                .status(DeliveryStatus.FAILED)
                .build();

        delivery.moveToDeadLetter("Max retries exceeded");

        assertEquals(DeliveryStatus.DEAD_LETTER, delivery.getStatus());
        assertEquals("Max retries exceeded", delivery.getErrorMessage());
    }

    @Test
    void skip_shouldSetStatusAndReason() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.SMS)
                .status(DeliveryStatus.PENDING)
                .build();

        delivery.skip("Channel disabled");

        assertEquals(DeliveryStatus.SKIPPED, delivery.getStatus());
        assertEquals("Channel disabled", delivery.getErrorMessage());
    }

    @Test
    void markInProgress_shouldSetStatusAndTimestamps() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.PENDING)
                .build();

        delivery.markInProgress();

        assertEquals(DeliveryStatus.IN_PROGRESS, delivery.getStatus());
        assertNotNull(delivery.getLastAttemptedAt());
        assertNotNull(delivery.getFirstAttemptedAt());
    }

    @Test
    void markInProgress_shouldNotOverwriteFirstAttemptedAt() {
        Instant firstAttempt = Instant.now().minusSeconds(60);
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.RETRYING)
                .firstAttemptedAt(firstAttempt)
                .build();

        delivery.markInProgress();

        assertEquals(firstAttempt, delivery.getFirstAttemptedAt());
    }

    @Test
    void calculateRetryDelay_shouldApplyExponentialBackoff() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.EMAIL)
                .attemptCount(0)
                .build();

        assertEquals(1000L, delivery.calculateRetryDelay(1000L, 2.0));

        delivery.setAttemptCount(1);
        assertEquals(2000L, delivery.calculateRetryDelay(1000L, 2.0));

        delivery.setAttemptCount(2);
        assertEquals(4000L, delivery.calculateRetryDelay(1000L, 2.0));
    }

    @Test
    void isReadyForRetry_whenPendingWithNoRetryTime_shouldReturnTrue() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.PENDING)
                .nextRetryAt(null)
                .build();

        assertTrue(delivery.isReadyForRetry());
    }

    @Test
    void isReadyForRetry_whenRetryTimeInFuture_shouldReturnFalse() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.RETRYING)
                .nextRetryAt(Instant.now().plusSeconds(3600))
                .build();

        assertFalse(delivery.isReadyForRetry());
    }

    @Test
    void isReadyForRetry_whenDelivered_shouldReturnFalse() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .id(1L)
                .channel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.DELIVERED)
                .build();

        assertFalse(delivery.isReadyForRetry());
    }
}

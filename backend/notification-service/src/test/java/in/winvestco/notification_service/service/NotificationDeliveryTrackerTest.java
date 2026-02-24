package in.winvestco.notification_service.service;

import in.winvestco.notification_service.config.NotificationChannelConfig;
import in.winvestco.notification_service.model.*;
import in.winvestco.notification_service.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryTrackerTest {

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private NotificationChannelConfig config;

    @InjectMocks
    private NotificationDeliveryTracker deliveryTracker;

    private Notification testNotification;
    private NotificationDelivery testDelivery;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .type(NotificationType.ORDER_CREATED)
                .title("Order Placed")
                .message("Your order has been placed")
                .build();

        testDelivery = NotificationDelivery.builder()
                .id(1L)
                .notification(testNotification)
                .channel(DeliveryChannel.EMAIL)
                .status(DeliveryStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(3)
                .build();
    }

    @Test
    void createDeliveryRecords_shouldCreateRecordsForEachChannel() {
        NotificationChannelConfig.Delivery deliveryConfig = new NotificationChannelConfig.Delivery();
        deliveryConfig.setMaxRetries(3);
        when(config.getDelivery()).thenReturn(deliveryConfig);
        when(deliveryRepository.save(any())).thenAnswer(inv -> {
            NotificationDelivery d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        Set<DeliveryChannel> channels = Set.of(DeliveryChannel.EMAIL, DeliveryChannel.SMS);
        Map<DeliveryChannel, String> destinations = Map.of(
                DeliveryChannel.EMAIL, "user@example.com",
                DeliveryChannel.SMS, "+919876543210");

        List<NotificationDelivery> result = deliveryTracker.createDeliveryRecords(
                testNotification, channels, destinations);

        assertEquals(2, result.size());
        verify(deliveryRepository, times(2)).save(any());
    }

    @Test
    void recordSuccess_shouldMarkDelivered() {
        when(deliveryRepository.findByNotificationIdAndChannel(1L, DeliveryChannel.EMAIL))
                .thenReturn(Optional.of(testDelivery));

        deliveryTracker.recordSuccess(1L, DeliveryChannel.EMAIL);

        assertEquals(DeliveryStatus.DELIVERED, testDelivery.getStatus());
        verify(deliveryRepository).save(testDelivery);
    }

    @Test
    void recordFailure_whenRetryAvailable_shouldReturnTrue() {
        NotificationChannelConfig.Delivery deliveryConfig = new NotificationChannelConfig.Delivery();
        deliveryConfig.setRetryDelayMs(1000L);
        deliveryConfig.setRetryBackoffMultiplier(2.0);
        when(config.getDelivery()).thenReturn(deliveryConfig);
        when(deliveryRepository.findByNotificationIdAndChannel(1L, DeliveryChannel.EMAIL))
                .thenReturn(Optional.of(testDelivery));

        boolean willRetry = deliveryTracker.recordFailure(1L, DeliveryChannel.EMAIL,
                "Connection timeout", "TIMEOUT");

        assertTrue(willRetry);
        assertEquals(DeliveryStatus.RETRYING, testDelivery.getStatus());
        verify(deliveryRepository).save(testDelivery);
    }

    @Test
    void recordFailure_whenMaxRetriesExceeded_shouldReturnFalse() {
        testDelivery.setAttemptCount(2);
        testDelivery.setMaxAttempts(3);
        NotificationChannelConfig.Delivery deliveryConfig = new NotificationChannelConfig.Delivery();
        deliveryConfig.setRetryDelayMs(1000L);
        deliveryConfig.setRetryBackoffMultiplier(2.0);
        when(config.getDelivery()).thenReturn(deliveryConfig);
        when(deliveryRepository.findByNotificationIdAndChannel(1L, DeliveryChannel.EMAIL))
                .thenReturn(Optional.of(testDelivery));

        boolean willRetry = deliveryTracker.recordFailure(1L, DeliveryChannel.EMAIL,
                "Service unavailable", "503");

        assertFalse(willRetry);
        assertEquals(DeliveryStatus.FAILED, testDelivery.getStatus());
    }

    @Test
    void skipDelivery_shouldMarkAsSkipped() {
        when(deliveryRepository.findByNotificationIdAndChannel(1L, DeliveryChannel.EMAIL))
                .thenReturn(Optional.of(testDelivery));

        deliveryTracker.skipDelivery(1L, DeliveryChannel.EMAIL, "Channel disabled");

        assertEquals(DeliveryStatus.SKIPPED, testDelivery.getStatus());
        verify(deliveryRepository).save(testDelivery);
    }

    @Test
    void getDeliveryStatus_shouldReturnDTOList() {
        when(deliveryRepository.findByNotificationId(1L)).thenReturn(List.of(testDelivery));

        var result = deliveryTracker.getDeliveryStatus(1L);

        assertEquals(1, result.size());
        assertEquals(DeliveryChannel.EMAIL, result.get(0).getChannel());
    }

    @Test
    void wasDelivered_whenOneChannelDelivered_shouldReturnTrue() {
        NotificationDelivery deliveredDelivery = NotificationDelivery.builder()
                .id(2L)
                .notification(testNotification)
                .channel(DeliveryChannel.WEBSOCKET)
                .status(DeliveryStatus.DELIVERED)
                .attemptCount(1)
                .build();
        when(deliveryRepository.findByNotificationId(1L))
                .thenReturn(List.of(testDelivery, deliveredDelivery));

        assertTrue(deliveryTracker.wasDelivered(1L));
    }

    @Test
    void wasDelivered_whenNoneDelivered_shouldReturnFalse() {
        when(deliveryRepository.findByNotificationId(1L)).thenReturn(List.of(testDelivery));

        assertFalse(deliveryTracker.wasDelivered(1L));
    }

    @Test
    void archiveFailedDeliveries_shouldDelegateToRepository() {
        Instant cutoff = Instant.now().minusSeconds(86400);
        when(deliveryRepository.moveToDeadLetter(cutoff)).thenReturn(5);

        int result = deliveryTracker.archiveFailedDeliveries(cutoff);

        assertEquals(5, result);
    }

    @Test
    void cleanupDeadLetters_shouldDelegateToRepository() {
        Instant cutoff = Instant.now().minusSeconds(604800);
        when(deliveryRepository.deleteOldDeadLetters(cutoff)).thenReturn(3);

        int result = deliveryTracker.cleanupDeadLetters(cutoff);

        assertEquals(3, result);
    }
}

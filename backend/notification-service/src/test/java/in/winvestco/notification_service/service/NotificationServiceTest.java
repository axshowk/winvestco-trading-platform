package in.winvestco.notification_service.service;

import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.mapper.NotificationMapper;
import in.winvestco.notification_service.model.Notification;
import in.winvestco.notification_service.model.NotificationStatus;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private WebSocketNotificationService webSocketService;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .type(NotificationType.TRADE_EXECUTED)
                .title("Trade Executed")
                .message("Your trade for RELIANCE was executed")
                .status(NotificationStatus.UNREAD)
                .build();
    }

    @Test
    void createNotification_WhenNotMuted_ShouldSaveAndSend() {
        when(preferenceService.isNotificationMuted(anyLong(), any())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(new NotificationDTO());

        NotificationDTO result = notificationService.createNotification(1L, NotificationType.TRADE_EXECUTED, "Test",
                "Message", Collections.emptyMap());

        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendToUser(eq(1L), any());
    }

    @Test
    void createNotification_WhenMuted_ShouldReturnNull() {
        when(preferenceService.isNotificationMuted(anyLong(), any())).thenReturn(true);

        NotificationDTO result = notificationService.createNotification(1L, NotificationType.TRADE_EXECUTED, "Test",
                "Message", Collections.emptyMap());

        assertNull(result);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_ShouldUpdateStatus() {
        when(notificationRepository.findById(anyLong())).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(new NotificationDTO());

        Optional<NotificationDTO> result = notificationService.markAsRead(1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(NotificationStatus.READ, testNotification.getStatus());
        verify(notificationRepository).save(testNotification);
    }
}

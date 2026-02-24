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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
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
    private NotificationDeliveryStrategy deliveryStrategy;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private NotificationDTO testDTO;

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

        testDTO = new NotificationDTO();
        testDTO.setId(1L);
        testDTO.setTitle("Trade Executed");
    }

    @Test
    void createNotification_whenNotMuted_shouldSaveAndDeliver() {
        when(preferenceService.isNotificationMuted(anyLong(), any())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(testDTO);

        NotificationDTO result = notificationService.createNotification(1L, NotificationType.TRADE_EXECUTED, "Test",
                "Message", Collections.emptyMap());

        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
        verify(deliveryStrategy).deliver(eq(1L), any());
    }

    @Test
    void createNotification_whenMuted_shouldReturnNull() {
        when(preferenceService.isNotificationMuted(anyLong(), any())).thenReturn(true);

        NotificationDTO result = notificationService.createNotification(1L, NotificationType.TRADE_EXECUTED, "Test",
                "Message", Collections.emptyMap());

        assertNull(result);
        verify(notificationRepository, never()).save(any());
        verify(deliveryStrategy, never()).deliver(anyLong(), any());
    }

    @Test
    void getNotificationsForUser_shouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(testDTO);

        Page<NotificationDTO> result = notificationService.getNotificationsForUser(1L, pageable);

        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void getNotificationsByStatus_shouldFilterByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, NotificationStatus.UNREAD, pageable))
                .thenReturn(page);
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(testDTO);

        Page<NotificationDTO> result = notificationService.getNotificationsByStatus(1L, NotificationStatus.UNREAD,
                pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.UNREAD)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    void markAsRead_shouldUpdateStatus() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(testDTO);

        Optional<NotificationDTO> result = notificationService.markAsRead(1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(NotificationStatus.READ, testNotification.getStatus());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void markAsRead_wrongUser_shouldReturnEmpty() {
        Notification otherUserNotification = Notification.builder()
                .id(1L)
                .userId(999L)
                .type(NotificationType.TRADE_EXECUTED)
                .title("Test")
                .message("Test")
                .status(NotificationStatus.UNREAD)
                .build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(otherUserNotification));

        Optional<NotificationDTO> result = notificationService.markAsRead(1L, 1L);

        assertTrue(result.isEmpty());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_notFound_shouldReturnEmpty() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<NotificationDTO> result = notificationService.markAsRead(999L, 1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void markAllAsRead_shouldDelegateToRepository() {
        when(notificationRepository.markAllAsReadForUser(1L)).thenReturn(3);

        int count = notificationService.markAllAsRead(1L);

        assertEquals(3, count);
        verify(notificationRepository).markAllAsReadForUser(1L);
    }

    @Test
    void deleteNotification_whenFound_shouldReturnTrue() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        boolean result = notificationService.deleteNotification(1L, 1L);

        assertTrue(result);
        verify(notificationRepository).delete(testNotification);
    }

    @Test
    void deleteNotification_whenNotFound_shouldReturnFalse() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = notificationService.deleteNotification(999L, 1L);

        assertFalse(result);
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void archiveNotification_whenFound_shouldArchive() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(testDTO);

        Optional<NotificationDTO> result = notificationService.archiveNotification(1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(NotificationStatus.ARCHIVED, testNotification.getStatus());
    }

    @Test
    void archiveNotification_whenNotFound_shouldReturnEmpty() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<NotificationDTO> result = notificationService.archiveNotification(999L, 1L);

        assertTrue(result.isEmpty());
    }
}

package in.winvestco.notification_service.service;

import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.mapper.NotificationMapper;
import in.winvestco.notification_service.model.Notification;
import in.winvestco.notification_service.model.NotificationStatus;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Service for managing notifications.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationPreferenceService preferenceService;
    private final WebSocketNotificationService webSocketService;

    /**
     * Create and send a notification.
     */
    @Transactional
    public NotificationDTO createNotification(Long userId, NotificationType type, 
                                               String title, String message, 
                                               Map<String, Object> data) {
        log.info("Creating notification for user {}: type={}, title={}", userId, type, title);

        // Check if notification is muted
        if (preferenceService.isNotificationMuted(userId, type)) {
            log.debug("Notification muted for user {}: type={}", userId, type);
            return null;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationDTO dto = notificationMapper.toDTO(saved);

        // Send via WebSocket
        webSocketService.sendToUser(userId, dto);

        log.info("Created notification {} for user {}", saved.getId(), userId);
        return dto;
    }

    /**
     * Get notifications for a user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotificationsForUser(Long userId, Pageable pageable) {
        log.debug("Fetching notifications for user: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toDTO);
    }

    /**
     * Get notifications for a user with specific status.
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotificationsByStatus(Long userId, NotificationStatus status, 
                                                           Pageable pageable) {
        log.debug("Fetching {} notifications for user: {}", status, userId);
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                .map(notificationMapper::toDTO);
    }

    /**
     * Get unread count for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public Optional<NotificationDTO> markAsRead(Long notificationId, Long userId) {
        log.info("Marking notification {} as read for user {}", notificationId, userId);
        
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(notification -> {
                    notification.markAsRead();
                    Notification saved = notificationRepository.save(notification);
                    return notificationMapper.toDTO(saved);
                });
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user {}", userId);
        return notificationRepository.markAllAsReadForUser(userId);
    }

    /**
     * Delete a notification.
     */
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        log.info("Deleting notification {} for user {}", notificationId, userId);
        
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(notification -> {
                    notificationRepository.delete(notification);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Archive a notification.
     */
    @Transactional
    public Optional<NotificationDTO> archiveNotification(Long notificationId, Long userId) {
        log.info("Archiving notification {} for user {}", notificationId, userId);
        
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(notification -> {
                    notification.archive();
                    Notification saved = notificationRepository.save(notification);
                    return notificationMapper.toDTO(saved);
                });
    }
}

package in.winvestco.notification_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.*;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for user-related events from RabbitMQ.
 * Security notifications (login, password change) cannot be muted.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.USER_LOGIN_NOTIFICATION_QUEUE)
    public void handleUserLogin(UserLoginEvent event) {
        log.info("Received UserLoginEvent for user: {}", event.getUserId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("loginTime", event.getLoginTime().toString());
        data.put("ipAddress", event.getIpAddress());
        data.put("loginMethod", event.getLoginMethod());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.USER_LOGIN,
            "New Login Detected",
            String.format("New login to your account from %s at %s via %s",
                event.getIpAddress(), event.getLoginTime(), event.getLoginMethod()),
            data
        );
    }

    @RabbitListener(queues = RabbitMQConfig.USER_PASSWORD_CHANGED_NOTIFICATION_QUEUE)
    public void handlePasswordChanged(UserPasswordChangedEvent event) {
        log.info("Received UserPasswordChangedEvent for user: {}", event.getUserId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("changedAt", event.getChangedAt().toString());
        data.put("changedBy", event.getChangedBy());
        data.put("ipAddress", event.getIpAddress());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.USER_PASSWORD_CHANGED,
            "Password Changed",
            String.format("Your password was changed on %s from IP %s. If this wasn't you, contact support immediately.",
                event.getChangedAt(), event.getIpAddress()),
            data
        );
    }

    @RabbitListener(queues = RabbitMQConfig.USER_STATUS_CHANGED_NOTIFICATION_QUEUE)
    public void handleStatusChanged(UserStatusChangedEvent event) {
        log.info("Received UserStatusChangedEvent for user: {}", event.getUserId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("oldStatus", event.getOldStatus());
        data.put("newStatus", event.getNewStatus());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.USER_STATUS_CHANGED,
            "Account Status Changed",
            String.format("Your account status has been changed to %s.", event.getNewStatus()),
            data
        );
    }
}

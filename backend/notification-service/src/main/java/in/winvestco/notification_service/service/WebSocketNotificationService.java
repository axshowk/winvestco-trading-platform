package in.winvestco.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/**
 * Service for sending notifications via WebSocket.
 * Supports distributed delivery via Redis Pub/Sub.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final RedisNotificationPublisher redisPublisher;

    /**
     * Send notification to a specific user.
     * Publishes to Redis to reach all instances.
     */
    public void sendToUser(Long userId, NotificationDTO notification) {
        try {
            redisPublisher.publish(userId, notification);
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis, falling back to local delivery: {}", e.getMessage());
            // Fallback to local delivery if Redis fails
            sendToUserLocal(userId, notification);
        }
    }

    /**
     * Send notification to a specific user on this instance only.
     * Called by Redis subscriber or as fallback.
     */
    public void sendToUserLocal(Long userId, NotificationDTO notification) {
        Set<WebSocketSession> sessions = sessionManager.getSessionsForUser(userId);

        if (sessions.isEmpty()) {
            log.debug("No active WebSocket sessions for user: {}", userId);
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(message);

            int sentCount = 0;
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        sentCount++;
                    } catch (Exception e) {
                        log.error("Failed to send notification to session {}: {}",
                                session.getId(), e.getMessage());
                    }
                }
            }
            if (sentCount > 0) {
                log.debug("Sent notification {} to {} sessions for user {}",
                        notification.getId(), sentCount, userId);
            }
        } catch (Exception e) {
            log.error("Failed to serialize notification: {}", e.getMessage());
        }
    }

    /**
     * Broadcast notification to all connected users.
     * Publishes to Redis to reach all instances.
     */
    public void broadcast(NotificationDTO notification) {
        try {
            redisPublisher.publishBroadcast(notification);
        } catch (Exception e) {
            log.error("Failed to publish broadcast to Redis, falling back to local: {}", e.getMessage());
            sendBroadcastLocal(notification);
        }
    }

    /**
     * Broadcast to all users on this instance only.
     */
    public void sendBroadcastLocal(NotificationDTO notification) {
        log.info("Broadcasting notification locally: {}", notification.getId());

        try {
            String message = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(message);

            sessionManager.getAllSessions().forEach((userId, sessions) -> {
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (Exception e) {
                            log.error("Failed to broadcast to session {}: {}",
                                    session.getId(), e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize notification for broadcast: {}", e.getMessage());
        }
    }
}

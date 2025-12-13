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
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * Send notification to a specific user via WebSocket.
     */
    public void sendToUser(Long userId, NotificationDTO notification) {
        Set<WebSocketSession> sessions = sessionManager.getSessionsForUser(userId);
        
        if (sessions.isEmpty()) {
            log.debug("No active WebSocket sessions for user: {}", userId);
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        log.debug("Sent notification {} to session {} for user {}", 
                                notification.getId(), session.getId(), userId);
                    } catch (Exception e) {
                        log.error("Failed to send notification to session {}: {}", 
                                session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize notification: {}", e.getMessage());
        }
    }

    /**
     * Broadcast notification to all connected users.
     */
    public void broadcast(NotificationDTO notification) {
        log.info("Broadcasting notification: {}", notification.getId());
        
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

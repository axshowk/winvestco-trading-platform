package in.winvestco.notification_service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * WebSocket handler for notification connections.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        
        if (userId != null) {
            sessionManager.registerSession(userId, session);
            log.info("WebSocket connection established for user: {}", userId);
            
            // Send welcome message
            session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"message\":\"Connected to notification service\"}"));
        } else {
            log.warn("WebSocket connection without user ID, closing session: {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionManager.removeSession(session);
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages (e.g., ping, mark as read)
        String payload = message.getPayload();
        log.debug("Received message: {} from session: {}", payload, session.getId());
        
        // Could handle commands like "MARK_READ:123" here
        if (payload.startsWith("PING")) {
            session.sendMessage(new TextMessage("PONG"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessionManager.removeSession(session);
    }

    /**
     * Extract user ID from session attributes.
     * User ID should be set by the handshake interceptor.
     */
    private Long extractUserId(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        Object userId = attributes.get("userId");
        
        if (userId instanceof Long) {
            return (Long) userId;
        } else if (userId instanceof String) {
            try {
                return Long.parseLong((String) userId);
            } catch (NumberFormatException e) {
                log.error("Invalid userId format: {}", userId);
            }
        }
        return null;
    }
}

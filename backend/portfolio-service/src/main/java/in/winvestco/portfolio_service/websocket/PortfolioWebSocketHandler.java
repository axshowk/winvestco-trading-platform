package in.winvestco.portfolio_service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

/**
 * WebSocket handler for portfolio connections.
 * Handles connection lifecycle and incoming messages.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PortfolioWebSocketHandler extends TextWebSocketHandler {

    private final PortfolioWebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);

        if (userId != null) {
            sessionManager.registerSession(userId, session);
            log.info("Portfolio WebSocket connection established for user: {}", userId);

            // Send welcome message
            sendMessage(session, "{\"type\":\"CONNECTED\",\"message\":\"Connected to portfolio service\"}");
        } else {
            log.warn("Portfolio WebSocket connection without user ID, closing session: {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionManager.removeSession(session);
        log.info("Portfolio WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message: {} from session: {}", payload, session.getId());

        // Handle ping/pong for keep-alive
        if (payload.equals("PING") || payload.equals("{\"type\":\"PING\"}")) {
            sendMessage(session, "{\"type\":\"PONG\"}");
            return;
        }

        // Handle subscription requests
        if (payload.contains("\"type\":\"SUBSCRIBE\"")) {
            sendMessage(session, "{\"type\":\"SUBSCRIBED\",\"message\":\"Subscribed to portfolio updates\"}");
            return;
        }

        // Handle unsubscribe
        if (payload.contains("\"type\":\"UNSUBSCRIBE\"")) {
            sendMessage(session, "{\"type\":\"UNSUBSCRIBED\",\"message\":\"Unsubscribed from portfolio updates\"}");
            return;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Portfolio WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessionManager.removeSession(session);
    }

    /**
     * Send a message to a specific session.
     */
    private void sendMessage(WebSocketSession session, String message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            }
        }
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
        } else if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return null;
    }
}

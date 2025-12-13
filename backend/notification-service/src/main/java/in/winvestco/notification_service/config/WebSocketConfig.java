package in.winvestco.notification_service.config;

import in.winvestco.notification_service.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket configuration for notification service.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(new UserIdHandshakeInterceptor())
                .setAllowedOrigins("*"); // Configure properly for production
    }

    /**
     * Interceptor to extract user ID from request parameters or headers.
     */
    private static class UserIdHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            
            if (request instanceof ServletServerHttpRequest servletRequest) {
                // Try to get userId from query parameter
                String userIdParam = servletRequest.getServletRequest().getParameter("userId");
                if (userIdParam != null) {
                    try {
                        attributes.put("userId", Long.parseLong(userIdParam));
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                
                // Try to get from header (for JWT-based auth)
                String userIdHeader = servletRequest.getServletRequest().getHeader("X-User-Id");
                if (userIdHeader != null) {
                    try {
                        attributes.put("userId", Long.parseLong(userIdHeader));
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            
            return false; // Reject connection without user ID
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Exception exception) {
            // No-op
        }
    }
}

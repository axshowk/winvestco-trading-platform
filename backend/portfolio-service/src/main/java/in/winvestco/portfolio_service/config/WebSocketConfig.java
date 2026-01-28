package in.winvestco.portfolio_service.config;

import in.winvestco.portfolio_service.websocket.PortfolioWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * WebSocket configuration for portfolio service.
 * Enables real-time portfolio updates.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final PortfolioWebSocketHandler portfolioWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(portfolioWebSocketHandler, "/ws/portfolio")
                .addInterceptors(new UserIdHandshakeInterceptor())
                .setAllowedOrigins("*"); // Configure properly for production

        log.info("Registered WebSocket handler at /ws/portfolio");
    }

    /**
     * Interceptor to extract user ID from request parameters or headers.
     * Supports:
     * - Query parameter: ?userId=123
     * - Header: X-User-Id: 123
     * - Authorization header JWT token (future enhancement)
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

                // Try to get from header (for API gateway passthrough)
                String userIdHeader = servletRequest.getServletRequest().getHeader("X-User-Id");
                if (userIdHeader != null) {
                    try {
                        attributes.put("userId", Long.parseLong(userIdHeader));
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }

                // Try to extract from Authorization header (JWT token)
                String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // In a production system, you would decode the JWT here
                    // For now, we require explicit userId parameter or header
                    // Future: integrate with JWT decoder to extract userId claim
                }
            }

            // Reject connection without user ID for security
            return false;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {
            // No-op
        }
    }
}

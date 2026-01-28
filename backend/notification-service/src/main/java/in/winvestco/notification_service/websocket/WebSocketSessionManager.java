package in.winvestco.notification_service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions for users.
 * Allows multiple sessions per user (multiple tabs/devices).
 * Syncs session metadata to Redis for global visibility.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final StringRedisTemplate redisTemplate;

    @Value("${spring.application.instance-id:notification-service-" + "${random.uuid:default}}")
    private String instanceId;

    // userId -> Set of sessions (Local)
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    // sessionId -> userId (Local)
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    private static final String KEY_PREFIX_USER = "ws:user:";
    private static final String KEY_PREFIX_SESSION = "ws:sessions:";

    /**
     * Register a new session for a user.
     */
    public void registerSession(Long userId, WebSocketSession session) {
        String sessionId = session.getId();

        // 1. Local Registration
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(session);
        sessionToUser.put(sessionId, userId);

        // 2. Redis Registration
        try {
            // ws:user:{userId} -> Set of sessionId:instanceId
            String member = sessionId + ":" + getInstanceId();
            redisTemplate.opsForSet().add(KEY_PREFIX_USER + userId, member);

            // ws:sessions:{sessionId} -> Session metadata
            String metadata = String.format("{\"userId\":%d, \"instanceId\":\"%s\", \"connectedAt\":%d}",
                    userId, getInstanceId(), System.currentTimeMillis());
            redisTemplate.opsForValue().set(KEY_PREFIX_SESSION + sessionId, metadata);
        } catch (Exception e) {
            log.error("Failed to register session in Redis: {}", e.getMessage());
        }

        log.info("Registered WebSocket session {} for user {}. Total sessions: {}",
                sessionId, userId, userSessions.get(userId).size());
    }

    /**
     * Remove a session.
     */
    public void removeSession(WebSocketSession session) {
        String sessionId = session.getId();
        Long userId = sessionToUser.remove(sessionId);

        if (userId != null) {
            // 1. Local Removal
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }

            // 2. Redis Removal
            try {
                String member = sessionId + ":" + getInstanceId();
                redisTemplate.opsForSet().remove(KEY_PREFIX_USER + userId, member);
                redisTemplate.delete(KEY_PREFIX_SESSION + sessionId);
            } catch (Exception e) {
                log.error("Failed to remove session from Redis: {}", e.getMessage());
            }

            log.info("Removed WebSocket session {} for user {}", sessionId, userId);
        }
    }

    /**
     * Get all sessions for a user (Local).
     */
    public Set<WebSocketSession> getSessionsForUser(Long userId) {
        return userSessions.getOrDefault(userId, Collections.emptySet());
    }

    /**
     * Get all sessions (Local).
     */
    public Map<Long, Set<WebSocketSession>> getAllSessions() {
        return Collections.unmodifiableMap(userSessions);
    }

    /**
     * Check if user has any active sessions (Local).
     */
    public boolean hasActiveSessions(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Check if user has any active sessions (Global/Redis).
     */
    public boolean hasActiveSessionsGlobal(Long userId) {
        try {
            Long size = redisTemplate.opsForSet().size(KEY_PREFIX_USER + userId);
            return size != null && size > 0;
        } catch (Exception e) {
            log.error("Failed to check global sessions: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get count of active sessions for a user.
     */
    public int getSessionCount(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Get total number of active sessions.
     */
    public int getTotalSessionCount() {
        return sessionToUser.size();
    }

    private String _cachedInstanceId;

    private String getInstanceId() {
        if (_cachedInstanceId == null) {
            // Handle placeholder logic if property is raw
            if (instanceId != null && !instanceId.contains("${")) {
                _cachedInstanceId = instanceId;
            } else {
                _cachedInstanceId = "notification-service-" + UUID.randomUUID().toString();
            }
        }
        return _cachedInstanceId;
    }

    /**
     * Cleanup on shutdown.
     */
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up {} active sessions on shutdown...", sessionToUser.size());
        sessionToUser.keySet().forEach(sessionId -> {
            try {
                redisTemplate.delete(KEY_PREFIX_SESSION + sessionId);
                Long userId = sessionToUser.get(sessionId);
                if (userId != null) {
                    String member = sessionId + ":" + getInstanceId();
                    redisTemplate.opsForSet().remove(KEY_PREFIX_USER + userId, member);
                }
            } catch (Exception e) {
                log.warn("Failed to cleanup session {} in Redis", sessionId);
            }
        });
    }
}

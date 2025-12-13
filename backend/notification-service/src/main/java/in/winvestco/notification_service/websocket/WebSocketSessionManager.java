package in.winvestco.notification_service.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions for users.
 * Allows multiple sessions per user (multiple tabs/devices).
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    // userId -> Set of sessions
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    // sessionId -> userId
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    /**
     * Register a new session for a user.
     */
    public void registerSession(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(session);
        sessionToUser.put(session.getId(), userId);
        
        log.info("Registered WebSocket session {} for user {}. Total sessions: {}", 
                session.getId(), userId, userSessions.get(userId).size());
    }

    /**
     * Remove a session.
     */
    public void removeSession(WebSocketSession session) {
        String sessionId = session.getId();
        Long userId = sessionToUser.remove(sessionId);
        
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            log.info("Removed WebSocket session {} for user {}", sessionId, userId);
        }
    }

    /**
     * Get all sessions for a user.
     */
    public Set<WebSocketSession> getSessionsForUser(Long userId) {
        return userSessions.getOrDefault(userId, Collections.emptySet());
    }

    /**
     * Get all sessions.
     */
    public Map<Long, Set<WebSocketSession>> getAllSessions() {
        return Collections.unmodifiableMap(userSessions);
    }

    /**
     * Check if user has any active sessions.
     */
    public boolean hasActiveSessions(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
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
}

package in.winvestco.portfolio_service.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortfolioWebSocketSessionManagerTest {

    private PortfolioWebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new PortfolioWebSocketSessionManager();
    }

    private WebSocketSession createMockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }

    @Test
    @DisplayName("registerSession should track session for user")
    void registerSession_shouldTrackSession() {
        WebSocketSession session = createMockSession("session-1");

        sessionManager.registerSession(1L, session);

        Set<WebSocketSession> sessions = sessionManager.getSessionsForUser(1L);
        assertEquals(1, sessions.size());
        assertTrue(sessions.contains(session));
    }

    @Test
    @DisplayName("registerSession should support multiple sessions per user")
    void registerSession_multipleSessions_shouldTrackAll() {
        WebSocketSession session1 = createMockSession("session-1");
        WebSocketSession session2 = createMockSession("session-2");

        sessionManager.registerSession(1L, session1);
        sessionManager.registerSession(1L, session2);

        assertEquals(2, sessionManager.getSessionCount(1L));
    }

    @Test
    @DisplayName("removeSession should untrack session")
    void removeSession_shouldUntrack() {
        WebSocketSession session = createMockSession("session-1");
        sessionManager.registerSession(1L, session);

        sessionManager.removeSession(session);

        assertTrue(sessionManager.getSessionsForUser(1L).isEmpty());
        assertFalse(sessionManager.hasActiveSessions(1L));
    }

    @Test
    @DisplayName("removeSession with unknown session should not throw")
    void removeSession_unknownSession_shouldNotThrow() {
        WebSocketSession session = createMockSession("unknown");

        assertDoesNotThrow(() -> sessionManager.removeSession(session));
    }

    @Test
    @DisplayName("getSessionsForUser with unknown user should return empty set")
    void getSessionsForUser_unknownUser_shouldReturnEmpty() {
        Set<WebSocketSession> sessions = sessionManager.getSessionsForUser(999L);

        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }

    @Test
    @DisplayName("hasActiveSessions should return correct status")
    void hasActiveSessions_shouldReturnCorrectStatus() {
        assertFalse(sessionManager.hasActiveSessions(1L));

        WebSocketSession session = createMockSession("session-1");
        sessionManager.registerSession(1L, session);

        assertTrue(sessionManager.hasActiveSessions(1L));
    }

    @Test
    @DisplayName("getTotalSessionCount should count all sessions")
    void getTotalSessionCount_shouldCountAll() {
        assertEquals(0, sessionManager.getTotalSessionCount());

        sessionManager.registerSession(1L, createMockSession("s1"));
        sessionManager.registerSession(2L, createMockSession("s2"));
        sessionManager.registerSession(1L, createMockSession("s3"));

        assertEquals(3, sessionManager.getTotalSessionCount());
    }

    @Test
    @DisplayName("getConnectedUserCount should count distinct users")
    void getConnectedUserCount_shouldCountDistinctUsers() {
        assertEquals(0, sessionManager.getConnectedUserCount());

        sessionManager.registerSession(1L, createMockSession("s1"));
        sessionManager.registerSession(2L, createMockSession("s2"));
        sessionManager.registerSession(1L, createMockSession("s3"));

        assertEquals(2, sessionManager.getConnectedUserCount());
    }

    @Test
    @DisplayName("getAllSessions should return unmodifiable map")
    void getAllSessions_shouldReturnUnmodifiableMap() {
        sessionManager.registerSession(1L, createMockSession("s1"));

        Map<Long, Set<WebSocketSession>> allSessions = sessionManager.getAllSessions();

        assertNotNull(allSessions);
        assertEquals(1, allSessions.size());
        assertThrows(UnsupportedOperationException.class,
                () -> allSessions.put(2L, Set.of()));
    }
}

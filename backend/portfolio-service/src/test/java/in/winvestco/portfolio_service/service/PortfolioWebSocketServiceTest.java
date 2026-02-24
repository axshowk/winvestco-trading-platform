package in.winvestco.portfolio_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.portfolio_service.dto.PortfolioUpdateMessage;
import in.winvestco.portfolio_service.dto.PortfolioUpdateMessage.HoldingUpdate;
import in.winvestco.portfolio_service.dto.PortfolioUpdateMessage.PortfolioSummary;
import in.winvestco.portfolio_service.dto.PortfolioUpdateMessage.PriceUpdate;
import in.winvestco.portfolio_service.websocket.PortfolioWebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioWebSocketServiceTest {

    @Mock
    private PortfolioWebSocketSessionManager sessionManager;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private PortfolioWebSocketService webSocketService;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"TEST\"}");
    }

    @Test
    @DisplayName("sendPriceUpdate should send to user sessions")
    void sendPriceUpdate_shouldSendToUser() throws IOException {
        when(sessionManager.getSessionsForUser(1L)).thenReturn(Set.of(session));
        when(session.isOpen()).thenReturn(true);

        PriceUpdate priceUpdate = PriceUpdate.builder()
                .symbol("RELIANCE")
                .lastPrice(new BigDecimal("2500"))
                .build();

        webSocketService.sendPriceUpdate(1L, "RELIANCE", priceUpdate);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendPortfolioUpdate should send summary to user")
    void sendPortfolioUpdate_shouldSendSummary() throws IOException {
        when(sessionManager.getSessionsForUser(1L)).thenReturn(Set.of(session));
        when(session.isOpen()).thenReturn(true);

        PortfolioSummary summary = PortfolioSummary.builder()
                .portfolioId(1L)
                .currentValue(new BigDecimal("100000"))
                .build();

        webSocketService.sendPortfolioUpdate(1L, 1L, summary);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendHoldingUpdate should send holding data to user")
    void sendHoldingUpdate_shouldSend() throws IOException {
        when(sessionManager.getSessionsForUser(1L)).thenReturn(Set.of(session));
        when(session.isOpen()).thenReturn(true);

        HoldingUpdate holdingUpdate = HoldingUpdate.builder()
                .holdingId(10L)
                .symbol("TCS")
                .build();

        webSocketService.sendHoldingUpdate(1L, holdingUpdate);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendPortfolioValueUpdate should send value update")
    void sendPortfolioValueUpdate_shouldSend() throws IOException {
        when(sessionManager.getSessionsForUser(1L)).thenReturn(Set.of(session));
        when(session.isOpen()).thenReturn(true);

        webSocketService.sendPortfolioValueUpdate(1L, 1L,
                new BigDecimal("100000"), new BigDecimal("5000"), new BigDecimal("5.26"));

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("broadcastPriceUpdate with no sessions should do nothing")
    void broadcastPriceUpdate_noSessions_shouldDoNothing() {
        when(sessionManager.getAllSessions()).thenReturn(Collections.emptyMap());

        PriceUpdate priceUpdate = PriceUpdate.builder().symbol("RELIANCE").build();

        webSocketService.broadcastPriceUpdate("RELIANCE", priceUpdate);

        verify(session, never()).isOpen();
    }

    @Test
    @DisplayName("broadcastPriceUpdate with sessions should send to all")
    void broadcastPriceUpdate_withSessions_shouldSendToAll() throws IOException {
        when(sessionManager.getAllSessions()).thenReturn(Map.of(1L, Set.of(session)));
        when(session.isOpen()).thenReturn(true);

        PriceUpdate priceUpdate = PriceUpdate.builder().symbol("RELIANCE").build();

        webSocketService.broadcastPriceUpdate("RELIANCE", priceUpdate);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendTradeExecutedNotification should format and send")
    void sendTradeExecutedNotification_shouldSend() throws IOException {
        when(sessionManager.getSessionsForUser(1L)).thenReturn(Set.of(session));
        when(session.isOpen()).thenReturn(true);

        webSocketService.sendTradeExecutedNotification(1L, "TCS",
                "Bought", new BigDecimal("10"), new BigDecimal("3500"));

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendToUser with no sessions should not throw")
    void sendToUser_noSessions_shouldNotThrow() {
        when(sessionManager.getSessionsForUser(99L)).thenReturn(Collections.emptySet());

        webSocketService.sendInfoMessage(99L, "test");

        verify(session, never()).isOpen();
    }

    @Test
    @DisplayName("isUserConnected should delegate to session manager")
    void isUserConnected_shouldDelegate() {
        when(sessionManager.hasActiveSessions(1L)).thenReturn(true);
        assertTrue(webSocketService.isUserConnected(1L));

        when(sessionManager.hasActiveSessions(2L)).thenReturn(false);
        assertFalse(webSocketService.isUserConnected(2L));
    }

    @Test
    @DisplayName("connection count methods should delegate")
    void connectionCounts_shouldDelegate() {
        when(sessionManager.getSessionCount(1L)).thenReturn(3);
        assertEquals(3, webSocketService.getUserSessionCount(1L));

        when(sessionManager.getTotalSessionCount()).thenReturn(10);
        assertEquals(10, webSocketService.getTotalConnectionCount());

        when(sessionManager.getConnectedUserCount()).thenReturn(5);
        assertEquals(5, webSocketService.getConnectedUserCount());
    }

    private void assertTrue(boolean value) {
        org.junit.jupiter.api.Assertions.assertTrue(value);
    }

    private void assertFalse(boolean value) {
        org.junit.jupiter.api.Assertions.assertFalse(value);
    }

    private void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}

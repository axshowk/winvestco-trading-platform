package in.winvestco.portfolio_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.portfolio_service.dto.PortfolioUpdateMessage;
import in.winvestco.portfolio_service.dto.PortfolioUpdateMessage.HoldingUpdate;
import in.winvestco.portfolio_service.dto.PortfolioUpdateMessage.PortfolioSummary;
import in.winvestco.portfolio_service.dto.PortfolioUpdateMessage.PriceUpdate;
import in.winvestco.portfolio_service.websocket.PortfolioWebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Service for sending real-time portfolio updates via WebSocket.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioWebSocketService {

    private final PortfolioWebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * Send a price update to a specific user.
     */
    public void sendPriceUpdate(Long userId, String symbol, PriceUpdate priceUpdate) {
        PortfolioUpdateMessage message = PortfolioUpdateMessage.priceUpdate(userId, symbol, priceUpdate);
        sendToUser(userId, message);
    }

    /**
     * Send a portfolio summary update to a user.
     */
    public void sendPortfolioUpdate(Long userId, Long portfolioId, PortfolioSummary summary) {
        PortfolioUpdateMessage message = PortfolioUpdateMessage.portfolioUpdate(userId, portfolioId, summary);
        sendToUser(userId, message);
    }

    /**
     * Send a holding update to a user.
     */
    public void sendHoldingUpdate(Long userId, HoldingUpdate holdingUpdate) {
        PortfolioUpdateMessage message = PortfolioUpdateMessage.holdingUpdate(userId, holdingUpdate);
        sendToUser(userId, message);
    }

    /**
     * Send portfolio value update (lightweight update with just totals).
     */
    public void sendPortfolioValueUpdate(Long userId, Long portfolioId,
            BigDecimal currentValue, BigDecimal profitLoss, BigDecimal profitLossPercent) {
        PortfolioSummary summary = PortfolioSummary.builder()
                .portfolioId(portfolioId)
                .currentValue(currentValue)
                .profitLoss(profitLoss)
                .profitLossPercent(profitLossPercent)
                .build();

        PortfolioUpdateMessage message = PortfolioUpdateMessage.builder()
                .type(PortfolioUpdateMessage.MessageType.PORTFOLIO_VALUE_UPDATE)
                .userId(userId)
                .portfolioId(portfolioId)
                .portfolioSummary(summary)
                .build();

        sendToUser(userId, message);
    }

    /**
     * Broadcast price update to all connected users who hold a specific symbol.
     * This would typically be called when receiving price updates from market data
     * feed.
     */
    public void broadcastPriceUpdate(String symbol, PriceUpdate priceUpdate) {
        Map<Long, Set<WebSocketSession>> allSessions = sessionManager.getAllSessions();

        if (allSessions.isEmpty()) {
            log.debug("No active sessions to broadcast price update for {}", symbol);
            return;
        }

        PortfolioUpdateMessage message = PortfolioUpdateMessage.builder()
                .type(PortfolioUpdateMessage.MessageType.PRICE_UPDATE)
                .symbol(symbol)
                .priceUpdate(priceUpdate)
                .build();

        String jsonMessage = serializeMessage(message);
        if (jsonMessage == null)
            return;

        int sentCount = 0;
        for (Map.Entry<Long, Set<WebSocketSession>> entry : allSessions.entrySet()) {
            for (WebSocketSession session : entry.getValue()) {
                if (sendRawMessage(session, jsonMessage)) {
                    sentCount++;
                }
            }
        }

        log.debug("Broadcasted price update for {} to {} sessions", symbol, sentCount);
    }

    /**
     * Send a trade executed notification to a user.
     */
    public void sendTradeExecutedNotification(Long userId, String symbol,
            String action, BigDecimal quantity, BigDecimal price) {
        String tradeMessage = String.format("%s %s shares of %s at ₹%s",
                action, quantity.toPlainString(), symbol, price.toPlainString());

        PortfolioUpdateMessage message = PortfolioUpdateMessage.builder()
                .type(PortfolioUpdateMessage.MessageType.TRADE_EXECUTED)
                .userId(userId)
                .symbol(symbol)
                .message(tradeMessage)
                .build();

        sendToUser(userId, message);
    }

    /**
     * Send an info message to a user.
     */
    public void sendInfoMessage(Long userId, String infoMessage) {
        sendToUser(userId, PortfolioUpdateMessage.info(infoMessage));
    }

    /**
     * Send an error message to a user.
     */
    public void sendErrorMessage(Long userId, String errorMessage) {
        sendToUser(userId, PortfolioUpdateMessage.error(errorMessage));
    }

    /**
     * Check if a user has any active WebSocket connections.
     */
    public boolean isUserConnected(Long userId) {
        return sessionManager.hasActiveSessions(userId);
    }

    /**
     * Get the number of active sessions for a user.
     */
    public int getUserSessionCount(Long userId) {
        return sessionManager.getSessionCount(userId);
    }

    /**
     * Get total number of active WebSocket connections.
     */
    public int getTotalConnectionCount() {
        return sessionManager.getTotalSessionCount();
    }

    /**
     * Get total number of connected users.
     */
    public int getConnectedUserCount() {
        return sessionManager.getConnectedUserCount();
    }

    // Private helper methods

    private void sendToUser(Long userId, PortfolioUpdateMessage message) {
        Set<WebSocketSession> sessions = sessionManager.getSessionsForUser(userId);

        if (sessions.isEmpty()) {
            log.debug("No active WebSocket sessions for user: {}", userId);
            return;
        }

        String jsonMessage = serializeMessage(message);
        if (jsonMessage == null)
            return;

        int sentCount = 0;
        for (WebSocketSession session : sessions) {
            if (sendRawMessage(session, jsonMessage)) {
                sentCount++;
            }
        }

        log.debug("Sent {} message to {} sessions for user {}",
                message.getType(), sentCount, userId);
    }

    private boolean sendRawMessage(WebSocketSession session, String message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                return true;
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to session {}: {}",
                        session.getId(), e.getMessage());
            }
        }
        return false;
    }

    private String serializeMessage(PortfolioUpdateMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WebSocket message: {}", e.getMessage());
            return null;
        }
    }
}

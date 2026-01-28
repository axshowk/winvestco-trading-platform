package in.winvestco.portfolio_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for WebSocket portfolio update messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioUpdateMessage {

    /**
     * Type of update message.
     */
    private MessageType type;

    /**
     * Timestamp of the update.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Portfolio ID (if applicable).
     */
    private Long portfolioId;

    /**
     * User ID (for routing).
     */
    private Long userId;

    /**
     * Stock symbol (for price updates).
     */
    private String symbol;

    /**
     * Price data for the symbol.
     */
    private PriceUpdate priceUpdate;

    /**
     * Full portfolio summary (for full updates).
     */
    private PortfolioSummary portfolioSummary;

    /**
     * Holding update (for individual holding changes).
     */
    private HoldingUpdate holdingUpdate;

    /**
     * Optional message for info/error types.
     */
    private String message;

    /**
     * Message types for WebSocket communication.
     */
    public enum MessageType {
        // Connection lifecycle
        CONNECTED,
        DISCONNECTED,
        SUBSCRIBED,
        UNSUBSCRIBED,
        PONG,

        // Updates
        PRICE_UPDATE, // Real-time price change for a symbol
        HOLDING_UPDATE, // Individual holding updated
        PORTFOLIO_UPDATE, // Full portfolio summary updated
        PORTFOLIO_VALUE_UPDATE, // Just the total value changed

        // Notifications
        TRADE_EXECUTED, // A trade was executed
        DIVIDEND_RECEIVED, // Dividend credited

        // System
        ERROR,
        INFO
    }

    /**
     * Price update data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceUpdate {
        private String symbol;
        private BigDecimal lastPrice;
        private BigDecimal change;
        private BigDecimal changePercent;
        private BigDecimal dayHigh;
        private BigDecimal dayLow;
        private Long volume;
        private Instant updatedAt;
    }

    /**
     * Portfolio summary data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioSummary {
        private Long portfolioId;
        private BigDecimal totalInvested;
        private BigDecimal currentValue;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercent;
        private BigDecimal dayProfitLoss;
        private BigDecimal dayProfitLossPercent;
        private Integer holdingsCount;
    }

    /**
     * Individual holding update data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingUpdate {
        private Long holdingId;
        private String symbol;
        private String companyName;
        private BigDecimal quantity;
        private BigDecimal averagePrice;
        private BigDecimal currentPrice;
        private BigDecimal currentValue;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercent;
        private BigDecimal dayChange;
        private BigDecimal dayChangePercent;
    }

    // Factory methods for common message types

    public static PortfolioUpdateMessage priceUpdate(Long userId, String symbol, PriceUpdate price) {
        return PortfolioUpdateMessage.builder()
                .type(MessageType.PRICE_UPDATE)
                .userId(userId)
                .symbol(symbol)
                .priceUpdate(price)
                .build();
    }

    public static PortfolioUpdateMessage portfolioUpdate(Long userId, Long portfolioId, PortfolioSummary summary) {
        return PortfolioUpdateMessage.builder()
                .type(MessageType.PORTFOLIO_UPDATE)
                .userId(userId)
                .portfolioId(portfolioId)
                .portfolioSummary(summary)
                .build();
    }

    public static PortfolioUpdateMessage holdingUpdate(Long userId, HoldingUpdate holding) {
        return PortfolioUpdateMessage.builder()
                .type(MessageType.HOLDING_UPDATE)
                .userId(userId)
                .symbol(holding.getSymbol())
                .holdingUpdate(holding)
                .build();
    }

    public static PortfolioUpdateMessage error(String message) {
        return PortfolioUpdateMessage.builder()
                .type(MessageType.ERROR)
                .message(message)
                .build();
    }

    public static PortfolioUpdateMessage info(String message) {
        return PortfolioUpdateMessage.builder()
                .type(MessageType.INFO)
                .message(message)
                .build();
    }
}

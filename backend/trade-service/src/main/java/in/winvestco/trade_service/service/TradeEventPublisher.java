package in.winvestco.trade_service.service;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.*;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.trade_service.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for publishing trade events using the outbox pattern.
 * Events are captured in the outbox table within the same transaction
 * as the data changes, ensuring atomicity and guaranteed delivery.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeEventPublisher {

    private final OutboxService outboxService;

    /**
     * Publish TradeCreatedEvent using outbox pattern
     */
    public void publishTradeCreated(Trade trade) {
        TradeCreatedEvent event = TradeCreatedEvent.builder()
                .tradeId(trade.getTradeId())
                .orderId(trade.getOrderId())
                .userId(trade.getUserId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .tradeType(trade.getTradeType())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .status(trade.getStatus())
                .createdAt(trade.getCreatedAt())
                .build();

        log.info("Capturing TradeCreatedEvent in outbox for trade: {}, order: {}", 
                trade.getTradeId(), trade.getOrderId());
        outboxService.captureEvent("Trade", trade.getTradeId(),
                RabbitMQConfig.TRADE_EXCHANGE, RabbitMQConfig.TRADE_CREATED_ROUTING_KEY, event);
    }

    /**
     * Publish TradePlacedEvent using outbox pattern
     */
    public void publishTradePlaced(Trade trade) {
        TradePlacedEvent event = TradePlacedEvent.builder()
                .tradeId(trade.getTradeId())
                .orderId(trade.getOrderId())
                .userId(trade.getUserId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .status(trade.getStatus())
                .placedAt(trade.getPlacedAt())
                .build();

        log.info("Capturing TradePlacedEvent in outbox for trade: {}", trade.getTradeId());
        outboxService.captureEvent("Trade", trade.getTradeId(),
                RabbitMQConfig.TRADE_EXCHANGE, RabbitMQConfig.TRADE_PLACED_ROUTING_KEY, event);
    }

    /**
     * Publish TradeExecutedEvent using outbox pattern
     */
    public void publishTradeExecuted(Trade trade, boolean isPartialFill) {
        TradeExecutedEvent event = TradeExecutedEvent.builder()
                .tradeId(trade.getTradeId())
                .orderId(trade.getOrderId())
                .userId(trade.getUserId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .executedQuantity(trade.getExecutedQuantity())
                .executedPrice(trade.getAveragePrice())
                .totalValue(trade.getExecutedValue())
                .isPartialFill(isPartialFill)
                .executedAt(trade.getExecutedAt())
                .build();

        log.info("Capturing TradeExecutedEvent in outbox for trade: {}, partial: {}", 
                trade.getTradeId(), isPartialFill);
        outboxService.captureEvent("Trade", trade.getTradeId(),
                RabbitMQConfig.TRADE_EXCHANGE, RabbitMQConfig.TRADE_EXECUTED_ROUTING_KEY, event);
    }

    /**
     * Publish TradeClosedEvent using outbox pattern
     */
    public void publishTradeClosed(Trade trade) {
        TradeClosedEvent event = TradeClosedEvent.builder()
                .tradeId(trade.getTradeId())
                .orderId(trade.getOrderId())
                .userId(trade.getUserId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .executedQuantity(trade.getExecutedQuantity())
                .averagePrice(trade.getAveragePrice())
                .totalValue(trade.getExecutedValue())
                .status(trade.getStatus())
                .closedAt(trade.getClosedAt())
                .build();

        log.info("Capturing TradeClosedEvent in outbox for trade: {}", trade.getTradeId());
        outboxService.captureEvent("Trade", trade.getTradeId(),
                RabbitMQConfig.TRADE_EXCHANGE, RabbitMQConfig.TRADE_CLOSED_ROUTING_KEY, event);
    }

    /**
     * Publish TradeCancelledEvent using outbox pattern
     */
    public void publishTradeCancelled(Trade trade, String cancelledBy, String reason) {
        TradeCancelledEvent event = TradeCancelledEvent.builder()
                .tradeId(trade.getTradeId())
                .orderId(trade.getOrderId())
                .userId(trade.getUserId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .executedQuantity(trade.getExecutedQuantity())
                .cancelledBy(cancelledBy)
                .cancelReason(reason)
                .previousStatus(trade.getStatus())
                .cancelledAt(Instant.now())
                .build();

        log.info("Capturing TradeCancelledEvent in outbox for trade: {}, reason: {}", 
                trade.getTradeId(), reason);
        outboxService.captureEvent("Trade", trade.getTradeId(),
                RabbitMQConfig.TRADE_EXCHANGE, RabbitMQConfig.TRADE_CANCELLED_ROUTING_KEY, event);
    }

    /**
     * Publish TradeFailedEvent using outbox pattern
     */
    public void publishTradeFailed(Trade trade, String errorCode) {
        TradeFailedEvent event = TradeFailedEvent.builder()
                .tradeId(trade.getTradeId())
                .orderId(trade.getOrderId())
                .userId(trade.getUserId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .quantity(trade.getQuantity())
                .failureReason(trade.getFailureReason())
                .errorCode(errorCode)
                .previousStatus(trade.getStatus())
                .failedAt(Instant.now())
                .build();

        log.info("Capturing TradeFailedEvent in outbox for trade: {}, error: {}", 
                trade.getTradeId(), errorCode);
        outboxService.captureEvent("Trade", trade.getTradeId(),
                RabbitMQConfig.TRADE_EXCHANGE, RabbitMQConfig.TRADE_FAILED_ROUTING_KEY, event);
    }
}

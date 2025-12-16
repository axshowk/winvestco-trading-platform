package in.winvestco.trade_service.service;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.*;
import in.winvestco.trade_service.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for publishing trade events to RabbitMQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish TradeCreatedEvent when a new trade is created.
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

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRADE_EXCHANGE,
                RabbitMQConfig.TRADE_CREATED_ROUTING_KEY,
                event);

        log.info("Published TradeCreatedEvent for trade: {}, order: {}", 
                trade.getTradeId(), trade.getOrderId());
    }

    /**
     * Publish TradePlacedEvent when trade is sent to execution.
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

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRADE_EXCHANGE,
                RabbitMQConfig.TRADE_PLACED_ROUTING_KEY,
                event);

        log.info("Published TradePlacedEvent for trade: {}", trade.getTradeId());
    }

    /**
     * Publish TradeExecutedEvent when trade is executed.
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

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRADE_EXCHANGE,
                RabbitMQConfig.TRADE_EXECUTED_ROUTING_KEY,
                event);

        log.info("Published TradeExecutedEvent for trade: {}, partial: {}", 
                trade.getTradeId(), isPartialFill);
    }

    /**
     * Publish TradeClosedEvent when trade is settled and closed.
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

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRADE_EXCHANGE,
                RabbitMQConfig.TRADE_CLOSED_ROUTING_KEY,
                event);

        log.info("Published TradeClosedEvent for trade: {}", trade.getTradeId());
    }

    /**
     * Publish TradeCancelledEvent when trade is cancelled.
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

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRADE_EXCHANGE,
                RabbitMQConfig.TRADE_CANCELLED_ROUTING_KEY,
                event);

        log.info("Published TradeCancelledEvent for trade: {}, reason: {}", 
                trade.getTradeId(), reason);
    }

    /**
     * Publish TradeFailedEvent when trade fails.
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

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRADE_EXCHANGE,
                RabbitMQConfig.TRADE_FAILED_ROUTING_KEY,
                event);

        log.info("Published TradeFailedEvent for trade: {}, error: {}", 
                trade.getTradeId(), errorCode);
    }
}

package in.winvestco.notification_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.TradeExecutedEvent;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for trade-related events from RabbitMQ.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TradeEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.TRADE_EXECUTED_NOTIFICATION_QUEUE)
    public void handleTradeExecuted(TradeExecutedEvent event) {
        log.info("Received TradeExecutedEvent for trade: {}", event.getTradeId());

        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", event.getTradeId());
        data.put("orderId", event.getOrderId());
        data.put("symbol", event.getSymbol());
        data.put("side", event.getSide().name());
        data.put("executedQuantity", event.getExecutedQuantity());
        data.put("executedPrice", event.getExecutedPrice());
        data.put("totalValue", event.getTotalValue());

        String sideText = event.getSide().name().equals("BUY") ? "bought" : "sold";

        notificationService.createNotification(
                event.getUserId(),
                NotificationType.TRADE_EXECUTED,
                "Trade Executed",
                String.format("You %s %s shares of %s at ₹%s. Total: ₹%s",
                        sideText, event.getExecutedQuantity(), event.getSymbol(),
                        event.getExecutedPrice(), event.getTotalValue()),
                data);
    }

    @RabbitListener(queues = RabbitMQConfig.TRADE_FAILED_NOTIFICATION_QUEUE)
    public void handleTradeFailed(in.winvestco.common.event.TradeFailedEvent event) {
        log.info("Received TradeFailedEvent for trade: {}", event.getTradeId());

        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", event.getTradeId());
        data.put("symbol", event.getSymbol());
        data.put("reason", event.getFailureReason());

        notificationService.createNotification(
                event.getUserId(),
                NotificationType.TRADE_FAILED,
                "Trade Execution Failed",
                String.format("Execution for %s shares of %s failed: %s",
                        event.getQuantity(), event.getSymbol(), event.getFailureReason()),
                data);
    }

    @RabbitListener(queues = RabbitMQConfig.TRADE_CANCELLED_NOTIFICATION_QUEUE)
    public void handleTradeCancelled(in.winvestco.common.event.TradeCancelledEvent event) {
        log.info("Received TradeCancelledEvent for trade: {}", event.getTradeId());

        Map<String, Object> data = new HashMap<>();
        data.put("tradeId", event.getTradeId());
        data.put("symbol", event.getSymbol());
        data.put("reason", event.getCancelReason());

        notificationService.createNotification(
                event.getUserId(),
                NotificationType.TRADE_CANCELLED,
                "Trade Cancelled",
                String.format("Your trade for %s has been cancelled. Reason: %s",
                        event.getSymbol(), event.getCancelReason()),
                data);
    }
}

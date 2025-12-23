package in.winvestco.trade_service.messaging;

import com.rabbitmq.client.Channel;
import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.OrderCancelledEvent;
import in.winvestco.trade_service.model.Trade;
import in.winvestco.trade_service.repository.TradeRepository;
import in.winvestco.trade_service.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Listener for order-related events from RabbitMQ that affect trades.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final TradeService tradeService;
    private final TradeRepository tradeRepository;

    /**
     * Handle OrderCancelledEvent - cancel associated trade if it exists.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_TRADE_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received OrderCancelledEvent for order: {}, user: {}, reason: {}",
                event.getOrderId(), event.getUserId(), event.getCancelReason());

        try {
            // Find trade associated with this order
            Optional<Trade> tradeOpt = tradeRepository.findByOrderId(event.getOrderId());

            if (tradeOpt.isPresent()) {
                Trade trade = tradeOpt.get();
                if (!trade.isTerminal()) {
                    log.info("Cancelling trade {} due to order cancellation", trade.getTradeId());
                    tradeService.cancelTrade(trade.getTradeId(), event.getUserId(),
                            "Order cancelled: " + event.getCancelReason());
                } else {
                    log.info("Trade {} for order {} is already terminal ({}), skipping cancellation",
                            trade.getTradeId(), event.getOrderId(), trade.getStatus());
                }
            } else {
                log.info("No trade found for cancelled order: {}", event.getOrderId());
            }

            // Acknowledge message
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process OrderCancelledEvent for order: {}", event.getOrderId(), e);
            try {
                // Reject and requeue for retry
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception nackEx) {
                log.error("Failed to nack message for order cancellation: {}", event.getOrderId(), nackEx);
            }
        }
    }
}

package in.winvestco.trade_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.FundsLockedEvent;
import in.winvestco.trade_service.dto.CreateTradeRequest;
import in.winvestco.trade_service.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;

/**
 * Listener for FundsLockedEvent from funds-service.
 * Creates trades when funds are successfully locked for an order.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FundsEventListener {

    private final TradeService tradeService;

    /**
     * Handle FundsLockedEvent - create trade when funds are locked.
     */
    @RabbitListener(queues = RabbitMQConfig.FUNDS_LOCKED_TRADE_QUEUE)
    public void handleFundsLocked(FundsLockedEvent event, Channel channel, 
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received FundsLockedEvent for order: {}, user: {}, amount: {}", 
                event.getOrderId(), event.getUserId(), event.getAmount());

        try {
            // Create trade from the order that has funds locked
            CreateTradeRequest request = CreateTradeRequest.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .symbol(event.getSymbol())
                    .side(event.getSide())
                    .tradeType(event.getOrderType())
                    .quantity(event.getQuantity())
                    .price(event.getPrice())
                    .build();

            tradeService.createTradeFromOrder(request);
            
            // Acknowledge the message
            channel.basicAck(deliveryTag, false);
            log.info("Successfully processed FundsLockedEvent for order: {}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to process FundsLockedEvent for order: {}", event.getOrderId(), e);
            try {
                // Reject and requeue for retry
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                log.error("Failed to nack message", ex);
            }
        }
    }
}

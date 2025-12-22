package in.winvestco.order_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.order_service.service.OrderExpiryScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderScheduleListener {

    private final OrderExpiryScheduler orderExpiryScheduler;

    @RabbitListener(queues = RabbitMQConfig.ORDER_EXPIRE_TRIGGER_QUEUE)
    public void handleOrderExpireTrigger(String message) {
        log.info("Received order expiry trigger: {}", message);
        try {
            if ("MARKET_CLOSE_TRIGGER".equals(message)) {
                orderExpiryScheduler.expireOrdersAtMarketClose();
            } else {
                orderExpiryScheduler.checkExpiredOrders();
            }
            log.info("Successfully completed order expiry processing");
        } catch (Exception e) {
            log.error("Error during triggered order expiry", e);
        }
    }
}

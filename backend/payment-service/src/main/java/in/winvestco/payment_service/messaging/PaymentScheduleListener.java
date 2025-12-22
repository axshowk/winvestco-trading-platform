package in.winvestco.payment_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.payment_service.service.PaymentExpiryScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduleListener {

    private final PaymentExpiryScheduler paymentExpiryScheduler;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_EXPIRE_TRIGGER_QUEUE)
    public void handlePaymentExpireTrigger(String message) {
        log.info("Received payment expiry trigger: {}", message);
        try {
            paymentExpiryScheduler.expirePayments();
            log.info("Successfully completed expired payments processing");
        } catch (Exception e) {
            log.error("Error during triggered payment expiry", e);
        }
    }
}

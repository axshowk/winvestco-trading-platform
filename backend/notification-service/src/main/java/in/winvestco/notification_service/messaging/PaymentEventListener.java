package in.winvestco.notification_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.PaymentExpiredEvent;
import in.winvestco.common.event.PaymentFailedEvent;
import in.winvestco.common.event.PaymentSuccessEvent;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for payment-related events from RabbitMQ.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

        private final NotificationService notificationService;

        @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_NOTIFICATION_QUEUE)
        public void handlePaymentSuccess(PaymentSuccessEvent event) {
                log.info("Received PaymentSuccessEvent for account: {}", event.getUserId());

                Map<String, Object> data = new HashMap<>();
                data.put("paymentId", event.getPaymentId());
                data.put("amount", event.getAmount());
                data.put("method", event.getPaymentMethod());

                notificationService.createNotification(
                                event.getUserId(),
                                NotificationType.PAYMENT_SUCCESS,
                                "Deposit Successful",
                                String.format("₹%s has been successfully deposited into your wallet via %s.",
                                                event.getAmount(), event.getPaymentMethod()),
                                data);
        }

        @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_NOTIFICATION_QUEUE)
        public void handlePaymentFailed(PaymentFailedEvent event) {
                log.info("Received PaymentFailedEvent for account: {}", event.getUserId());

                Map<String, Object> data = new HashMap<>();
                data.put("paymentId", event.getPaymentId());
                data.put("amount", event.getAmount());
                data.put("reason", event.getFailureReason());

                notificationService.createNotification(
                                event.getUserId(),
                                NotificationType.PAYMENT_FAILED,
                                "Deposit Failed",
                                String.format("Your deposit of ₹%s failed. Reason: %s",
                                                event.getAmount(), event.getFailureReason()),
                                data);
        }

        @RabbitListener(queues = RabbitMQConfig.PAYMENT_EXPIRED_NOTIFICATION_QUEUE)
        public void handlePaymentExpired(PaymentExpiredEvent event) {
                log.info("Received PaymentExpiredEvent for account: {}", event.getUserId());

                Map<String, Object> data = new HashMap<>();
                data.put("paymentId", event.getPaymentId());
                data.put("amount", event.getAmount());

                notificationService.createNotification(
                                event.getUserId(),
                                NotificationType.PAYMENT_EXPIRED,
                                "Payment Link Expired",
                                String.format("The payment request for ₹%s has expired.", event.getAmount()),
                                data);
        }
}

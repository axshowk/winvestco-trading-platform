package in.winvestco.payment_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.event.PaymentCreatedEvent;
import in.winvestco.common.event.PaymentExpiredEvent;
import in.winvestco.common.event.PaymentFailedEvent;
import in.winvestco.common.event.PaymentSuccessEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.payment_service.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Event publisher for payment events using the outbox pattern.
 * Events are captured in the outbox table within the same transaction
 * as the data changes, ensuring atomicity and guaranteed delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final OutboxService outboxService;

    /**
     * Publish payment created event using outbox pattern
     */
    public void publishPaymentCreated(Payment payment) {
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
            .paymentId(payment.getId())
            .userId(payment.getUserId())
            .amount(payment.getAmount())
            .status(payment.getStatus())
            .provider(payment.getProvider())
            .razorpayOrderId(payment.getRazorpayOrderId())
            .receipt(payment.getReceipt())
            .description(payment.getDescription())
            .createdAt(payment.getCreatedAt())
            .build();

        log.info("Capturing PaymentCreatedEvent in outbox for payment: {}", payment.getId());
        outboxService.captureEvent("Payment", payment.getId().toString(),
                RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.PAYMENT_CREATED_ROUTING_KEY, event);
    }

    /**
     * Publish payment success event using outbox pattern - triggers wallet credit
     */
    public void publishPaymentSuccess(Payment payment) {
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
            .paymentId(payment.getId())
            .userId(payment.getUserId())
            .walletId(payment.getWalletId())
            .amount(payment.getAmount())
            .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : PaymentMethod.OTHER)
            .provider(payment.getProvider())
            .razorpayOrderId(payment.getRazorpayOrderId())
            .razorpayPaymentId(payment.getRazorpayPaymentId())
            .receipt(payment.getReceipt())
            .description(payment.getDescription())
            .completedAt(Instant.now())
            .build();

        log.info("Capturing PaymentSuccessEvent in outbox for payment: {}, amount: {}", 
            payment.getId(), payment.getAmount());
        outboxService.captureEvent("Payment", payment.getId().toString(),
                RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY, event);
    }

    /**
     * Publish payment failed event using outbox pattern
     */
    public void publishPaymentFailed(Payment payment) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
            .paymentId(payment.getId())
            .userId(payment.getUserId())
            .amount(payment.getAmount())
            .provider(payment.getProvider())
            .razorpayOrderId(payment.getRazorpayOrderId())
            .razorpayPaymentId(payment.getRazorpayPaymentId())
            .failureReason(payment.getFailureReason())
            .errorCode(payment.getErrorCode())
            .failedAt(Instant.now())
            .build();

        log.info("Capturing PaymentFailedEvent in outbox for payment: {}", payment.getId());
        outboxService.captureEvent("Payment", payment.getId().toString(),
                RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY, event);
    }

    /**
     * Publish payment expired event using outbox pattern
     */
    public void publishPaymentExpired(Payment payment) {
        PaymentExpiredEvent event = PaymentExpiredEvent.builder()
            .paymentId(payment.getId())
            .userId(payment.getUserId())
            .amount(payment.getAmount())
            .provider(payment.getProvider())
            .razorpayOrderId(payment.getRazorpayOrderId())
            .createdAt(payment.getCreatedAt())
            .expiredAt(Instant.now())
            .build();

        log.info("Capturing PaymentExpiredEvent in outbox for payment: {}", payment.getId());
        outboxService.captureEvent("Payment", payment.getId().toString(),
                RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.PAYMENT_EXPIRED_ROUTING_KEY, event);
    }
}

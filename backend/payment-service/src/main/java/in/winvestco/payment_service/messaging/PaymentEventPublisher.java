package in.winvestco.payment_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.event.PaymentCreatedEvent;
import in.winvestco.common.event.PaymentExpiredEvent;
import in.winvestco.common.event.PaymentFailedEvent;
import in.winvestco.common.event.PaymentSuccessEvent;
import in.winvestco.payment_service.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publisher for payment events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish payment created event
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

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PAYMENT_EXCHANGE,
            RabbitMQConfig.PAYMENT_CREATED_ROUTING_KEY,
            event
        );

        log.info("Published PaymentCreatedEvent for payment: {}", payment.getId());
    }

    /**
     * Publish payment success event - triggers wallet credit
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

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PAYMENT_EXCHANGE,
            RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY,
            event
        );

        log.info("Published PaymentSuccessEvent for payment: {}, amount: {}", 
            payment.getId(), payment.getAmount());
    }

    /**
     * Publish payment failed event
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

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PAYMENT_EXCHANGE,
            RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
            event
        );

        log.info("Published PaymentFailedEvent for payment: {}", payment.getId());
    }

    /**
     * Publish payment expired event
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

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PAYMENT_EXCHANGE,
            RabbitMQConfig.PAYMENT_EXPIRED_ROUTING_KEY,
            event
        );

        log.info("Published PaymentExpiredEvent for payment: {}", payment.getId());
    }
}

package in.winvestco.funds_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.PaymentSuccessEvent;
import in.winvestco.funds_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for payment events from RabbitMQ.
 * Credits wallet when payment is successful.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final WalletService walletService;

    /**
     * Handle PaymentSuccessEvent - credit user's wallet after successful payment
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_FUNDS_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent: paymentId={}, userId={}, amount={}",
                event.getPaymentId(), event.getUserId(), event.getAmount());

        try {
            // Credit the user's wallet
            walletService.creditFunds(
                    event.getUserId(),
                    event.getAmount(),
                    "PAYMENT-" + event.getPaymentId(), // referenceId
                    "PAYMENT", // referenceType
                    buildDescription(event) // description
            );

            log.info("Successfully credited wallet for user: {} with amount: {}",
                    event.getUserId(), event.getAmount());

        } catch (Exception e) {
            log.error("Failed to credit wallet for payment: {}, user: {}",
                    event.getPaymentId(), event.getUserId(), e);
            // Throw to trigger retry/DLQ
            throw e;
        }
    }

    private String buildDescription(PaymentSuccessEvent event) {
        StringBuilder desc = new StringBuilder("Deposit via ");

        if (event.getPaymentMethod() != null) {
            desc.append(event.getPaymentMethod().name());
        } else {
            desc.append("Razorpay");
        }

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            desc.append(" - ").append(event.getDescription());
        }

        return desc.toString();
    }
}

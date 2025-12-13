package in.winvestco.notification_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.*;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for funds-related events from RabbitMQ.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FundsEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.FUNDS_LOCKED_NOTIFICATION_QUEUE)
    public void handleFundsLocked(FundsLockedEvent event) {
        log.info("Received FundsLockedEvent for order: {}", event.getOrderId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("lockedAmount", event.getLockedAmount());
        data.put("lockId", event.getLockId());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.FUNDS_LOCKED,
            "Funds Blocked",
            String.format("₹%s has been blocked for your order.", event.getLockedAmount()),
            data
        );
    }

    @RabbitListener(queues = RabbitMQConfig.FUNDS_RELEASED_NOTIFICATION_QUEUE)
    public void handleFundsReleased(FundsReleasedEvent event) {
        log.info("Received FundsReleasedEvent for order: {}", event.getOrderId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("releasedAmount", event.getReleasedAmount());
        data.put("releaseReason", event.getReleaseReason());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.FUNDS_RELEASED,
            "Funds Released",
            String.format("₹%s has been released back to your wallet.", event.getReleasedAmount()),
            data
        );
    }

    @RabbitListener(queues = RabbitMQConfig.FUNDS_DEPOSITED_NOTIFICATION_QUEUE)
    public void handleFundsDeposited(FundsDepositedEvent event) {
        log.info("Received FundsDepositedEvent for user: {}", event.getUserId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("amount", event.getAmount());
        data.put("newBalance", event.getNewBalance());
        data.put("depositMethod", event.getDepositMethod());
        data.put("referenceId", event.getReferenceId());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.FUNDS_DEPOSITED,
            "Deposit Confirmed",
            String.format("₹%s has been credited to your wallet via %s. New balance: ₹%s",
                event.getAmount(), event.getDepositMethod(), event.getNewBalance()),
            data
        );
    }

    @RabbitListener(queues = RabbitMQConfig.FUNDS_WITHDRAWN_NOTIFICATION_QUEUE)
    public void handleFundsWithdrawn(FundsWithdrawnEvent event) {
        log.info("Received FundsWithdrawnEvent for user: {}", event.getUserId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("amount", event.getAmount());
        data.put("newBalance", event.getNewBalance());
        data.put("withdrawalMethod", event.getWithdrawalMethod());
        data.put("bankAccountLast4", event.getBankAccountLast4());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.FUNDS_WITHDRAWN,
            "Withdrawal Processed",
            String.format("₹%s has been withdrawn to account ending in %s. Remaining balance: ₹%s",
                event.getAmount(), event.getBankAccountLast4(), event.getNewBalance()),
            data
        );
    }
}

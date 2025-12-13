package in.winvestco.funds_service.messaging;

import in.winvestco.common.event.UserCreatedEvent;
import in.winvestco.funds_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for user-related events from RabbitMQ.
 * Creates wallets for new users.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventListener {

    private final WalletService walletService;

    /**
     * Handle UserCreatedEvent - create wallet for new user
     */
    @RabbitListener(queues = "${rabbitmq.queues.user-created:user.created.funds}")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for user: {} ({})", event.getUserId(), event.getEmail());
        
        try {
            walletService.createWalletForUser(event.getUserId());
            log.info("Successfully created wallet for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create wallet for user: {}", event.getUserId(), e);
            // In production, you might want to retry or send to DLQ
            throw e;
        }
    }
}

package in.winvestco.funds_service.service;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.FundsDepositedEvent;
import in.winvestco.common.event.FundsLockedEvent;
import in.winvestco.common.event.FundsReleasedEvent;
import in.winvestco.common.event.FundsWithdrawnEvent;
import in.winvestco.funds_service.model.FundsLock;
import in.winvestco.funds_service.model.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Service for publishing funds-related events to RabbitMQ
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FundsEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish FundsLockedEvent
     */
    public void publishFundsLocked(Long userId, Wallet wallet, FundsLock lock) {
        FundsLockedEvent event = FundsLockedEvent.builder()
                .orderId(lock.getOrderId())
                .userId(userId)
                .walletId(wallet.getId())
                .lockedAmount(lock.getAmount())
                .lockId(lock.getId().toString())
                .lockedAt(Instant.now())
                .build();

        log.info("Publishing FundsLockedEvent for order: {}", lock.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FUNDS_EXCHANGE,
                RabbitMQConfig.FUNDS_LOCKED_ROUTING_KEY,
                event
        );
    }

    /**
     * Publish FundsReleasedEvent
     */
    public void publishFundsReleased(Long userId, Wallet wallet, FundsLock lock, String releaseReason) {
        FundsReleasedEvent event = FundsReleasedEvent.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .orderId(lock.getOrderId())
                .lockId(lock.getId().toString())
                .releasedAmount(lock.getAmount())
                .releaseReason(releaseReason)
                .releasedAt(Instant.now())
                .build();

        log.info("Publishing FundsReleasedEvent for order: {}", lock.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FUNDS_EXCHANGE,
                RabbitMQConfig.FUNDS_RELEASED_ROUTING_KEY,
                event
        );
    }

    /**
     * Publish FundsDepositedEvent
     */
    public void publishFundsDeposited(Long userId, Wallet wallet, BigDecimal amount, 
                                       String referenceId, String depositMethod) {
        FundsDepositedEvent event = FundsDepositedEvent.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .amount(amount)
                .newBalance(wallet.getAvailableBalance())
                .referenceId(referenceId)
                .depositMethod(depositMethod != null ? depositMethod : "BANK_TRANSFER")
                .depositedAt(Instant.now())
                .build();

        log.info("Publishing FundsDepositedEvent for user: {}, amount: {}", userId, amount);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FUNDS_EXCHANGE,
                RabbitMQConfig.FUNDS_DEPOSITED_ROUTING_KEY,
                event
        );
    }

    /**
     * Publish FundsWithdrawnEvent
     */
    public void publishFundsWithdrawn(Long userId, Wallet wallet, BigDecimal amount,
                                       String referenceId, String withdrawalMethod, String bankAccountLast4) {
        FundsWithdrawnEvent event = FundsWithdrawnEvent.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .amount(amount)
                .newBalance(wallet.getAvailableBalance())
                .referenceId(referenceId)
                .withdrawalMethod(withdrawalMethod != null ? withdrawalMethod : "BANK_TRANSFER")
                .bankAccountLast4(bankAccountLast4 != null ? bankAccountLast4 : "****")
                .withdrawnAt(Instant.now())
                .build();

        log.info("Publishing FundsWithdrawnEvent for user: {}, amount: {}", userId, amount);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FUNDS_EXCHANGE,
                RabbitMQConfig.FUNDS_WITHDRAWN_ROUTING_KEY,
                event
        );
    }
}

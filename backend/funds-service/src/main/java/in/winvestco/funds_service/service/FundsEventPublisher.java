package in.winvestco.funds_service.service;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.event.FundsDepositedEvent;
import in.winvestco.common.event.FundsLockedEvent;
import in.winvestco.common.event.FundsReleasedEvent;
import in.winvestco.common.event.FundsWithdrawnEvent;
import in.winvestco.common.event.OrderRejectedEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.model.FundsLock;
import in.winvestco.funds_service.model.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Service for publishing funds-related events using the outbox pattern.
 * Events are captured in the outbox table within the same transaction
 * as the data changes, ensuring atomicity and guaranteed delivery.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FundsEventPublisher {

    private final OutboxService outboxService;

    /**
     * Publish FundsLockedEvent using outbox pattern
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

        log.info("Capturing FundsLockedEvent in outbox for order: {}", lock.getOrderId());
        outboxService.captureEvent("Wallet", wallet.getId().toString(), 
                RabbitMQConfig.FUNDS_EXCHANGE, 
                RabbitMQConfig.FUNDS_LOCKED_ROUTING_KEY,
                event);
    }

    /**
     * Publish FundsReleasedEvent using outbox pattern
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

        log.info("Capturing FundsReleasedEvent in outbox for order: {}", lock.getOrderId());
        outboxService.captureEvent("Wallet", wallet.getId().toString(), 
                RabbitMQConfig.FUNDS_EXCHANGE, 
                RabbitMQConfig.FUNDS_RELEASED_ROUTING_KEY,
                event);
    }

    /**
     * Publish FundsDepositedEvent using outbox pattern
     */
    public void publishFundsDeposited(Long userId, Wallet wallet, BigDecimal amount,
                    BigDecimal balanceBefore, String referenceId, String depositMethod) {
        FundsDepositedEvent event = FundsDepositedEvent.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .amount(amount)
                .balanceBefore(balanceBefore)
                .newBalance(wallet.getAvailableBalance())
                .referenceId(referenceId)
                .depositMethod(depositMethod != null ? depositMethod : "BANK_TRANSFER")
                .depositedAt(Instant.now())
                .build();

        log.info("Capturing FundsDepositedEvent in outbox for user: {}, amount: {}", userId, amount);
        outboxService.captureEvent("Wallet", wallet.getId().toString(), 
                RabbitMQConfig.FUNDS_EXCHANGE, 
                RabbitMQConfig.FUNDS_DEPOSITED_ROUTING_KEY,
                event);
    }

    /**
     * Publish FundsWithdrawnEvent using outbox pattern
     */
    public void publishFundsWithdrawn(Long userId, Wallet wallet, BigDecimal amount,
                    BigDecimal balanceBefore, String referenceId, String withdrawalMethod,
                    String bankAccountLast4) {
        FundsWithdrawnEvent event = FundsWithdrawnEvent.builder()
                .userId(userId)
                .walletId(wallet.getId())
                .amount(amount)
                .balanceBefore(balanceBefore)
                .newBalance(wallet.getAvailableBalance())
                .referenceId(referenceId)
                .withdrawalMethod(withdrawalMethod != null ? withdrawalMethod : "BANK_TRANSFER")
                .bankAccountLast4(bankAccountLast4 != null ? bankAccountLast4 : "****")
                .withdrawnAt(Instant.now())
                .build();

        log.info("Capturing FundsWithdrawnEvent in outbox for user: {}, amount: {}", userId, amount);
        outboxService.captureEvent("Wallet", wallet.getId().toString(), 
                RabbitMQConfig.FUNDS_EXCHANGE, 
                RabbitMQConfig.FUNDS_WITHDRAWN_ROUTING_KEY,
                event);
    }

    /**
     * Publish FundsLockedEvent with trading details using outbox pattern
     */
    public void publishFundsLockedWithDetails(Long userId, Wallet wallet, FundsLockDTO lock,
                    String symbol, OrderSide side, OrderType orderType,
                    BigDecimal quantity, BigDecimal price) {
        FundsLockedEvent event = FundsLockedEvent.builder()
                .orderId(lock.getOrderId())
                .userId(userId)
                .walletId(wallet.getId())
                .lockedAmount(lock.getAmount())
                .lockId(lock.getId().toString())
                .lockedAt(Instant.now())
                // Trading details for trade creation
                .symbol(symbol)
                .side(side)
                .orderType(orderType)
                .quantity(quantity)
                .price(price)
                .build();

        log.info("Capturing FundsLockedEvent with trading details in outbox for order: {}", lock.getOrderId());
        outboxService.captureEvent("Wallet", wallet.getId().toString(), 
                RabbitMQConfig.FUNDS_EXCHANGE, 
                RabbitMQConfig.FUNDS_LOCKED_ROUTING_KEY,
                event);
    }

    /**
     * Publish OrderRejectedEvent using outbox pattern
     */
    public void publishOrderRejected(String orderId, Long userId, String symbol,
                    OrderSide side, OrderType orderType,
                    BigDecimal quantity, BigDecimal price,
                    String rejectionReason) {
        OrderRejectedEvent event = OrderRejectedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .symbol(symbol)
                .side(side)
                .orderType(orderType)
                .quantity(quantity)
                .price(price)
                .rejectionReason(rejectionReason)
                .rejectedBy("FUNDS")
                .rejectedAt(Instant.now())
                .build();

        log.info("Capturing OrderRejectedEvent in outbox for order: {} - {}", orderId, rejectionReason);
        outboxService.captureEvent("Order", orderId, 
                RabbitMQConfig.ORDER_EXCHANGE, 
                RabbitMQConfig.ORDER_REJECTED_ROUTING_KEY,
                event);
    }
}

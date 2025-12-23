package in.winvestco.report_service.messaging;

import in.winvestco.common.event.*;
import in.winvestco.report_service.model.ProcessedEvent;
import in.winvestco.report_service.model.projection.*;
import in.winvestco.report_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event listener for building local projections from domain events.
 * Implements Event Sourcing pattern - subscribes to events and updates local
 * tables.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectionEventListener {

    private final TradeProjectionRepository tradeProjectionRepository;
    private final HoldingProjectionRepository holdingProjectionRepository;
    private final LedgerProjectionRepository ledgerProjectionRepository;
    private final WalletProjectionRepository walletProjectionRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Handle TradeExecutedEvent - update trade and holding projections
     */
    @RabbitListener(queues = "#{T(in.winvestco.common.config.RabbitMQConfig).TRADE_EXECUTED_REPORT_QUEUE}", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void handleTradeExecuted(TradeExecutedEvent event) {
        String eventId = "trade-executed-" + event.getTradeId();
        if (isEventProcessed(eventId)) {
            log.debug("Trade executed event already processed: {}", event.getTradeId());
            return;
        }

        log.info("Processing TradeExecutedEvent: tradeId={}, symbol={}, side={}",
                event.getTradeId(), event.getSymbol(), event.getSide());

        try {
            // Create trade projection
            TradeProjection trade = TradeProjection.builder()
                    .tradeId(event.getTradeId())
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .symbol(event.getSymbol())
                    .side(event.getSide().name())
                    .quantity(event.getExecutedQuantity())
                    .price(event.getExecutedPrice())
                    .executedAt(event.getExecutedAt())
                    .status("EXECUTED")
                    .build();
            tradeProjectionRepository.save(trade);

            // Update holding projection
            updateHoldingFromTrade(event);

            markEventProcessed(eventId, "TradeExecutedEvent");
            log.info("Trade projection created for: {}", event.getTradeId());

        } catch (Exception e) {
            log.error("Failed to process TradeExecutedEvent: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger retry
        }
    }

    /**
     * Handle FundsDepositedEvent - update ledger and wallet projections
     */
    @RabbitListener(queues = "#{T(in.winvestco.common.config.RabbitMQConfig).FUNDS_DEPOSITED_REPORT_QUEUE}", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void handleFundsDeposited(FundsDepositedEvent event) {
        String eventId = "funds-deposited-" + event.getReferenceId();
        if (eventId == null || isEventProcessed(eventId)) {
            return;
        }

        log.info("Processing FundsDepositedEvent: userId={}, amount={}",
                event.getUserId(), event.getAmount());

        try {
            // Create ledger projection
            LedgerProjection ledger = LedgerProjection.builder()
                    .walletId(event.getWalletId())
                    .userId(event.getUserId())
                    .entryType("DEPOSIT")
                    .amount(event.getAmount())
                    .balanceBefore(event.getBalanceBefore())
                    .balanceAfter(event.getNewBalance())
                    .referenceId(event.getReferenceId())
                    .referenceType("DEPOSIT")
                    .description("Funds deposit")
                    .createdAt(event.getDepositedAt())
                    .build();
            ledgerProjectionRepository.save(ledger);

            // Update wallet projection
            updateWalletProjection(event.getUserId(), event.getWalletId(),
                    event.getNewBalance(), BigDecimal.ZERO);

            markEventProcessed(eventId, "FundsDepositedEvent");

        } catch (Exception e) {
            log.error("Failed to process FundsDepositedEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handle FundsWithdrawnEvent - update ledger projection
     */
    @RabbitListener(queues = "#{T(in.winvestco.common.config.RabbitMQConfig).FUNDS_WITHDRAWN_REPORT_QUEUE}", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void handleFundsWithdrawn(FundsWithdrawnEvent event) {
        String eventId = "funds-withdrawn-" + event.getReferenceId();
        if (eventId == null || isEventProcessed(eventId)) {
            return;
        }

        log.info("Processing FundsWithdrawnEvent: userId={}, amount={}",
                event.getUserId(), event.getAmount());

        try {
            LedgerProjection ledger = LedgerProjection.builder()
                    .walletId(event.getWalletId())
                    .userId(event.getUserId())
                    .entryType("WITHDRAWAL")
                    .amount(event.getAmount().negate())
                    .balanceBefore(event.getBalanceBefore())
                    .balanceAfter(event.getNewBalance())
                    .referenceId(event.getReferenceId())
                    .referenceType("WITHDRAWAL")
                    .description("Funds withdrawal")
                    .createdAt(event.getWithdrawnAt())
                    .build();
            ledgerProjectionRepository.save(ledger);

            updateWalletProjection(event.getUserId(), event.getWalletId(),
                    event.getNewBalance(), BigDecimal.ZERO);

            markEventProcessed(eventId, "FundsWithdrawnEvent");

        } catch (Exception e) {
            log.error("Failed to process FundsWithdrawnEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== Helper Methods ====================

    private void updateHoldingFromTrade(TradeExecutedEvent event) {
        HoldingProjection holding = holdingProjectionRepository
                .findByUserIdAndSymbol(event.getUserId(), event.getSymbol())
                .orElse(HoldingProjection.builder()
                        .userId(event.getUserId())
                        .symbol(event.getSymbol())
                        .quantity(BigDecimal.ZERO)
                        .averagePrice(BigDecimal.ZERO)
                        .totalInvested(BigDecimal.ZERO)
                        .build());

        if ("BUY".equalsIgnoreCase(event.getSide().name())) {
            holding.applyBuy(event.getExecutedQuantity(), event.getExecutedPrice());
        } else {
            holding.applySell(event.getExecutedQuantity());
        }

        holdingProjectionRepository.save(holding);
    }

    private void updateWalletProjection(Long userId, Long walletId,
            BigDecimal availableBalance, BigDecimal lockedBalance) {
        WalletProjection wallet = walletProjectionRepository.findByUserId(userId)
                .orElse(WalletProjection.builder()
                        .userId(userId)
                        .walletId(walletId)
                        .currency("INR")
                        .build());

        wallet.setAvailableBalance(availableBalance);
        wallet.setLockedBalance(lockedBalance);
        wallet.setLastUpdatedAt(Instant.now());
        walletProjectionRepository.save(wallet);
    }

    private boolean isEventProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    private void markEventProcessed(String eventId, String eventType) {
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .processedAt(Instant.now())
                .build());
    }
}

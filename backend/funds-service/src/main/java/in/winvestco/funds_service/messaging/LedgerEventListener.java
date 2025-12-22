package in.winvestco.funds_service.messaging;

import in.winvestco.common.event.LedgerEntryEvent;
import in.winvestco.funds_service.config.RabbitMQConfig;
import in.winvestco.funds_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for Ledger events.
 * Part of the CQRS implementation: Projects ledger events into the Wallet read
 * model.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LedgerEventListener {

    private final WalletService walletService;

    @RabbitListener(queues = RabbitMQConfig.LEDGER_RECORDED_FUNDS_QUEUE)
    public void handleLedgerEntryRecorded(LedgerEntryEvent event) {
        log.info("Received LedgerEntryRecorded event: wallet={}, type={}, amount={}",
                event.getWalletId(), event.getEntryType(), event.getAmount());

        try {
            walletService.applyLedgerEvent(event);
            log.debug("Successfully applied ledger event {} to wallet {}", event.getId(), event.getWalletId());
        } catch (Exception e) {
            log.error("Failed to apply ledger event {} to wallet {}: {}",
                    event.getId(), event.getWalletId(), e.getMessage(), e);
            // In a production system, we might want to dead-letter this or trigger a full
            // reconciliation
        }
    }
}

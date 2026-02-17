package in.winvestco.ledger_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.LedgerEntryEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.ledger_service.model.LedgerEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Event publisher for ledger events using the outbox pattern.
 * Events are captured in the outbox table within the same transaction
 * as the data changes, ensuring atomicity and guaranteed delivery.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LedgerEventPublisher {

    private final OutboxService outboxService;

    public void publishLedgerEntryRecorded(LedgerEntry entry) {
        log.info("Capturing LedgerEntryRecorded event in outbox for wallet: {}, entry: {}", 
                entry.getWalletId(), entry.getId());

        LedgerEntryEvent event = LedgerEntryEvent.builder()
                .id(entry.getId())
                .walletId(entry.getWalletId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .balanceBefore(entry.getBalanceBefore())
                .balanceAfter(entry.getBalanceAfter())
                .referenceId(entry.getReferenceId())
                .referenceType(entry.getReferenceType())
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();

        outboxService.captureEvent("Ledger", entry.getId().toString(),
                RabbitMQConfig.LEDGER_EXCHANGE, RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY, event);
    }
}

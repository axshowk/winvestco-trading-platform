package in.winvestco.ledger_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.LedgerEntryEvent;
import in.winvestco.ledger_service.model.LedgerEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LedgerEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishLedgerEntryRecorded(LedgerEntry entry) {
        log.info("Publishing LedgerEntryRecorded event for wallet: {}, entry: {}", entry.getWalletId(), entry.getId());

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

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LEDGER_EXCHANGE,
                RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY,
                event);
    }
}

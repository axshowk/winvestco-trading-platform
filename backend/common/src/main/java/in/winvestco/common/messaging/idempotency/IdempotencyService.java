package in.winvestco.common.messaging.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * Checks if an event has already been processed.
     */
    public boolean exists(String correlationId) {
        return processedEventRepository.existsByCorrelationId(correlationId);
    }

    /**
     * Marks an event as processed.
     * Should be called within the same transaction as the consumer logic.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void markAsProcessed(String correlationId, String consumerName) {
        if (exists(correlationId)) {
            log.warn("Attempting to mark already processed event: {}", correlationId);
            return;
        }

        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .correlationId(correlationId)
                .consumerName(consumerName)
                .build();

        processedEventRepository.save(processedEvent);
        log.debug("Marked event as processed: {} by consumer: {}", correlationId, consumerName);
    }
}

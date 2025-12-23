package in.winvestco.common.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Captures an event in the outbox table.
     * Should be called within an existing transaction.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void captureEvent(String aggregateType, String aggregateId, String exchange, String routingKey,
            BaseEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(event.getClass().getSimpleName())
                    .payload(payload)
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .correlationId(event.getCorrelationId())
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxRepository.save(outboxEvent);
            log.debug("Event captured in outbox: {} for aggregate: {}", event.getClass().getSimpleName(), aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for outbox: {}", event, e);
            throw new RuntimeException("Mapping error during event capture", e);
        }
    }
}

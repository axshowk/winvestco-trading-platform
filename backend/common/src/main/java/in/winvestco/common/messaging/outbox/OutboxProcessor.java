package in.winvestco.common.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.messaging.outbox.poll-interval:5000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findPendingOrFailedEvents(PageRequest.of(0, 50));

        if (events.isEmpty()) {
            return;
        }

        log.debug("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                // Determine the event class
                Class<?> eventClass = Class.forName("in.winvestco.common.event." + event.getEventType());
                Object eventData = objectMapper.readValue(event.getPayload(), eventClass);

                // Publish to RabbitMQ
                rabbitTemplate.convertAndSend(event.getExchange(), event.getRoutingKey(), eventData, m -> {
                    m.getMessageProperties().setCorrelationId(event.getCorrelationId());
                    return m;
                });

                // Update status on success
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setProcessedAt(Instant.now());
                event.setLastError(null);

                log.debug("Successfully published outbox event: {} (id: {})", event.getEventType(), event.getId());

            } catch (Exception e) {
                log.error("Failed to process outbox event: {} (id: {})", event.getEventType(), event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setStatus(OutboxStatus.FAILED);
                event.setLastError(e.getMessage());
            }
        }

        outboxRepository.saveAll(events);
    }
}

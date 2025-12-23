package in.winvestco.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Provides correlation ID and timestamp for deduplication and tracing.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent implements Serializable {

    @Builder.Default
    private String correlationId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant timestamp = Instant.now();
}

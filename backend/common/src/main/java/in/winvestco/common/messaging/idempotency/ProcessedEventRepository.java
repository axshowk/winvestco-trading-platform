package in.winvestco.common.messaging.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    Optional<ProcessedEvent> findByCorrelationId(String correlationId);

    boolean existsByCorrelationId(String correlationId);
}

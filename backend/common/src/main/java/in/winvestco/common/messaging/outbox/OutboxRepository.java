package in.winvestco.common.messaging.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatus(OutboxStatus status, Pageable pageable);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' OR (e.status = 'FAILED' AND e.retryCount < 5)")
    List<OutboxEvent> findPendingOrFailedEvents(Pageable pageable);
}

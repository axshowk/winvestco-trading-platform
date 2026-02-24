package in.winvestco.notification_service.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration — separated from the main application class so that
 * {@code @WebMvcTest} slices can exclude JPA infrastructure without errors.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
        "in.winvestco.notification_service.repository",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
@EntityScan(basePackages = {
        "in.winvestco.notification_service.model",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
public class JpaConfig {
}

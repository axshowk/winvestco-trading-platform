package in.winvestco.order_service.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "in.winvestco.order_service.repository",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
@EntityScan(basePackages = {
        "in.winvestco.order_service.model",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
public class JpaConfig {
}

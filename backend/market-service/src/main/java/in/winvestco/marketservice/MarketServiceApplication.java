package in.winvestco.marketservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
                "in.winvestco.marketservice",
                "in.winvestco.common"
}, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "in\\.winvestco\\.common\\.security\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "in\\.winvestco\\.common\\.config\\.CommonSecurityConfig")
})
@EnableJpaRepositories(basePackages = {
                "in.winvestco.common.messaging.idempotency",
                "in.winvestco.common.messaging.outbox"
})
@EntityScan(basePackages = {
                "in.winvestco.common.messaging.idempotency",
                "in.winvestco.common.messaging.outbox"
})
@EnableDiscoveryClient
@EnableScheduling
public class MarketServiceApplication {

        public static void main(String[] args) {
                SpringApplication.run(MarketServiceApplication.class, args);
        }
}

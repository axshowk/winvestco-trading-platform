package in.winvestco.funds_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Funds Service Application
 * 
 * Manages:
 * - Wallet / Cash Balance (available, locked, total)
 * - Immutable Ledger entries
 * - Funds Locking for orders
 * - Deposits & Withdrawals
 */
@SpringBootApplication(scanBasePackages = {
        "in.winvestco.funds_service",
        "in.winvestco.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "in.winvestco.funds_service.repository",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
@EntityScan(basePackages = {
        "in.winvestco.funds_service.model",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
public class FundsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundsServiceApplication.class, args);
    }
}

package in.winvestco.trade_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Trade Service Application - manages trade lifecycle and execution.
 * 
 * Responsibilities:
 * - Accept trade intent from validated orders
 * - Validate trade business rules
 * - Manage trade state machine (CREATED → VALIDATED → PLACED → EXECUTED →
 * CLOSED)
 * - Trigger execution via events
 * - Emit trade lifecycle events
 * 
 * Does NOT:
 * - Deduct money directly (funds-service)
 * - Maintain portfolio balances (portfolio-service)
 * - Fetch market data (market-service)
 * - Calculate permanent P&L (portfolio-service)
 */
@SpringBootApplication(scanBasePackages = {
        "in.winvestco.trade_service",
        "in.winvestco.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "in.winvestco.trade_service.repository",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
@EntityScan(basePackages = {
        "in.winvestco.trade_service.model",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
public class TradeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeServiceApplication.class, args);
    }
}

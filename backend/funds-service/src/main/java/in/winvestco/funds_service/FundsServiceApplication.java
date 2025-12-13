package in.winvestco.funds_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Funds Service Application
 * 
 * Manages:
 * - Wallet / Cash Balance (available, locked, total)
 * - Immutable Ledger entries
 * - Funds Locking for orders
 * - Deposits & Withdrawals
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@ComponentScan(basePackages = {"in.winvestco.funds_service", "in.winvestco.common"})
public class FundsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundsServiceApplication.class, args);
    }
}

package in.winvestco.funds_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

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
public class FundsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundsServiceApplication.class, args);
    }
}

package in.winvestco.ledger_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Ledger Service Application
 * 
 * IMMUTABLE, AUDIT-COMPLIANT ledger service.
 * This service is the SOURCE OF TRUTH for all financial transactions.
 * 
 * Design Principles:
 * - INSERT ONLY - no updates or deletes allowed
 * - Complete audit trail with timestamps
 * - All services query this for authoritative data
 */
@SpringBootApplication(scanBasePackages = {
        "in.winvestco.ledger_service",
        "in.winvestco.common"
})
@EnableDiscoveryClient
public class LedgerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}

package in.winvestco.report_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Report Service Application
 * 
 * Provides async report generation for:
 * - P&L Statements (realized + unrealized)
 * - Tax Reports (STCG/LTCG for Indian compliance)
 * - Transaction History
 * - Holdings Summary
 * - Trade History
 * 
 * Uses Event Sourcing pattern to build local projection tables
 * from domain events received via RabbitMQ.
 */
@SpringBootApplication(scanBasePackages = {
    "in.winvestco.report_service",
    "in.winvestco.common"
})
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}

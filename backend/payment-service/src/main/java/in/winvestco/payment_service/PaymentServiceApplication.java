package in.winvestco.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Payment Service Application
 * 
 * Handles payment lifecycle with Razorpay integration:
 * - Initiates payments
 * - Verifies webhooks
 * - Marks payment status (SUCCESS/FAILED/EXPIRED)
 * - Emits payment events
 * 
 * Does NOT update balance directly - emits events for funds-service.
 */
@SpringBootApplication(scanBasePackages = {
        "in.winvestco.payment_service",
        "in.winvestco.common"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "in.winvestco.payment_service.repository",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
@EntityScan(basePackages = {
        "in.winvestco.payment_service.model",
        "in.winvestco.common.messaging.idempotency",
        "in.winvestco.common.messaging.outbox"
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

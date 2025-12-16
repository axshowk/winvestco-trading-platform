package in.winvestco.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableJpaAuditing
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

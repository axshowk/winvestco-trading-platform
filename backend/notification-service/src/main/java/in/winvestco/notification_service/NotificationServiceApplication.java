package in.winvestco.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service Application
 * Provides real-time notifications via WebSocket for trading platform events.
 */
@SpringBootApplication(scanBasePackages = {
        "in.winvestco.notification_service",
        "in.winvestco.common"
})
@EnableDiscoveryClient
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

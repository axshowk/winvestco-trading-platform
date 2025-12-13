package in.winvestco.notification_service;

import in.winvestco.common.config.RabbitMQConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service Application
 * Provides real-time notifications via WebSocket for trading platform events.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@Import({RabbitMQConfig.class})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

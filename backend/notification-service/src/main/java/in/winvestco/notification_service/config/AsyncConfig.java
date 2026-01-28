package in.winvestco.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async notification delivery.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool for async notification delivery.
     */
    @Bean(name = "notificationDeliveryExecutor")
    public Executor notificationDeliveryExecutor(NotificationChannelConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getDelivery().getAsyncPoolSize());
        executor.setMaxPoolSize(config.getDelivery().getAsyncPoolSize() * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notif-delivery-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

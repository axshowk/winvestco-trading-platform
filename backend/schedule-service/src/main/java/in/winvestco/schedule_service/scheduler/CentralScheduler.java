package in.winvestco.schedule_service.scheduler;

import in.winvestco.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Centralized scheduler for the entire platform.
 * Publishes trigger events to RabbitMQ to initiate tasks in respective
 * services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CentralScheduler {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Trigger Market Data Fetch every 3 minutes
     * Original: @Scheduled(initialDelay = 0, fixedRate = 180000) in
     * MarketDataScheduler
     */
    @Scheduled(initialDelay = 0, fixedRate = 180000)
    public void triggerMarketDataFetch() {
        log.info("Triggering market data fetch at {}", LocalDateTime.now());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SCHEDULE_EXCHANGE,
                RabbitMQConfig.MARKET_FETCH_TRIGGER_ROUTING_KEY,
                "TRIGGER");
    }

    /**
     * Trigger Order Expiry Check every 5 minutes
     * Original: @Scheduled(cron = "0 * * * * *") in OrderExpiryScheduler
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void triggerOrderExpiryCheck() {
        log.info("Triggering order expiry check at {}", LocalDateTime.now());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SCHEDULE_EXCHANGE,
                RabbitMQConfig.ORDER_EXPIRE_TRIGGER_ROUTING_KEY,
                "TRIGGER");
    }

    /**
     * Trigger Order Expiry at Market Close (15:30 IST)
     * Original: @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Kolkata") in
     * OrderExpiryScheduler
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void triggerMarketCloseOrderExpiry() {
        log.info("Triggering market close order expiry at {}", LocalDateTime.now());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SCHEDULE_EXCHANGE,
                RabbitMQConfig.ORDER_EXPIRE_TRIGGER_ROUTING_KEY,
                "MARKET_CLOSE_TRIGGER");
    }

    /**
     * Trigger Payment Expiry Check every 5 minutes
     * Original: @Scheduled(fixedRate = 60000) in PaymentExpiryScheduler
     */
    @Scheduled(fixedRate = 300000)
    public void triggerPaymentExpiryCheck() {
        log.info("Triggering payment expiry check at {}", LocalDateTime.now());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SCHEDULE_EXCHANGE,
                RabbitMQConfig.PAYMENT_EXPIRE_TRIGGER_ROUTING_KEY,
                "TRIGGER");
    }

    /**
     * Trigger Expired Reports Cleanup daily at 2 AM
     * Original: @Scheduled(cron = "0 0 2 * * *") in ReportService
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void triggerReportCleanup() {
        log.info("Triggering report cleanup at {}", LocalDateTime.now());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SCHEDULE_EXCHANGE,
                RabbitMQConfig.REPORT_CLEANUP_TRIGGER_ROUTING_KEY,
                "TRIGGER");
    }
}

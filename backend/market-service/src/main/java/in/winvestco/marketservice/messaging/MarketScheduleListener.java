package in.winvestco.marketservice.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.marketservice.scheduler.MarketDataScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketScheduleListener {

    private final MarketDataScheduler marketDataScheduler;

    @RabbitListener(queues = RabbitMQConfig.MARKET_FETCH_TRIGGER_QUEUE)
    public void handleMarketFetchTrigger(String message) {
        log.info("Received market fetch trigger: {}", message);
        try {
            marketDataScheduler.fetchAndPublishMarketData();
            log.info("Successfully completed market data fetch and publish");
        } catch (Exception e) {
            log.error("Error during triggered market fetch", e);
        }
    }
}

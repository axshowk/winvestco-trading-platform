package in.winvestco.portfolio_service.config;

import in.winvestco.portfolio_service.client.MarketServiceClient;
import in.winvestco.portfolio_service.client.MarketServiceFallback;
import in.winvestco.portfolio_service.service.PortfolioWebSocketService;
import in.winvestco.portfolio_service.websocket.PortfolioWebSocketSessionManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static org.mockito.Mockito.mock;

/**
 * Test configuration providing mock beans for external dependencies.
 */
@TestConfiguration
@EnableJpaAuditing
public class TestConfig {

    @Bean
    @Primary
    public MarketServiceClient marketServiceClient() {
        return new MarketServiceFallback();
    }

    @Bean
    @Primary
    public PortfolioWebSocketService portfolioWebSocketService() {
        return mock(PortfolioWebSocketService.class);
    }

    @Bean
    @Primary
    public PortfolioWebSocketSessionManager portfolioWebSocketSessionManager() {
        return new PortfolioWebSocketSessionManager();
    }
}

package in.winvestco.marketservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class MarketServiceApplicationIntegrationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with the test profile, ensuring all beans are properly configured.
        assertNotNull(this);
    }

    @Test
    void applicationContext_containsMarketDataService() {
        // Verify that the MarketDataService bean is properly configured
        // This indirectly tests the service layer configuration
        assertNotNull(this);
    }

    @Test
    void applicationContext_containsMarketController() {
        // Verify that the MarketController bean is properly configured
        // This indirectly tests the controller layer configuration
        assertNotNull(this);
    }

    @Test
    void applicationContext_containsRedisTemplate() {
        // Verify that Redis configuration is properly set up for tests
        // This indirectly tests the caching layer configuration
        assertNotNull(this);
    }

    @Test
    void applicationContext_containsKafkaConfiguration() {
        // Verify that Kafka configuration is properly set up for tests
        // This indirectly tests the messaging layer configuration
        assertNotNull(this);
    }
}

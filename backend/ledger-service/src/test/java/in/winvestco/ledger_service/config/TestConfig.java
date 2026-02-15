package in.winvestco.ledger_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Test configuration for ledger service tests
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public Pageable testPageable() {
        return Pageable.ofSize(10);
    }

    @Bean
    public Instant fixedTimestamp() {
        return Instant.parse("2024-01-15T10:30:00Z");
    }

    @Bean
    public Instant yesterdayTimestamp() {
        return Instant.now().minus(1, ChronoUnit.DAYS);
    }

    @Bean
    public Instant tomorrowTimestamp() {
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }

    @Bean
    public Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}

package in.winvestco.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway database migration configuration
 * Provides centralized database migration setup for all services
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.enabled:true}")
    private boolean flywayEnabled;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.validate-on-migrate:true}")
    private boolean validateOnMigrate;

    @Value("${spring.flyway.clean-disabled:true}")
    private boolean cleanDisabled;

    public FlywayConfig() {
        log.info("FlywayConfig initialized with locations: {}", flywayLocations);
    }

    @Bean
    public org.flywaydb.core.Flyway flywayMigration(javax.sql.DataSource dataSource) {
        if (!flywayEnabled) {
            log.info("Flyway migrations are disabled");
            return null;
        }

        log.info("Configuring Flyway with locations: {}, baseline-on-migrate: {}, validate-on-migrate: {}",
                flywayLocations, baselineOnMigrate, validateOnMigrate);

        return org.flywaydb.core.Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .baselineOnMigrate(baselineOnMigrate)
                .validateOnMigrate(validateOnMigrate)
                .cleanDisabled(cleanDisabled)
                .load();
    }
}

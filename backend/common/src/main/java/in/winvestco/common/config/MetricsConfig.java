package in.winvestco.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Centralized metrics configuration for all services.
 * Provides consistent metrics collection, monitoring, and health checks across
 * the application.
 */
@Slf4j
@Configuration
public class MetricsConfig {

    private String serviceName;
    private boolean prometheusEnabled;

    public MetricsConfig(
            @Value("${spring.application.name:Winvestco-Service}") String serviceName,
            @Value("${management.metrics.export.prometheus.enabled:false}") boolean prometheusEnabled) {
        this.serviceName = serviceName;
        this.prometheusEnabled = prometheusEnabled;
        log.info("MetricsConfig initialized for service: {}", this.serviceName);
    }

    /**
     * Configure metrics registry with common tags and filters
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry
                .config()
                .commonTags("service", serviceName, "application", "trading-platform")
                .meterFilter(MeterFilter.deny(id -> {
                    // Filter out high-cardinality metrics that don't add value
                    String name = id.getName();
                    return name.startsWith("jvm.memory.usage") ||
                            name.startsWith("jvm.gc.pause") ||
                            name.startsWith("hikaricp.connections");
                }))
                .meterFilter(MeterFilter.maxExpected("http.server.requests", Duration.ofMillis(1000)));
    }

    /**
     * Configure Prometheus metrics export if enabled
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> prometheusMetricsCustomizer() {
        return registry -> {
            if (prometheusEnabled) {
                log.info("Prometheus metrics export enabled for service: {}", serviceName);
                // Additional Prometheus-specific configurations can be added here
            }
        };
    }

    /**
     * Health check endpoint configuration
     */
    @Bean
    public HealthEndpointHealthIndicator healthEndpointHealthIndicator(HealthEndpoint healthEndpoint) {
        return new HealthEndpointHealthIndicator(healthEndpoint);
    }

    /**
     * Custom health indicator that provides detailed service health information
     */
    public static class HealthEndpointHealthIndicator {

        private final HealthEndpoint healthEndpoint;

        public HealthEndpointHealthIndicator(HealthEndpoint healthEndpoint) {
            this.healthEndpoint = healthEndpoint;
        }

        /**
         * Get comprehensive health status including all health indicators
         */
        public Status getHealthStatus() {
            try {
                org.springframework.boot.actuate.health.HealthComponent health = healthEndpoint.health();
                return health.getStatus();
            } catch (Exception e) {
                log.error("Error checking health status", e);
                return Status.DOWN;
            }
        }

        /**
         * Get detailed health information
         */
        public String getHealthDetails() {
            try {
                org.springframework.boot.actuate.health.HealthComponent health = healthEndpoint.health();
                return health.toString();
            } catch (Exception e) {
                log.error("Error getting health details", e);
                return "Health check unavailable";
            }
        }
    }
}

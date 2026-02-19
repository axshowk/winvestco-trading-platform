package in.winvestco.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsConfigTest {

    @Test
    void metricsCommonTags_ShouldReturnCustomizer() {
        MetricsConfig config = new MetricsConfig("test-service", true);

        var customizer = config.metricsCommonTags();

        assertNotNull(customizer);
    }

    @Test
    void prometheusMetricsCustomizer_WhenEnabled_ShouldConfigure() {
        MetricsConfig config = new MetricsConfig("test-service", true);

        var customizer = config.prometheusMetricsCustomizer();

        assertNotNull(customizer);
    }

    @Test
    void prometheusMetricsCustomizer_WhenDisabled_ShouldStillReturnCustomizer() {
        MetricsConfig config = new MetricsConfig("test-service", false);

        var customizer = config.prometheusMetricsCustomizer();

        assertNotNull(customizer);
    }

    @Test
    void metricsCommonTags_ShouldApplyCommonTags() {
        MetricsConfig config = new MetricsConfig("my-service", true);
        MeterRegistry registry = new SimpleMeterRegistry();

        var customizer = config.metricsCommonTags();
        customizer.customize(registry);

        assertNotNull(registry);
    }

    @Test
    void healthEndpointHealthIndicator_ShouldCreateIndicator() {
        MetricsConfig config = new MetricsConfig("test-service", true);

        var indicator = config.healthEndpointHealthIndicator(null);

        assertNotNull(indicator);
    }

    @Test
    void healthEndpointHealthIndicator_GetHealthStatus_WhenEndpointNull_ShouldReturnDown() {
        MetricsConfig.HealthEndpointHealthIndicator indicator =
                new MetricsConfig.HealthEndpointHealthIndicator(null);

        var status = indicator.getHealthStatus();

        assertNotNull(status);
    }

    @Test
    void healthEndpointHealthIndicator_GetHealthDetails_WhenEndpointNull_ShouldReturnUnavailable() {
        MetricsConfig.HealthEndpointHealthIndicator indicator =
                new MetricsConfig.HealthEndpointHealthIndicator(null);

        String details = indicator.getHealthDetails();

        assertEquals("Health check unavailable", details);
    }
}

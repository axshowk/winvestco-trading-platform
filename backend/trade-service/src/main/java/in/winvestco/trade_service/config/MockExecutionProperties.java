package in.winvestco.trade_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the mock execution engine.
 * Controls execution behavior including delays, slippage, and partial fill
 * simulation.
 */
@Component
@ConfigurationProperties(prefix = "trading.mock-execution")
@Getter
@Setter
public class MockExecutionProperties {

    /**
     * Enable/disable mock execution engine.
     * When disabled, trades remain in PLACED status.
     */
    private boolean enabled = true;

    /**
     * Minimum execution delay in milliseconds.
     * Simulates network latency for realistic execution.
     */
    private int minDelayMs = 100;

    /**
     * Maximum execution delay in milliseconds.
     */
    private int maxDelayMs = 500;

    /**
     * Maximum price slippage percentage (0.01 = 1%).
     * Applied randomly to simulate market impact.
     */
    private double maxSlippagePercent = 0.1;

    /**
     * Automatically close trades after full fill.
     */
    private boolean autoClose = true;

    /**
     * Enable partial fill simulation (70/30 split).
     */
    private boolean partialFillEnabled = true;

    /**
     * Percentage of quantity for first partial fill.
     * Remaining percentage executed in second fill.
     */
    private int partialFillPercent = 70;
}

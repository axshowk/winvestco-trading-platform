package in.winvestco.trade_service.messaging;

import com.rabbitmq.client.Channel;
import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.event.TradePlacedEvent;
import in.winvestco.trade_service.client.MarketDataGrpcClient;
import in.winvestco.trade_service.config.MockExecutionProperties;
import in.winvestco.trade_service.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock execution engine that automatically executes trades.
 * 
 * Listens for TradePlacedEvent and simulates broker execution:
 * - Fetches current market price from market-service
 * - Applies configurable slippage
 * - Simulates network latency with random delays
 * - Executes with optional partial fills (70/30 split)
 * - Transitions trades: PLACED → EXECUTING → FILLED → CLOSED
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MockExecutionEngine {

    private final TradeService tradeService;
    private final MarketDataGrpcClient marketDataGrpcClient;
    private final MockExecutionProperties properties;
    private final Random random = new Random();

    /**
     * Listen for TradePlacedEvent and trigger async execution.
     */
    @RabbitListener(queues = RabbitMQConfig.TRADE_PLACED_MOCK_QUEUE)
    public void handleTradePlaced(TradePlacedEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("MockExecutionEngine received TradePlacedEvent for trade: {}, symbol: {}",
                event.getTradeId(), event.getSymbol());

        try {
            if (!properties.isEnabled()) {
                log.info("Mock execution disabled, skipping trade: {}", event.getTradeId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Trigger async execution (non-blocking)
            asyncExecuteTrade(event);

            // Acknowledge message immediately
            channel.basicAck(deliveryTag, false);
            log.debug("Acknowledged TradePlacedEvent for trade: {}", event.getTradeId());

        } catch (IOException e) {
            log.error("Failed to acknowledge message for trade: {}", event.getTradeId(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("Failed to nack message", ex);
            }
        }
    }

    /**
     * Asynchronously execute a trade with simulated delays and fills.
     */
    @Async("mockExecutionExecutor")
    public void asyncExecuteTrade(TradePlacedEvent event) {
        try {
            log.info("Starting mock execution for trade: {}", event.getTradeId());

            // 1. Get market price
            BigDecimal executionPrice = getExecutionPrice(event.getSymbol(), event.getPrice(), event.getSide());
            log.info("Trade {}: Execution price {} (market with slippage)", event.getTradeId(), executionPrice);

            // 2. Simulate network delay
            simulateDelay();

            // 3. Execute trade (with optional partial fills)
            if (properties.isPartialFillEnabled() && event.getQuantity().compareTo(BigDecimal.ONE) > 0) {
                executeWithPartialFills(event, executionPrice);
            } else {
                executeFull(event, executionPrice);
            }

            log.info("Mock execution completed for trade: {}", event.getTradeId());

        } catch (Exception e) {
            log.error("Failed to execute trade: {} - {}", event.getTradeId(), e.getMessage(), e);
            try {
                tradeService.failTrade(event.getTradeId(), "Mock execution failed: " + e.getMessage(), "EXEC_ERROR");
            } catch (Exception ex) {
                log.error("Failed to mark trade as failed: {}", event.getTradeId(), ex);
            }
        }
    }

    /**
     * Execute trade with 70/30 partial fill simulation.
     */
    private void executeWithPartialFills(TradePlacedEvent event, BigDecimal executionPrice) {
        BigDecimal totalQty = event.getQuantity();

        // First fill (e.g., 70%)
        int firstFillPercent = properties.getPartialFillPercent();
        BigDecimal firstFillQty = totalQty.multiply(BigDecimal.valueOf(firstFillPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);

        // Ensure at least 1 for first fill
        if (firstFillQty.compareTo(BigDecimal.ONE) < 0) {
            firstFillQty = BigDecimal.ONE;
        }

        BigDecimal secondFillQty = totalQty.subtract(firstFillQty);

        log.info("Trade {}: Executing partial fill - first: {}, second: {}",
                event.getTradeId(), firstFillQty, secondFillQty);

        // First partial fill
        tradeService.handleExecutionUpdate(event.getTradeId(), firstFillQty, executionPrice, true);
        log.info("Trade {}: First partial fill completed ({}/{})",
                event.getTradeId(), firstFillQty, totalQty);

        // Delay between fills
        simulateDelay();

        // Second fill (remaining quantity)
        if (secondFillQty.compareTo(BigDecimal.ZERO) > 0) {
            // Slight price variation for second fill
            BigDecimal secondFillPrice = applySlightVariation(executionPrice);
            tradeService.handleExecutionUpdate(event.getTradeId(), secondFillQty, secondFillPrice, false);
            log.info("Trade {}: Second fill completed at {} ({}/{})",
                    event.getTradeId(), secondFillPrice, totalQty, totalQty);
        }

        // Auto-close if enabled
        if (properties.isAutoClose()) {
            tradeService.closeTrade(event.getTradeId());
            log.info("Trade {} closed", event.getTradeId());
        }
    }

    /**
     * Execute full trade without partial fills.
     */
    private void executeFull(TradePlacedEvent event, BigDecimal executionPrice) {
        tradeService.handleExecutionUpdate(event.getTradeId(), event.getQuantity(), executionPrice, false);
        log.info("Trade {} fully executed at {}", event.getTradeId(), executionPrice);

        if (properties.isAutoClose()) {
            tradeService.closeTrade(event.getTradeId());
            log.info("Trade {} closed", event.getTradeId());
        }
    }

    /**
     * Get execution price from market service or fall back to order price.
     * Applies slippage based on order side.
     */
    private BigDecimal getExecutionPrice(String symbol, BigDecimal orderPrice, OrderSide side) {
        BigDecimal marketPrice = fetchMarketPrice(symbol);

        if (marketPrice == null) {
            log.warn("No market price available for {}, using order price: {}", symbol, orderPrice);
            marketPrice = orderPrice;
        }

        // Apply slippage
        double slippagePercent = random.nextDouble() * properties.getMaxSlippagePercent();
        BigDecimal slippage = marketPrice.multiply(BigDecimal.valueOf(slippagePercent / 100));

        // BUY orders: price increases (worse for buyer)
        // SELL orders: price decreases (worse for seller)
        BigDecimal executionPrice = side == OrderSide.BUY
                ? marketPrice.add(slippage)
                : marketPrice.subtract(slippage);

        log.debug("Symbol {}: Market={}, Slippage={}%, Execution={}",
                symbol, marketPrice, String.format("%.4f", slippagePercent), executionPrice);

        return executionPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fetch current market price from market-service via gRPC.
     */
    private BigDecimal fetchMarketPrice(String symbol) {
        return marketDataGrpcClient.getQuote(symbol);
    }

    /**
     * Simulate network delay.
     */
    private void simulateDelay() {
        int delayMs = ThreadLocalRandom.current().nextInt(
                properties.getMinDelayMs(),
                properties.getMaxDelayMs() + 1);

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay interrupted");
        }
    }

    /**
     * Apply slight price variation for subsequent fills.
     */
    private BigDecimal applySlightVariation(BigDecimal price) {
        // ±0.02% variation
        double variation = (random.nextDouble() - 0.5) * 0.0004;
        return price.multiply(BigDecimal.valueOf(1 + variation))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

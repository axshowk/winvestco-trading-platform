package in.winvestco.trade_service.messaging;

import in.winvestco.trade_service.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Listener for execution engine events.
 * 
 * This is a placeholder for handling fills from an external execution engine.
 * In a real implementation, this would listen to events from a broker API
 * or matching engine.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExecutionEventListener {

    private final TradeService tradeService;

    /**
     * Handle execution fill from execution engine.
     * 
     * This method would be called when the execution engine reports a fill.
     * In a real system, this would be a RabbitMQ listener or WebSocket handler.
     * 
     * @param tradeId The trade ID
     * @param executedQuantity Quantity executed in this fill
     * @param executedPrice Price of this fill
     * @param isPartialFill Whether this is a partial fill
     */
    public void handleExecutionFill(String tradeId, BigDecimal executedQuantity, 
                                     BigDecimal executedPrice, boolean isPartialFill) {
        log.info("Received execution fill for trade: {}, qty: {}, price: {}", 
                tradeId, executedQuantity, executedPrice);
        
        try {
            tradeService.handleExecutionUpdate(tradeId, executedQuantity, executedPrice, isPartialFill);
            log.info("Successfully processed execution fill for trade: {}", tradeId);
        } catch (Exception e) {
            log.error("Failed to process execution fill for trade: {}", tradeId, e);
            throw e;
        }
    }

    /**
     * Simulate a full execution for testing/demo purposes.
     * 
     * @param tradeId The trade ID to execute
     * @param quantity Quantity to execute
     * @param price Execution price
     */
    public void simulateFullExecution(String tradeId, BigDecimal quantity, BigDecimal price) {
        log.info("Simulating full execution for trade: {}", tradeId);
        handleExecutionFill(tradeId, quantity, price, false);
        
        // Auto-close the trade after full execution
        tradeService.closeTrade(tradeId);
    }
}

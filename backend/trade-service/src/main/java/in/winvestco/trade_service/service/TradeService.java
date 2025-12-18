package in.winvestco.trade_service.service;

import in.winvestco.common.enums.TradeStatus;
import in.winvestco.trade_service.dto.CreateTradeRequest;
import in.winvestco.trade_service.dto.TradeDTO;
import in.winvestco.trade_service.exception.InvalidTradeStateException;
import in.winvestco.trade_service.exception.TradeNotFoundException;
import in.winvestco.trade_service.mapper.TradeMapper;
import in.winvestco.trade_service.model.Trade;
import in.winvestco.trade_service.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core service for trade lifecycle management.
 * 
 * Responsibilities:
 * - Accept trade intent from validated orders
 * - Validate trade business rules
 * - Manage trade state machine (CREATED → VALIDATED → PLACED → EXECUTED →
 * CLOSED)
 * - Trigger execution via events
 * - Emit trade lifecycle events
 * 
 * Does NOT:
 * - Deduct money directly (funds-service)
 * - Maintain portfolio balances (portfolio-service)
 * - Fetch market data (market-service)
 * - Calculate permanent P&L (portfolio-service)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final TradeMapper tradeMapper;
    private final TradeValidationService validationService;
    private final TradeEventPublisher eventPublisher;

    private static final List<TradeStatus> TERMINAL_STATUSES = List.of(
            TradeStatus.CLOSED, TradeStatus.CANCELLED, TradeStatus.FAILED);

    // ==================== Trade Creation ====================

    /**
     * Create a new trade from a validated order.
     * Called when FundsLockedEvent is received.
     */
    @Transactional
    public TradeDTO createTradeFromOrder(CreateTradeRequest request) {
        log.info("Creating trade for order: {}, symbol: {}", request.getOrderId(), request.getSymbol());

        // Check if trade already exists for this order
        if (tradeRepository.existsByOrderId(request.getOrderId())) {
            log.warn("Trade already exists for order: {}", request.getOrderId());
            return tradeMapper.toDTO(tradeRepository.findByOrderId(request.getOrderId()).orElseThrow());
        }

        // Validate trade request
        validationService.validate(request);

        // Create trade entity
        Trade trade = Trade.builder()
                .tradeId(UUID.randomUUID().toString())
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .symbol(request.getSymbol().toUpperCase())
                .side(request.getSide())
                .tradeType(request.getTradeType())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .status(TradeStatus.CREATED)
                .executedQuantity(BigDecimal.ZERO)
                .build();

        trade = tradeRepository.save(trade);
        log.info("Trade created: {} with status CREATED", trade.getTradeId());

        // Publish trade created event
        eventPublisher.publishTradeCreated(trade);

        // Immediately transition to VALIDATED (validation passed above)
        trade.setStatus(TradeStatus.VALIDATED);
        trade.setValidatedAt(Instant.now());
        trade = tradeRepository.save(trade);
        log.info("Trade {} transitioned to VALIDATED", trade.getTradeId());

        // Auto-place trade for execution
        return placeTrade(trade);
    }

    // ==================== Trade Placement ====================

    /**
     * Place trade for execution.
     * Transitions: VALIDATED → PLACED
     */
    @Transactional
    public TradeDTO placeTrade(Trade trade) {
        if (!trade.canBePlaced()) {
            throw new InvalidTradeStateException(trade.getTradeId(), trade.getStatus(), "place");
        }

        trade.setStatus(TradeStatus.PLACED);
        trade.setPlacedAt(Instant.now());
        trade = tradeRepository.save(trade);

        log.info("Trade {} placed for execution", trade.getTradeId());

        // Publish trade placed event (triggers execution engine)
        eventPublisher.publishTradePlaced(trade);

        return tradeMapper.toDTO(trade);
    }

    // ==================== Execution Updates ====================

    /**
     * Handle execution update from execution engine.
     * Updates filled quantity and transitions status.
     */
    @Transactional
    public TradeDTO handleExecutionUpdate(String tradeId, BigDecimal executedQuantity,
            BigDecimal executedPrice, boolean isPartialFill) {
        Trade trade = findTradeByTradeId(tradeId);

        if (trade.isTerminal()) {
            log.warn("Ignoring execution update for terminal trade: {}", tradeId);
            return tradeMapper.toDTO(trade);
        }

        // Update to EXECUTING if not already
        if (trade.getStatus() == TradeStatus.PLACED) {
            trade.setStatus(TradeStatus.EXECUTING);
        }

        // Calculate new filled quantity
        BigDecimal previousFilledQty = trade.getExecutedQuantity();
        BigDecimal newFilledQty = previousFilledQty.add(executedQuantity);
        trade.setExecutedQuantity(newFilledQty);

        // Calculate weighted average price
        if (trade.getAveragePrice() == null) {
            trade.setAveragePrice(executedPrice);
        } else {
            BigDecimal totalValue = trade.getAveragePrice().multiply(previousFilledQty)
                    .add(executedPrice.multiply(executedQuantity));
            trade.setAveragePrice(totalValue.divide(newFilledQty, 4, RoundingMode.HALF_UP));
        }

        // Update execution time
        trade.setExecutedAt(Instant.now());

        // Determine new status
        if (trade.isFullyFilled()) {
            trade.setStatus(TradeStatus.FILLED);
            log.info("Trade {} fully filled at avg price {}", tradeId, trade.getAveragePrice());
        } else {
            trade.setStatus(TradeStatus.PARTIALLY_FILLED);
            log.info("Trade {} partially filled: {}/{}", tradeId, newFilledQty, trade.getQuantity());
        }

        trade = tradeRepository.save(trade);

        // Publish execution event
        eventPublisher.publishTradeExecuted(trade, !trade.isFullyFilled());

        return tradeMapper.toDTO(trade);
    }

    // ==================== Trade Closure ====================

    /**
     * Close a filled trade.
     * Transitions: FILLED → CLOSED
     */
    @Transactional
    public TradeDTO closeTrade(String tradeId) {
        Trade trade = findTradeByTradeId(tradeId);

        if (trade.getStatus() != TradeStatus.FILLED) {
            throw new InvalidTradeStateException(tradeId, trade.getStatus(), "close");
        }

        trade.setStatus(TradeStatus.CLOSED);
        trade.setClosedAt(Instant.now());
        trade = tradeRepository.save(trade);

        log.info("Trade {} closed", tradeId);

        // Publish trade closed event
        eventPublisher.publishTradeClosed(trade);

        return tradeMapper.toDTO(trade);
    }

    // ==================== Trade Cancellation ====================

    /**
     * Cancel an active trade.
     */
    @Transactional
    public TradeDTO cancelTrade(String tradeId, Long userId, String reason) {
        Trade trade = findTradeByTradeId(tradeId);

        // Verify ownership
        if (!trade.getUserId().equals(userId)) {
            throw new TradeNotFoundException(tradeId);
        }

        if (!trade.isCancellable()) {
            throw new InvalidTradeStateException(tradeId, trade.getStatus(), "cancel");
        }

        trade.setStatus(TradeStatus.CANCELLED);
        trade.setFailureReason("Cancelled: " + reason);
        trade = tradeRepository.save(trade);

        log.info("Trade {} cancelled by user {}: {}", tradeId, userId, reason);

        // Publish cancellation event
        eventPublisher.publishTradeCancelled(trade, "USER", reason);

        return tradeMapper.toDTO(trade);
    }

    // ==================== Trade Failure ====================

    /**
     * Mark a trade as failed.
     */
    @Transactional
    public TradeDTO failTrade(String tradeId, String reason, String errorCode) {
        Trade trade = findTradeByTradeId(tradeId);

        if (trade.isTerminal()) {
            log.warn("Cannot fail terminal trade: {}", tradeId);
            return tradeMapper.toDTO(trade);
        }

        trade.setStatus(TradeStatus.FAILED);
        trade.setFailureReason(reason);
        trade = tradeRepository.save(trade);

        log.error("Trade {} failed: {} ({})", tradeId, reason, errorCode);

        // Publish failure event
        eventPublisher.publishTradeFailed(trade, errorCode);

        return tradeMapper.toDTO(trade);
    }

    // ==================== Query Methods ====================

    /**
     * Get trade by trade ID.
     */
    @Transactional(readOnly = true)
    public TradeDTO getTrade(String tradeId) {
        Trade trade = findTradeByTradeId(tradeId);
        return tradeMapper.toDTO(trade);
    }

    /**
     * Get trade by order ID.
     */
    @Transactional(readOnly = true)
    public TradeDTO getTradeByOrderId(String orderId) {
        Trade trade = tradeRepository.findByOrderId(orderId)
                .orElseThrow(() -> new TradeNotFoundException("No trade found for order: " + orderId));
        return tradeMapper.toDTO(trade);
    }

    /**
     * Get trades for user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<TradeDTO> getTradesForUser(Long userId, Pageable pageable) {
        return tradeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(tradeMapper::toDTO);
    }

    /**
     * Get active (non-terminal) trades for user.
     */
    @Transactional(readOnly = true)
    public List<TradeDTO> getActiveTrades(Long userId) {
        List<Trade> trades = tradeRepository.findActiveTradesByUserId(userId, TERMINAL_STATUSES);
        return tradeMapper.toDTOList(trades);
    }

    // ==================== Helper Methods ====================

    private Trade findTradeByTradeId(String tradeId) {
        return tradeRepository.findByTradeId(tradeId)
                .orElseThrow(() -> new TradeNotFoundException(tradeId));
    }
}

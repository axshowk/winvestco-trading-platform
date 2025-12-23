package in.winvestco.order_service.service;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderValidity;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.dto.OrderDTO;
import in.winvestco.order_service.exception.InvalidOrderStateException;
import in.winvestco.order_service.exception.OrderNotFoundException;
import in.winvestco.order_service.mapper.OrderMapper;
import in.winvestco.order_service.model.Order;
import in.winvestco.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

/**
 * Core service for order lifecycle management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderValidationService validationService;
    private final OrderEventPublisher eventPublisher;

    @Value("${trading.market-close-hour:15}")
    private int marketCloseHour;

    @Value("${trading.market-close-minute:30}")
    private int marketCloseMinute;

    @Value("${trading.timezone:Asia/Kolkata}")
    private String timezone;

    private static final List<OrderStatus> TERMINAL_STATUSES = List.of(
            OrderStatus.FILLED, OrderStatus.CANCELLED,
            OrderStatus.REJECTED, OrderStatus.EXPIRED);

    /**
     * Create a new order
     */
    @Transactional
    public OrderDTO createOrder(Long userId, CreateOrderRequest request) {
        log.info("Creating order for user: {}, symbol: {}", userId, request.getSymbol());

        // Validate order
        validationService.validate(request);

        // Calculate expiry time for DAY orders
        Instant expiresAt = calculateExpiresAt(request.getValidity());

        // Create order entity
        Order order = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .userId(userId)
                .symbol(request.getSymbol().toUpperCase())
                .side(request.getSide())
                .orderType(request.getOrderType())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .stopPrice(request.getStopPrice())
                .validity(request.getValidity() != null ? request.getValidity() : OrderValidity.DAY)
                .expiresAt(expiresAt)
                .status(OrderStatus.NEW)
                .filledQuantity(BigDecimal.ZERO)
                .build();

        order = orderRepository.save(order);
        log.info("Order created: {} with status NEW", order.getOrderId());

        // Publish order created event
        eventPublisher.publishOrderCreated(order);

        // Immediately validate and transition to VALIDATED
        order.setStatus(OrderStatus.VALIDATED);
        order = orderRepository.save(order);

        // Publish validated event (triggers funds-service for BUY orders)
        if (request.getSide() == OrderSide.BUY) {
            eventPublisher.publishOrderValidated(order);
        } else {
            // For SELL orders, skip funds lock and move to PENDING
            order.setStatus(OrderStatus.PENDING);
            order = orderRepository.save(order);
        }

        return orderMapper.toDTO(order);
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrder(String orderId) {
        Order order = findOrderByOrderId(orderId);
        return orderMapper.toDTO(order);
    }

    /**
     * Get orders for user
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersForUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(orderMapper::toDTO);
    }

    /**
     * Get active orders for user
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getActiveOrders(Long userId) {
        List<Order> orders = orderRepository.findActiveOrdersByUserId(userId, TERMINAL_STATUSES);
        return orderMapper.toDTOList(orders);
    }

    /**
     * Cancel an order
     */
    @Transactional
    public OrderDTO cancelOrder(String orderId, Long userId, String reason) {
        Order order = findOrderByOrderId(orderId);

        // Verify ownership
        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException(orderId);
        }

        // Check if order can be cancelled
        if (!order.isCancellable()) {
            throw new InvalidOrderStateException(orderId, order.getStatus(), "cancel");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        log.info("Order {} cancelled by user {}: {}", orderId, userId, reason);

        // Publish cancelled event for notifications
        eventPublisher.publishOrderCancelled(order, reason, "USER");
        eventPublisher.publishOrderUpdated(order);

        return orderMapper.toDTO(order);
    }

    /**
     * Handle order rejected event (e.g., from FundsService due to insufficient
     * funds)
     */
    @Transactional
    public void handleOrderRejected(String orderId, String reason) {
        Order order = findOrderByOrderId(orderId);

        // Transition to REJECTED if not already in terminal state
        if (TERMINAL_STATUSES.contains(order.getStatus())) {
            log.warn("Order {} already in terminal state: {}", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.REJECTED);
        order = orderRepository.save(order);

        log.info("Order {} rejected: {}", orderId, reason);

        // Publish updated event for notifications/UI
        eventPublisher.publishOrderUpdated(order);
    }

    /**
     * Handle funds locked event - transition order to FUNDS_LOCKED
     */
    @Transactional
    public void handleFundsLocked(String orderId, String lockId) {
        Order order = findOrderByOrderId(orderId);

        if (order.getStatus() != OrderStatus.VALIDATED) {
            log.warn("Order {} not in VALIDATED state, current: {}", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.FUNDS_LOCKED);
        order = orderRepository.save(order);

        log.info("Order {} funds locked, transitioning to FUNDS_LOCKED", orderId);

        // Move to PENDING (ready for execution)
        order.setStatus(OrderStatus.PENDING);
        order = orderRepository.save(order);

        eventPublisher.publishOrderUpdated(order);
    }

    /**
     * Handle trade executed event - update order fill
     */
    @Transactional
    public void handleTradeExecuted(String orderId, BigDecimal executedQuantity,
            BigDecimal executedPrice, boolean isPartialFill) {
        Order order = findOrderByOrderId(orderId);

        // Update filled quantity
        BigDecimal newFilledQty = order.getFilledQuantity().add(executedQuantity);
        order.setFilledQuantity(newFilledQty);

        // Calculate new average price (simplified)
        if (order.getAveragePrice() == null) {
            order.setAveragePrice(executedPrice);
        } else {
            // Weighted average
            BigDecimal totalValue = order.getAveragePrice()
                    .multiply(order.getFilledQuantity().subtract(executedQuantity))
                    .add(executedPrice.multiply(executedQuantity));
            order.setAveragePrice(totalValue.divide(newFilledQty, 4, java.math.RoundingMode.HALF_UP));
        }

        // Update status
        if (order.isFullyFilled()) {
            order.setStatus(OrderStatus.FILLED);
            log.info("Order {} fully filled", orderId);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
            log.info("Order {} partially filled: {}/{}", orderId, newFilledQty, order.getQuantity());
        }

        order = orderRepository.save(order);

        // Publish filled event for notifications
        eventPublisher.publishOrderFilled(order);
        eventPublisher.publishOrderUpdated(order);
    }

    /**
     * Expire orders past their expiry time
     */
    @Transactional
    public int expireOrders() {
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.NEW, OrderStatus.VALIDATED,
                OrderStatus.FUNDS_LOCKED, OrderStatus.PENDING,
                OrderStatus.PARTIALLY_FILLED);

        List<Order> expiredOrders = orderRepository.findExpiredOrders(activeStatuses, Instant.now());

        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
            // Publish expired event for notifications
            eventPublisher.publishOrderExpired(order);
            eventPublisher.publishOrderUpdated(order);
            log.info("Order {} expired", order.getOrderId());
        }

        return expiredOrders.size();
    }

    private Order findOrderByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Instant calculateExpiresAt(OrderValidity validity) {
        if (validity == null || validity == OrderValidity.DAY) {
            // Expire at market close (15:30 IST)
            ZoneId zone = ZoneId.of(timezone);
            LocalDate today = LocalDate.now(zone);
            LocalTime closeTime = LocalTime.of(marketCloseHour, marketCloseMinute);
            ZonedDateTime expiryDateTime = ZonedDateTime.of(today, closeTime, zone);

            // If current time is after market close, set to next day
            if (ZonedDateTime.now(zone).isAfter(expiryDateTime)) {
                expiryDateTime = expiryDateTime.plusDays(1);
            }

            return expiryDateTime.toInstant();
        } else if (validity == OrderValidity.GTC) {
            // GTC orders don't expire
            return null;
        } else {
            // IOC orders are handled immediately, no expiry
            return Instant.now();
        }
    }
}

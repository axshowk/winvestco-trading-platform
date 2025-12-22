package in.winvestco.order_service.controller;

import in.winvestco.order_service.dto.CancelOrderRequest;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.dto.OrderDTO;
import in.winvestco.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for order management
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create order", description = "Create a new trading order")
    public ResponseEntity<OrderDTO> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request) {

        Long userId = extractUserId(jwt);
        log.info("Creating order for user: {}", userId);

        OrderDTO order = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order", description = "Get order by ID")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable String orderId) {
        OrderDTO order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    @Operation(summary = "Get orders", description = "Get user's orders with pagination")
    public ResponseEntity<Page<OrderDTO>> getOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Long userId = extractUserId(jwt);
        Page<OrderDTO> orders = orderService.getOrdersForUser(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active orders", description = "Get user's active (non-terminal) orders")
    public ResponseEntity<List<OrderDTO>> getActiveOrders(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        List<OrderDTO> orders = orderService.getActiveOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public ResponseEntity<OrderDTO> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request) {

        Long userId = extractUserId(jwt);
        log.info("Cancelling order {} for user {}", orderId, userId);

        OrderDTO order = orderService.cancelOrder(orderId, userId, request.getReason());
        return ResponseEntity.ok(order);
    }

    private Long extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim == null) {
            throw new RuntimeException("userId claim not found in JWT");
        }
        if (userIdClaim instanceof Number) {
            return ((Number) userIdClaim).longValue();
        }
        return Long.parseLong(userIdClaim.toString());
    }
}

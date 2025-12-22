package in.winvestco.trade_service.controller;

import in.winvestco.trade_service.dto.TradeDTO;
import in.winvestco.trade_service.messaging.ExecutionEventListener;
import in.winvestco.trade_service.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for trade operations.
 */
@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trade Management", description = "APIs for managing trades")
@SecurityRequirement(name = "bearerAuth")
public class TradeController {

        private final TradeService tradeService;
        private final ExecutionEventListener executionEventListener;

        @GetMapping("/{tradeId}")
        @Operation(summary = "Get trade by ID", description = "Retrieves a trade by its unique trade ID", responses = {
                        @ApiResponse(responseCode = "200", description = "Trade found"),
                        @ApiResponse(responseCode = "404", description = "Trade not found")
        })
        public ResponseEntity<TradeDTO> getTrade(
                        @Parameter(description = "Trade ID") @PathVariable String tradeId) {
                log.debug("Getting trade: {}", tradeId);
                return ResponseEntity.ok(tradeService.getTrade(tradeId));
        }

        @GetMapping("/order/{orderId}")
        @Operation(summary = "Get trade by order ID", description = "Retrieves the trade associated with a specific order", responses = {
                        @ApiResponse(responseCode = "200", description = "Trade found"),
                        @ApiResponse(responseCode = "404", description = "Trade not found for order")
        })
        public ResponseEntity<TradeDTO> getTradeByOrderId(
                        @Parameter(description = "Order ID") @PathVariable String orderId) {
                log.debug("Getting trade for order: {}", orderId);
                return ResponseEntity.ok(tradeService.getTradeByOrderId(orderId));
        }

        @GetMapping
        @Operation(summary = "Get user trades", description = "Retrieves all trades for the authenticated user with pagination", responses = {
                        @ApiResponse(responseCode = "200", description = "Trades retrieved successfully")
        })
        public ResponseEntity<Page<TradeDTO>> getUserTrades(
                        @AuthenticationPrincipal Jwt jwt,
                        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
                Long userId = extractUserId(jwt);
                log.debug("Getting trades for user: {}", userId);
                return ResponseEntity.ok(tradeService.getTradesForUser(userId, pageable));
        }

        @GetMapping("/active")
        @Operation(summary = "Get active trades", description = "Retrieves all active (non-terminal) trades for the authenticated user", responses = {
                        @ApiResponse(responseCode = "200", description = "Active trades retrieved successfully")
        })
        public ResponseEntity<List<TradeDTO>> getActiveTrades(@AuthenticationPrincipal Jwt jwt) {
                Long userId = extractUserId(jwt);
                log.debug("Getting active trades for user: {}", userId);
                return ResponseEntity.ok(tradeService.getActiveTrades(userId));
        }

        @PostMapping("/{tradeId}/cancel")
        @Operation(summary = "Cancel a trade", description = "Cancels an active trade", responses = {
                        @ApiResponse(responseCode = "200", description = "Trade cancelled successfully"),
                        @ApiResponse(responseCode = "404", description = "Trade not found"),
                        @ApiResponse(responseCode = "409", description = "Trade cannot be cancelled in current state")
        })
        public ResponseEntity<TradeDTO> cancelTrade(
                        @Parameter(description = "Trade ID") @PathVariable String tradeId,
                        @Parameter(description = "Cancellation reason") @RequestParam(defaultValue = "User requested cancellation") String reason,
                        @AuthenticationPrincipal Jwt jwt) {
                Long userId = extractUserId(jwt);
                log.info("Cancelling trade: {} for user: {}, reason: {}", tradeId, userId, reason);
                return ResponseEntity.ok(tradeService.cancelTrade(tradeId, userId, reason));
        }

        // ==================== Admin/Testing Endpoints ====================

        @PostMapping("/{tradeId}/simulate-execution")
        @Operation(summary = "Simulate trade execution (Testing)", description = "Simulates a full trade execution for testing purposes", responses = {
                        @ApiResponse(responseCode = "200", description = "Execution simulated successfully")
        })
        public ResponseEntity<TradeDTO> simulateExecution(
                        @Parameter(description = "Trade ID") @PathVariable String tradeId,
                        @Parameter(description = "Execution price") @RequestParam BigDecimal price) {
                log.info("Simulating execution for trade: {} at price: {}", tradeId, price);

                TradeDTO trade = tradeService.getTrade(tradeId);
                executionEventListener.simulateFullExecution(tradeId, trade.getQuantity(), price);

                return ResponseEntity.ok(tradeService.getTrade(tradeId));
        }

        // ==================== Helper Methods ====================

        private Long extractUserId(Jwt jwt) {
                Object userIdClaim = jwt.getClaim("userId");
                if (userIdClaim == null) {
                        // Try to get from subject
                        String subject = jwt.getSubject();
                        try {
                                return Long.parseLong(subject);
                        } catch (NumberFormatException e) {
                                throw new IllegalStateException("Unable to extract user ID from JWT");
                        }
                }
                return Long.valueOf(userIdClaim.toString());
        }
}

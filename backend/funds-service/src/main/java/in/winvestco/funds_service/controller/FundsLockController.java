package in.winvestco.funds_service.controller;

import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.dto.LockFundsRequest;
import in.winvestco.funds_service.dto.ReleaseFundsRequest;
import in.winvestco.funds_service.dto.WalletDTO;
import in.winvestco.funds_service.service.FundsLockService;
import in.winvestco.funds_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for funds locking operations.
 * Used internally by order service for order placement.
 */
@RestController
@RequestMapping("/api/funds/locks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Funds Locks", description = "Funds locking operations for orders")
@SecurityRequirement(name = "bearerAuth")
public class FundsLockController {

    private final FundsLockService fundsLockService;
    private final WalletService walletService;

    @PostMapping("/lock")
    @Operation(summary = "Lock funds", description = "Lock funds for an order (internal use)")
    public ResponseEntity<FundsLockDTO> lockFunds(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LockFundsRequest request) {
        
        Long userId = extractUserId(jwt);
        log.info("Locking funds for user {} order {}", userId, request.getOrderId());
        
        FundsLockDTO lock = fundsLockService.lockFunds(
                userId,
                request.getOrderId(),
                request.getAmount(),
                request.getReason()
        );
        return ResponseEntity.ok(lock);
    }

    @PostMapping("/unlock")
    @Operation(summary = "Unlock funds", description = "Release locked funds (order cancel/reject)")
    public ResponseEntity<FundsLockDTO> unlockFunds(@Valid @RequestBody ReleaseFundsRequest request) {
        log.info("Unlocking funds for order {}", request.getOrderId());
        
        FundsLockDTO lock = fundsLockService.releaseFunds(request.getOrderId(), request.getReason());
        return ResponseEntity.ok(lock);
    }

    @PostMapping("/settle")
    @Operation(summary = "Settle funds", description = "Settle locked funds (trade execution)")
    public ResponseEntity<FundsLockDTO> settleFunds(@Valid @RequestBody ReleaseFundsRequest request) {
        log.info("Settling funds for order {}", request.getOrderId());
        
        FundsLockDTO lock = fundsLockService.settleFunds(request.getOrderId(), request.getReason());
        return ResponseEntity.ok(lock);
    }

    @GetMapping
    @Operation(summary = "Get active locks", description = "Get all active locks for the authenticated user")
    public ResponseEntity<List<FundsLockDTO>> getActiveLocks(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        WalletDTO wallet = walletService.getWalletByUserId(userId);
        
        List<FundsLockDTO> locks = fundsLockService.getActiveLocksForWallet(wallet.getId());
        return ResponseEntity.ok(locks);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get lock by order ID", description = "Get a specific lock by its order ID")
    public ResponseEntity<FundsLockDTO> getLockByOrderId(@PathVariable String orderId) {
        FundsLockDTO lock = fundsLockService.getLockByOrderId(orderId);
        return ResponseEntity.ok(lock);
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

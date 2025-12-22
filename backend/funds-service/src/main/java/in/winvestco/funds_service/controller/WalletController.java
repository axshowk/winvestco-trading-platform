package in.winvestco.funds_service.controller;

import in.winvestco.funds_service.dto.LedgerEntryDTO;
import in.winvestco.funds_service.dto.WalletDTO;
import in.winvestco.funds_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for wallet operations.
 * Ledger entries are fetched via ledger-service (SOURCE OF TRUTH).
 */
@RestController
@RequestMapping("/api/v1/funds/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet", description = "Wallet and balance operations")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get wallet balance", description = "Get the authenticated user's wallet balance")
    public ResponseEntity<WalletDTO> getWallet(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.debug("Getting wallet for user: {}", userId);

        WalletDTO wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/ledger")
    @Operation(summary = "Get ledger entries", description = "Get paginated ledger entries from ledger-service")
    public ResponseEntity<Page<LedgerEntryDTO>> getLedger(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = extractUserId(jwt);
        log.debug("Getting ledger for user: {} (page: {}, size: {})", userId, page, size);

        WalletDTO wallet = walletService.getWalletByUserId(userId);
        Page<LedgerEntryDTO> ledger = walletService.getLedgerEntries(wallet.getId(), page, size);

        return ResponseEntity.ok(ledger);
    }

    @GetMapping("/ledger/all")
    @Operation(summary = "Get all ledger entries", description = "Get all ledger entries from ledger-service (for reconciliation)")
    public ResponseEntity<List<LedgerEntryDTO>> getAllLedger(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.debug("Getting all ledger entries for user: {}", userId);

        WalletDTO wallet = walletService.getWalletByUserId(userId);
        List<LedgerEntryDTO> ledger = walletService.getAllLedgerEntries(wallet.getId());

        return ResponseEntity.ok(ledger);
    }

    @GetMapping("/balance")
    @Operation(summary = "Get balance summary", description = "Get a quick balance summary for the authenticated user")
    public ResponseEntity<BalanceSummary> getBalanceSummary(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        WalletDTO wallet = walletService.getWalletByUserId(userId);

        return ResponseEntity.ok(new BalanceSummary(
                wallet.getAvailableBalance(),
                wallet.getLockedBalance(),
                wallet.getTotalBalance()));
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

    @PostMapping("/rebuild")
    @Operation(summary = "Rebuild wallet state", description = "Rebuild the authenticated user's wallet state from ledger events")
    public ResponseEntity<Void> rebuildWallet(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.info("Request to rebuild wallet for user: {}", userId);
        walletService.rebuildWalletStateFromLedger(userId);
        return ResponseEntity.ok().build();
    }

    // Inner class for balance summary response
    public record BalanceSummary(
            java.math.BigDecimal available,
            java.math.BigDecimal locked,
            java.math.BigDecimal total) {
    }
}

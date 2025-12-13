package in.winvestco.funds_service.controller;

import in.winvestco.funds_service.dto.DepositRequest;
import in.winvestco.funds_service.dto.TransactionDTO;
import in.winvestco.funds_service.dto.WithdrawRequest;
import in.winvestco.funds_service.service.TransactionService;
import in.winvestco.funds_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for deposit and withdrawal operations
 */
@RestController
@RequestMapping("/api/funds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Deposit and withdrawal operations")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final WalletService walletService;

    @PostMapping("/deposit")
    @Operation(summary = "Initiate deposit", description = "Initiate a deposit (creates pending transaction)")
    public ResponseEntity<TransactionDTO> initiateDeposit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepositRequest request) {
        
        Long userId = extractUserId(jwt);
        log.info("Initiating deposit for user: {}", userId);
        
        TransactionDTO transaction = transactionService.initiateDeposit(userId, request);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/deposit/confirm")
    @Operation(summary = "Confirm deposit", description = "Confirm a pending deposit (webhook callback)")
    public ResponseEntity<TransactionDTO> confirmDeposit(@RequestParam String reference) {
        log.info("Confirming deposit: {}", reference);
        
        TransactionDTO transaction = transactionService.confirmDeposit(reference);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Initiate withdrawal", description = "Initiate a withdrawal (creates pending transaction)")
    public ResponseEntity<TransactionDTO> initiateWithdrawal(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WithdrawRequest request) {
        
        Long userId = extractUserId(jwt);
        log.info("Initiating withdrawal for user: {}", userId);
        
        TransactionDTO transaction = transactionService.initiateWithdrawal(userId, request);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/withdraw/complete")
    @Operation(summary = "Complete withdrawal", description = "Complete a pending withdrawal")
    public ResponseEntity<TransactionDTO> completeWithdrawal(@RequestParam String reference) {
        log.info("Completing withdrawal: {}", reference);
        
        TransactionDTO transaction = transactionService.completeWithdrawal(reference);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get transaction history", description = "Get paginated transaction history for the authenticated user")
    public ResponseEntity<Page<TransactionDTO>> getTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long userId = extractUserId(jwt);
        log.debug("Getting transactions for user: {} (page: {}, size: {})", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionDTO> transactions = transactionService.getTransactionsForUser(userId, pageable);
        
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/{reference}")
    @Operation(summary = "Get transaction by reference", description = "Get a specific transaction by its external reference")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable String reference) {
        TransactionDTO transaction = transactionService.getTransactionByReference(reference);
        return ResponseEntity.ok(transaction);
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

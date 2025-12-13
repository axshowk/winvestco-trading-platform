package in.winvestco.ledger_service.controller;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.dto.CreateLedgerEntryRequest;
import in.winvestco.ledger_service.dto.LedgerEntryDTO;
import in.winvestco.ledger_service.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * REST controller for ledger operations.
 * 
 * IMMUTABLE LEDGER API:
 * - POST (append new entries) - internal use only
 * - GET (query entries) - for audit and reconciliation
 * 
 * NO PUT, PATCH, or DELETE endpoints exist.
 */
@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ledger", description = "Immutable ledger operations - Source of Truth")
@SecurityRequirement(name = "bearerAuth")
public class LedgerController {

    private final LedgerService ledgerService;

    // ==============================================
    // WRITE ENDPOINT - INSERT ONLY
    // ==============================================

    @PostMapping("/entries")
    @Operation(summary = "Record entry", description = "Append a new ledger entry (internal service use)")
    public ResponseEntity<LedgerEntryDTO> recordEntry(@Valid @RequestBody CreateLedgerEntryRequest request) {
        log.info("Recording ledger entry for wallet: {}", request.getWalletId());
        LedgerEntryDTO entry = ledgerService.recordEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    // ==============================================
    // READ ENDPOINTS - QUERY ONLY
    // ==============================================

    @GetMapping("/entries/{id}")
    @Operation(summary = "Get entry by ID", description = "Get a specific ledger entry")
    public ResponseEntity<LedgerEntryDTO> getEntry(@PathVariable Long id) {
        return ledgerService.getEntryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "Get wallet entries", description = "Get paginated ledger entries for a wallet")
    public ResponseEntity<Page<LedgerEntryDTO>> getWalletEntries(
            @PathVariable Long walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerEntryDTO> entries = ledgerService.getEntriesForWallet(walletId, pageable);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/wallet/{walletId}/all")
    @Operation(summary = "Get all wallet entries", description = "Get all entries for a wallet (for reconciliation)")
    public ResponseEntity<List<LedgerEntryDTO>> getAllWalletEntries(@PathVariable Long walletId) {
        List<LedgerEntryDTO> entries = ledgerService.getAllEntriesForWallet(walletId);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/wallet/{walletId}/latest")
    @Operation(summary = "Get latest entry", description = "Get the most recent entry for a wallet")
    public ResponseEntity<LedgerEntryDTO> getLatestEntry(@PathVariable Long walletId) {
        return ledgerService.getLatestEntryForWallet(walletId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wallet/{walletId}/type/{entryType}")
    @Operation(summary = "Get entries by type", description = "Get entries filtered by type for a wallet")
    public ResponseEntity<Page<LedgerEntryDTO>> getEntriesByType(
            @PathVariable Long walletId,
            @PathVariable LedgerEntryType entryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerEntryDTO> entries = ledgerService.getEntriesByType(walletId, entryType, pageable);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/reference/{referenceId}")
    @Operation(summary = "Get entries by reference", description = "Get all entries for a reference ID")
    public ResponseEntity<List<LedgerEntryDTO>> getEntriesByReference(@PathVariable String referenceId) {
        List<LedgerEntryDTO> entries = ledgerService.getEntriesByReference(referenceId);
        return ResponseEntity.ok(entries);
    }

    // ==============================================
    // RECONCILIATION ENDPOINTS
    // ==============================================

    @GetMapping("/wallet/{walletId}/sum/{entryType}")
    @Operation(summary = "Sum by type", description = "Get sum of amounts by type for reconciliation")
    public ResponseEntity<BigDecimal> getSumByType(
            @PathVariable Long walletId,
            @PathVariable LedgerEntryType entryType) {
        
        BigDecimal sum = ledgerService.sumAmountsByType(walletId, entryType);
        return ResponseEntity.ok(sum);
    }

    @GetMapping("/wallet/{walletId}/count")
    @Operation(summary = "Count entries", description = "Get total count of entries for a wallet")
    public ResponseEntity<Long> countEntries(@PathVariable Long walletId) {
        long count = ledgerService.countEntriesForWallet(walletId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/audit")
    @Operation(summary = "Audit query", description = "Get all entries in date range for audit")
    public ResponseEntity<List<LedgerEntryDTO>> getAuditEntries(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        
        List<LedgerEntryDTO> entries = ledgerService.getAllEntriesInDateRange(startDate, endDate);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/wallet/{walletId}/audit")
    @Operation(summary = "Wallet audit query", description = "Get wallet entries in date range for audit")
    public ResponseEntity<List<LedgerEntryDTO>> getWalletAuditEntries(
            @PathVariable Long walletId,
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        
        List<LedgerEntryDTO> entries = ledgerService.getEntriesInDateRange(walletId, startDate, endDate);
        return ResponseEntity.ok(entries);
    }
}

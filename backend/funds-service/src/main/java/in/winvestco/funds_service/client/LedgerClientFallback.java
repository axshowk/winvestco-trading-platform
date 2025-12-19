package in.winvestco.funds_service.client;

import in.winvestco.funds_service.dto.CreateLedgerEntryRequest;
import in.winvestco.funds_service.dto.LedgerEntryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback implementation for LedgerClient when ledger-service is unavailable
 * or circuit breaker is OPEN.
 * 
 * CRITICAL: Financial operations should NOT silently fail.
 * This fallback logs the failure and throws exceptions for write operations.
 */
@Component
@Slf4j
public class LedgerClientFallback implements LedgerClient {

    @Override
    public LedgerEntryDTO recordEntry(String idempotencyKey, CreateLedgerEntryRequest request) {
        log.error("[FALLBACK] Ledger service unavailable. Cannot record entry with idempotency key: {}",
                idempotencyKey);
        throw new LedgerServiceUnavailableException(
                "Ledger service is currently unavailable. Please try again later.");
    }

    @Override
    public LedgerEntryDTO recordEntry(CreateLedgerEntryRequest request) {
        log.error("[FALLBACK] Ledger service unavailable. Cannot record entry for wallet: {}",
                request.getWalletId());
        throw new LedgerServiceUnavailableException(
                "Ledger service is currently unavailable. Please try again later.");
    }

    @Override
    public LedgerEntryDTO getEntry(Long id) {
        log.warn("[FALLBACK] Ledger service unavailable. Cannot get entry: {}", id);
        return null;
    }

    @Override
    public Page<LedgerEntryDTO> getWalletEntries(Long walletId, int page, int size) {
        log.warn("[FALLBACK] Ledger service unavailable. Cannot get entries for wallet: {}", walletId);
        return new PageImpl<>(Collections.emptyList());
    }

    @Override
    public List<LedgerEntryDTO> getAllWalletEntries(Long walletId) {
        log.warn("[FALLBACK] Ledger service unavailable. Cannot get all entries for wallet: {}", walletId);
        return Collections.emptyList();
    }

    @Override
    public LedgerEntryDTO getLatestEntry(Long walletId) {
        log.warn("[FALLBACK] Ledger service unavailable. Cannot get latest entry for wallet: {}", walletId);
        return null;
    }

    @Override
    public List<LedgerEntryDTO> getEntriesByReference(String referenceId) {
        log.warn("[FALLBACK] Ledger service unavailable. Cannot get entries for reference: {}", referenceId);
        return Collections.emptyList();
    }

    /**
     * Exception thrown when ledger service is unavailable and a write operation is
     * attempted.
     */
    public static class LedgerServiceUnavailableException extends RuntimeException {
        public LedgerServiceUnavailableException(String message) {
            super(message);
        }
    }
}

package in.winvestco.funds_service.client;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.funds_service.dto.CreateLedgerEntryRequest;
import in.winvestco.funds_service.dto.LedgerEntryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client to communicate with ledger-service.
 * The ledger-service is the SOURCE OF TRUTH for all financial transactions.
 */
@FeignClient(name = "ledger-service", path = "/api/ledger")
public interface LedgerClient {

    /**
     * Record a new ledger entry (APPEND ONLY)
     */
    @PostMapping("/entries")
    LedgerEntryDTO recordEntry(@RequestBody CreateLedgerEntryRequest request);

    /**
     * Get entry by ID
     */
    @GetMapping("/entries/{id}")
    LedgerEntryDTO getEntry(@PathVariable("id") Long id);

    /**
     * Get paginated entries for a wallet
     */
    @GetMapping("/wallet/{walletId}")
    Page<LedgerEntryDTO> getWalletEntries(
            @PathVariable("walletId") Long walletId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size);

    /**
     * Get all entries for a wallet
     */
    @GetMapping("/wallet/{walletId}/all")
    List<LedgerEntryDTO> getAllWalletEntries(@PathVariable("walletId") Long walletId);

    /**
     * Get latest entry for a wallet
     */
    @GetMapping("/wallet/{walletId}/latest")
    LedgerEntryDTO getLatestEntry(@PathVariable("walletId") Long walletId);

    /**
     * Get entries by reference ID
     */
    @GetMapping("/reference/{referenceId}")
    List<LedgerEntryDTO> getEntriesByReference(@PathVariable("referenceId") String referenceId);
}

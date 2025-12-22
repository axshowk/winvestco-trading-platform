package in.winvestco.ledger_service.service;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.dto.CreateLedgerEntryRequest;
import in.winvestco.ledger_service.dto.LedgerEntryDTO;
import in.winvestco.ledger_service.mapper.LedgerMapper;
import in.winvestco.ledger_service.model.LedgerEntry;
import in.winvestco.ledger_service.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Ledger Service - IMMUTABLE SOURCE OF TRUTH
 * 
 * This service ONLY supports:
 * - INSERT (recordEntry)
 * - SELECT (query methods)
 * 
 * NO UPDATE or DELETE operations are allowed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerMapper ledgerMapper;
    private final in.winvestco.ledger_service.messaging.LedgerEventPublisher ledgerEventPublisher;

    // ==============================================
    // WRITE OPERATION - INSERT ONLY
    // ==============================================

    /**
     * Record a new ledger entry (APPEND ONLY).
     * This is the ONLY write operation allowed.
     */
    @Transactional
    public LedgerEntryDTO recordEntry(CreateLedgerEntryRequest request) {
        log.info("Recording ledger entry: wallet={}, type={}, amount={}, ref={}",
                request.getWalletId(), request.getEntryType(), request.getAmount(), request.getReferenceId());

        LedgerEntry entry = LedgerEntry.create(
                request.getWalletId(),
                request.getEntryType(),
                request.getAmount(),
                request.getBalanceBefore(),
                request.getBalanceAfter(),
                request.getReferenceId(),
                request.getReferenceType(),
                request.getDescription());

        LedgerEntry saved = ledgerEntryRepository.save(entry);
        log.info("Recorded ledger entry: id={}, wallet={}, type={}",
                saved.getId(), saved.getWalletId(), saved.getEntryType());

        // Publish event for CQRS projections
        ledgerEventPublisher.publishLedgerEntryRecorded(saved);

        return ledgerMapper.toDTO(saved);
    }

    // ==============================================
    // READ OPERATIONS - QUERY ONLY
    // ==============================================

    /**
     * Get entry by ID
     */
    @Transactional(readOnly = true)
    public Optional<LedgerEntryDTO> getEntryById(Long id) {
        return ledgerEntryRepository.findById(id)
                .map(ledgerMapper::toDTO);
    }

    /**
     * Get paginated entries for a wallet
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntryDTO> getEntriesForWallet(Long walletId, Pageable pageable) {
        return ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
                .map(ledgerMapper::toDTO);
    }

    /**
     * Get all entries for a wallet (for full reconciliation)
     */
    @Transactional(readOnly = true)
    public List<LedgerEntryDTO> getAllEntriesForWallet(Long walletId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtAsc(walletId);
        return ledgerMapper.toDTOList(entries);
    }

    /**
     * Get entries by reference ID (e.g., find all entries for an order)
     */
    @Transactional(readOnly = true)
    public List<LedgerEntryDTO> getEntriesByReference(String referenceId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByReferenceId(referenceId);
        return ledgerMapper.toDTOList(entries);
    }

    /**
     * Get entries in date range (for audit/reconciliation)
     */
    @Transactional(readOnly = true)
    public List<LedgerEntryDTO> getEntriesInDateRange(Long walletId, Instant startDate, Instant endDate) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByWalletIdAndDateRange(walletId, startDate, endDate);
        return ledgerMapper.toDTOList(entries);
    }

    /**
     * Get entries by type for a wallet
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntryDTO> getEntriesByType(Long walletId, LedgerEntryType entryType, Pageable pageable) {
        return ledgerEntryRepository.findByWalletIdAndEntryTypeOrderByCreatedAtDesc(walletId, entryType, pageable)
                .map(ledgerMapper::toDTO);
    }

    /**
     * Get the latest entry for a wallet (for balance verification)
     */
    @Transactional(readOnly = true)
    public Optional<LedgerEntryDTO> getLatestEntryForWallet(Long walletId) {
        return ledgerEntryRepository.findFirstByWalletIdOrderByCreatedAtDesc(walletId)
                .map(ledgerMapper::toDTO);
    }

    /**
     * Get sum of amounts by type (for reconciliation)
     */
    @Transactional(readOnly = true)
    public BigDecimal sumAmountsByType(Long walletId, LedgerEntryType entryType) {
        return ledgerEntryRepository.sumAmountByWalletIdAndEntryType(walletId, entryType);
    }

    /**
     * Count entries for a wallet
     */
    @Transactional(readOnly = true)
    public long countEntriesForWallet(Long walletId) {
        return ledgerEntryRepository.countByWalletId(walletId);
    }

    /**
     * Get all entries in date range (global audit)
     */
    @Transactional(readOnly = true)
    public List<LedgerEntryDTO> getAllEntriesInDateRange(Instant startDate, Instant endDate) {
        List<LedgerEntry> entries = ledgerEntryRepository.findAllByDateRange(startDate, endDate);
        return ledgerMapper.toDTOList(entries);
    }

    /**
     * Get wallet balance at a specific point in time (Point-in-Time Query)
     */
    @Transactional(readOnly = true)
    public BigDecimal getWalletBalanceAt(Long walletId, Instant timestamp) {
        log.info("Querying balance for wallet {} at {}", walletId, timestamp);
        return ledgerEntryRepository
                .findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(walletId, timestamp)
                .map(LedgerEntry::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Rebuild wallet state from all events (Full Replay)
     * This is used for reconciliation and to verify the current balance.
     */
    @Transactional(readOnly = true)
    public BigDecimal rebuildWalletState(Long walletId) {
        log.info("Rebuilding state for wallet {} from all events", walletId);
        List<LedgerEntry> entries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtAsc(walletId);

        BigDecimal balance = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            // In a more complex system, we would apply events to a state object
            // For now, we just sum them up or trust the balanceAfter of the last entry
            balance = entry.getBalanceAfter();
        }

        log.info("Rebuilt balance for wallet {}: {}", walletId, balance);
        return balance;
    }
}

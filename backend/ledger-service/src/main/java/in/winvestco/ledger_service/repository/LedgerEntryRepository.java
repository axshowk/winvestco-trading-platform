package in.winvestco.ledger_service.repository;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for LedgerEntry - READ and INSERT ONLY.
 * 
 * CRITICAL: This repository intentionally does NOT expose:
 * - deleteById() - wrapped to throw exception
 * - deleteAll() - wrapped to throw exception
 * - Any update methods
 * 
 * The ONLY write operation allowed is save() for new entries.
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    // ==============================================
    // QUERY OPERATIONS (READ-ONLY)
    // ==============================================

    /**
     * Get ledger entries for a wallet (paginated, newest first)
     */
    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Get all entries for a wallet (for reconciliation)
     */
    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    /**
     * Get all entries for a wallet (oldest first - chronological order)
     */
    List<LedgerEntry> findByWalletIdOrderByCreatedAtAsc(Long walletId);

    /**
     * Find entries by reference ID
     */
    List<LedgerEntry> findByReferenceId(String referenceId);

    /**
     * Find entry by reference ID and type
     */
    Optional<LedgerEntry> findByReferenceIdAndReferenceType(String referenceId, String referenceType);

    /**
     * Get entries by type for a wallet
     */
    Page<LedgerEntry> findByWalletIdAndEntryTypeOrderByCreatedAtDesc(
            Long walletId, LedgerEntryType entryType, Pageable pageable);

    /**
     * Get entries within a date range (for audit/reconciliation)
     */
    @Query("SELECT le FROM LedgerEntry le WHERE le.walletId = :walletId " +
            "AND le.createdAt BETWEEN :startDate AND :endDate ORDER BY le.createdAt ASC")
    List<LedgerEntry> findByWalletIdAndDateRange(
            @Param("walletId") Long walletId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Count entries for a wallet
     */
    long countByWalletId(Long walletId);

    /**
     * Get the latest entry for a wallet (for balance verification)
     */
    Optional<LedgerEntry> findFirstByWalletIdOrderByCreatedAtDesc(Long walletId);

    /**
     * Get the latest entry for a wallet before or at a specific time
     */
    Optional<LedgerEntry> findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
            Long walletId, Instant timestamp);

    /**
     * Sum of amounts by type for a wallet (for reconciliation)
     */
    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le " +
            "WHERE le.walletId = :walletId AND le.entryType = :entryType")
    java.math.BigDecimal sumAmountByWalletIdAndEntryType(
            @Param("walletId") Long walletId,
            @Param("entryType") LedgerEntryType entryType);

    /**
     * Get all entries in date range (for global audit)
     */
    @Query("SELECT le FROM LedgerEntry le " +
            "WHERE le.createdAt BETWEEN :startDate AND :endDate ORDER BY le.createdAt ASC")
    List<LedgerEntry> findAllByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // ==============================================
    // DELETE OPERATIONS - NOT ALLOWED
    // Override to throw UnsupportedOperationException
    // ==============================================

    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("DELETE not allowed on immutable ledger");
    }

    @Override
    default void delete(LedgerEntry entity) {
        throw new UnsupportedOperationException("DELETE not allowed on immutable ledger");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("DELETE not allowed on immutable ledger");
    }

    @Override
    default void deleteAll(Iterable<? extends LedgerEntry> entities) {
        throw new UnsupportedOperationException("DELETE not allowed on immutable ledger");
    }

    @Override
    default void deleteAllById(Iterable<? extends Long> ids) {
        throw new UnsupportedOperationException("DELETE not allowed on immutable ledger");
    }

    @Override
    default void deleteAllInBatch() {
        throw new UnsupportedOperationException("DELETE not allowed on immutable ledger");
    }

    @Override
    default void deleteAllByIdInBatch(Iterable<Long> ids) {
        throw new UnsupportedOperationException("DELETE not allowed on immutable ledger");
    }

    @Override
    default void deleteAllInBatch(Iterable<LedgerEntry> entities) {
        throw new UnsupportedOperationException("DELETE not allowed on immutable ledger");
    }
}

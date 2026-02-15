package in.winvestco.ledger_service.repository;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.model.LedgerEntry;
import in.winvestco.ledger_service.testdata.LedgerTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("LedgerEntryRepository Tests")
class LedgerEntryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LedgerEntryRepository repository;

    private List<LedgerEntry> testEntries;
    private final Pageable testPageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        // Create test data with different timestamps and types
        testEntries = List.of(
                createEntry(1L, LedgerEntryType.DEPOSIT, new BigDecimal("1000.00"), 
                        BigDecimal.ZERO, new BigDecimal("1000.00"), 
                        Instant.now().minusSeconds(7200), "REF-001"),
                createEntry(1L, LedgerEntryType.TRADE_BUY, new BigDecimal("200.00"), 
                        new BigDecimal("1000.00"), new BigDecimal("800.00"), 
                        Instant.now().minusSeconds(3600), "REF-002"),
                createEntry(1L, LedgerEntryType.TRADE_SELL, new BigDecimal("250.00"), 
                        new BigDecimal("800.00"), new BigDecimal("1050.00"), 
                        Instant.now().minusSeconds(1800), "REF-003"),
                createEntry(2L, LedgerEntryType.DEPOSIT, new BigDecimal("500.00"), 
                        BigDecimal.ZERO, new BigDecimal("500.00"), 
                        Instant.now().minusSeconds(900), "REF-004"),
                createEntry(1L, LedgerEntryType.WITHDRAWAL, new BigDecimal("100.00"), 
                        new BigDecimal("1050.00"), new BigDecimal("950.00"), 
                        Instant.now().minusSeconds(300), "REF-005")
        );

        testEntries.forEach(entityManager::persistAndFlush);
    }

    private LedgerEntry createEntry(Long walletId, LedgerEntryType entryType, BigDecimal amount,
                                   BigDecimal balanceBefore, BigDecimal balanceAfter, Instant createdAt, String referenceId) {
        return LedgerEntry.builder()
                .walletId(walletId)
                .entryType(entryType)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceId(referenceId)
                .referenceType("TEST")
                .description("Test entry")
                .createdAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("Should find entries by wallet ID ordered by creation date desc")
    void findByWalletIdOrderByCreatedAtDesc_ShouldReturnOrderedEntries() {
        // When
        Page<LedgerEntry> result = repository.findByWalletIdOrderByCreatedAtDesc(1L, testPageable);

        // Then
        assertEquals(4, result.getContent().size());
        assertEquals(4, result.getTotalElements());
        
        // Verify ordering (newest first)
        List<LedgerEntry> entries = result.getContent();
        assertTrue(entries.get(0).getCreatedAt().isAfter(entries.get(1).getCreatedAt()));
        assertTrue(entries.get(1).getCreatedAt().isAfter(entries.get(2).getCreatedAt()));
        assertTrue(entries.get(2).getCreatedAt().isAfter(entries.get(3).getCreatedAt()));
    }

    @Test
    @DisplayName("Should find all entries by wallet ID ordered by creation date asc")
    void findByWalletIdOrderByCreatedAtAsc_ShouldReturnChronologicalEntries() {
        // When
        List<LedgerEntry> result = repository.findByWalletIdOrderByCreatedAtAsc(1L);

        // Then
        assertEquals(4, result.size());
        
        // Verify ordering (oldest first)
        assertTrue(result.get(0).getCreatedAt().isBefore(result.get(1).getCreatedAt()));
        assertTrue(result.get(1).getCreatedAt().isBefore(result.get(2).getCreatedAt()));
        assertTrue(result.get(2).getCreatedAt().isBefore(result.get(3).getCreatedAt()));
    }

    @Test
    @DisplayName("Should find entries by reference ID")
    void findByReferenceId_ShouldReturnMatchingEntries() {
        // When
        List<LedgerEntry> result = repository.findByReferenceId("REF-003");

        // Then
        assertEquals(1, result.size());
        assertEquals("REF-003", result.get(0).getReferenceId());
        assertEquals(LedgerEntryType.TRADE_SELL, result.get(0).getEntryType());
    }

    @Test
    @DisplayName("Should find entry by reference ID and reference type")
    void findByReferenceIdAndReferenceType_ShouldReturnMatchingEntry() {
        // Given
        LedgerEntry entry = LedgerEntry.builder()
                .walletId(1L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("100.00"))
                .referenceId("REF-001")
                .referenceType("ORDER")
                .description("Test entry")
                .createdAt(Instant.now())
                .build();
        entityManager.persistAndFlush(entry);

        // When
        Optional<LedgerEntry> result = repository.findByReferenceIdAndReferenceType("REF-001", "ORDER");

        // Then
        assertTrue(result.isPresent());
        assertEquals("REF-001", result.get().getReferenceId());
        assertEquals("ORDER", result.get().getReferenceType());
    }

    @Test
    @DisplayName("Should find entries by wallet ID and entry type ordered by creation date desc")
    void findByWalletIdAndEntryTypeOrderByCreatedAtDesc_ShouldReturnFilteredOrderedEntries() {
        // When
        Page<LedgerEntry> result = repository.findByWalletIdAndEntryTypeOrderByCreatedAtDesc(
                1L, LedgerEntryType.DEPOSIT, testPageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(LedgerEntryType.DEPOSIT, result.getContent().get(0).getEntryType());
        assertEquals(1L, result.getContent().get(0).getWalletId());
    }

    @Test
    @DisplayName("Should find entries by wallet ID and date range")
    void findByWalletIdAndDateRange_ShouldReturnEntriesInDateRange() {
        // Given
        Instant startDate = Instant.now().minusSeconds(4000);
        Instant endDate = Instant.now().minusSeconds(1000);

        // When
        List<LedgerEntry> result = repository.findByWalletIdAndDateRange(1L, startDate, endDate);

        // Then
        assertEquals(2, result.size()); // Should include REF-002 and REF-003
        result.forEach(entry -> {
            assertTrue(entry.getCreatedAt().isAfter(startDate));
            assertTrue(entry.getCreatedAt().isBefore(endDate));
            assertEquals(1L, entry.getWalletId());
        });
    }

    @Test
    @DisplayName("Should count entries by wallet ID")
    void countByWalletId_ShouldReturnCorrectCount() {
        // When
        long count = repository.countByWalletId(1L);

        // Then
        assertEquals(4, count);
    }

    @Test
    @DisplayName("Should find first entry by wallet ID ordered by creation date desc")
    void findFirstByWalletIdOrderByCreatedAtDesc_ShouldReturnMostRecentEntry() {
        // When
        Optional<LedgerEntry> result = repository.findFirstByWalletIdOrderByCreatedAtDesc(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("REF-005", result.get().getReferenceId()); // Most recent entry
    }

    @Test
    @DisplayName("Should find first entry by wallet ID and timestamp")
    void findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc_ShouldReturnEntryBeforeTimestamp() {
        // Given
        Instant timestamp = Instant.now().minusSeconds(2000);

        // When
        Optional<LedgerEntry> result = repository.findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                1L, timestamp);

        // Then
        assertTrue(result.isPresent());
        assertEquals("REF-002", result.get().getReferenceId()); // Latest entry before timestamp
        assertTrue(result.get().getCreatedAt().isBefore(timestamp));
    }

    @Test
    @DisplayName("Should sum amounts by wallet ID and entry type")
    void sumAmountByWalletIdAndEntryType_ShouldReturnCorrectSum() {
        // When
        BigDecimal sum = repository.sumAmountByWalletIdAndEntryType(1L, LedgerEntryType.DEPOSIT);

        // Then
        assertEquals(new BigDecimal("1000.00"), sum);
    }

    @Test
    @DisplayName("Should find all entries in global date range")
    void findAllByDateRange_ShouldReturnAllEntriesInDateRange() {
        // Given
        Instant startDate = Instant.now().minusSeconds(4000);
        Instant endDate = Instant.now().minusSeconds(1000);

        // When
        List<LedgerEntry> result = repository.findAllByDateRange(startDate, endDate);

        // Then
        assertEquals(3, result.size()); // Should include entries from both wallets
        result.forEach(entry -> {
            assertTrue(entry.getCreatedAt().isAfter(startDate));
            assertTrue(entry.getCreatedAt().isBefore(endDate));
        });
    }

    @Test
    @DisplayName("Should return empty results for non-existent wallet")
    void queriesForNonExistentWallet_ShouldReturnEmptyResults() {
        // When & Then
        assertEquals(0, repository.findByWalletIdOrderByCreatedAtDesc(999L, testPageable).getTotalElements());
        assertEquals(0, repository.findByWalletIdOrderByCreatedAtAsc(999L).size());
        assertEquals(0, repository.findByWalletIdAndEntryTypeOrderByCreatedAtDesc(
                999L, LedgerEntryType.DEPOSIT, testPageable).getTotalElements());
        assertEquals(0, repository.countByWalletId(999L));
        assertTrue(repository.findFirstByWalletIdOrderByCreatedAtDesc(999L).isEmpty());
        assertEquals(BigDecimal.ZERO, repository.sumAmountByWalletIdAndEntryType(
                999L, LedgerEntryType.DEPOSIT));
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for delete operations")
    void deleteOperations_ShouldThrowUnsupportedOperationException() {
        // Given
        LedgerEntry entry = testEntries.get(0);

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> repository.deleteById(entry.getId()));
        assertThrows(UnsupportedOperationException.class, () -> repository.delete(entry));
        assertThrows(UnsupportedOperationException.class, () -> repository.deleteAll());
        assertThrows(UnsupportedOperationException.class, () -> repository.deleteAll(List.of(entry)));
        assertThrows(UnsupportedOperationException.class, () -> repository.deleteAllById(List.of(entry.getId())));
        assertThrows(UnsupportedOperationException.class, () -> repository.deleteAllInBatch());
        assertThrows(UnsupportedOperationException.class, () -> repository.deleteAllInBatch(List.of(entry)));
        assertThrows(UnsupportedOperationException.class, () -> repository.deleteAllByIdInBatch(List.of(entry.getId())));
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void pagination_ShouldWorkCorrectly() {
        // Given
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        // When
        Page<LedgerEntry> first = repository.findByWalletIdOrderByCreatedAtDesc(1L, firstPage);
        Page<LedgerEntry> second = repository.findByWalletIdOrderByCreatedAtDesc(1L, secondPage);

        // Then
        assertEquals(2, first.getContent().size());
        assertEquals(2, second.getContent().size());
        assertEquals(4, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        
        // Verify no overlap between pages
        List<Long> firstIds = first.getContent().stream().map(LedgerEntry::getId).toList();
        List<Long> secondIds = second.getContent().stream().map(LedgerEntry::getId).toList();
        assertTrue(firstIds.stream().noneMatch(secondIds::contains));
    }

    @Test
    @DisplayName("Should handle edge cases with timestamps")
    void edgeCasesWithTimestamps_ShouldWorkCorrectly() {
        // Given
        Instant exactlyOnTimestamp = testEntries.get(2).getCreatedAt(); // REF-003 timestamp

        // When
        Optional<LedgerEntry> result = repository.findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                1L, exactlyOnTimestamp);

        // Then
        assertTrue(result.isPresent());
        assertEquals("REF-003", result.get().getReferenceId());
    }
}

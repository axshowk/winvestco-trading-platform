package in.winvestco.ledger_service.service;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.dto.CreateLedgerEntryRequest;
import in.winvestco.ledger_service.dto.LedgerEntryDTO;
import in.winvestco.ledger_service.mapper.LedgerMapper;
import in.winvestco.ledger_service.messaging.LedgerEventPublisher;
import in.winvestco.ledger_service.model.LedgerEntry;
import in.winvestco.ledger_service.repository.LedgerEntryRepository;
import in.winvestco.ledger_service.testdata.LedgerTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerService Tests")
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private LedgerMapper ledgerMapper;

    @Mock
    private LedgerEventPublisher ledgerEventPublisher;

    @InjectMocks
    private LedgerService ledgerService;

    private LedgerEntry testEntry;
    private CreateLedgerEntryRequest testRequest;
    private LedgerEntryDTO testDTO;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        testEntry = LedgerTestDataFactory.createTestEntry();
        testRequest = LedgerTestDataFactory.createTestRequest();
        testDTO = LedgerTestDataFactory.createTestDTO();
        testPageable = Pageable.ofSize(10);
    }

    // ==============================================
    // RECORD ENTRY TESTS
    // ==============================================

    @Test
    @DisplayName("Should record entry and publish event")
    void recordEntry_ShouldSaveAndPublishEvent() {
        // Given
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(testEntry);
        when(ledgerMapper.toDTO(any(LedgerEntry.class))).thenReturn(testDTO);

        // When
        LedgerEntryDTO result = ledgerService.recordEntry(testRequest);

        // Then
        assertNotNull(result);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(ledgerEventPublisher).publishLedgerEntryRecorded(any(LedgerEntry.class));
        verify(ledgerMapper).toDTO(testEntry);
    }

    @ParameterizedTest
    @EnumSource(LedgerEntryType.class)
    @DisplayName("Should record entry for all transaction types")
    void recordEntry_ShouldWorkForAllEntryTypes(LedgerEntryType entryType) {
        // Given
        CreateLedgerEntryRequest request = LedgerTestDataFactory.createTestRequest(1L, entryType,
                new BigDecimal("500.00"), new BigDecimal("1000.00"), new BigDecimal("1500.00"));
        LedgerEntry entry = LedgerTestDataFactory.createTestEntry(1L, entryType,
                new BigDecimal("500.00"), new BigDecimal("1000.00"), new BigDecimal("1500.00"));
        
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(entry);
        when(ledgerMapper.toDTO(any(LedgerEntry.class))).thenReturn(testDTO);

        // When
        LedgerEntryDTO result = ledgerService.recordEntry(request);

        // Then
        assertNotNull(result);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(ledgerEventPublisher).publishLedgerEntryRecorded(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("Should handle repository exception during entry recording")
    void recordEntry_ShouldHandleRepositoryException() {
        // Given
        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> ledgerService.recordEntry(testRequest));
        verify(ledgerEventPublisher, never()).publishLedgerEntryRecorded(any());
    }

    @Test
    @DisplayName("Should handle event publishing exception")
    void recordEntry_ShouldHandleEventPublishingException() {
        // Given
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(testEntry);
        when(ledgerMapper.toDTO(any(LedgerEntry.class))).thenReturn(testDTO);
        doThrow(new RuntimeException("Event publishing failed"))
                .when(ledgerEventPublisher).publishLedgerEntryRecorded(any(LedgerEntry.class));

        // When & Then
        // Should still return result even if event publishing fails
        assertDoesNotThrow(() -> ledgerService.recordEntry(testRequest));
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
    }

    // ==============================================
    // QUERY TESTS
    // ==============================================

    @Test
    @DisplayName("Should get entry by ID")
    void getEntryById_ShouldReturnEntry() {
        // Given
        when(ledgerEntryRepository.findById(1L)).thenReturn(Optional.of(testEntry));
        when(ledgerMapper.toDTO(testEntry)).thenReturn(testDTO);

        // When
        Optional<LedgerEntryDTO> result = ledgerService.getEntryById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testDTO, result.get());
        verify(ledgerEntryRepository).findById(1L);
        verify(ledgerMapper).toDTO(testEntry);
    }

    @Test
    @DisplayName("Should return empty when entry not found by ID")
    void getEntryById_ShouldReturnEmptyWhenNotFound() {
        // Given
        when(ledgerEntryRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<LedgerEntryDTO> result = ledgerService.getEntryById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(ledgerEntryRepository).findById(999L);
        verify(ledgerMapper, never()).toDTO(any());
    }

    @Test
    @DisplayName("Should get paginated entries for wallet")
    void getEntriesForWallet_ShouldReturnPaginatedResults() {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 5);
        Page<LedgerEntry> entryPage = new PageImpl<>(entries, testPageable, entries.size());
        List<LedgerEntryDTO> dtoList = entries.stream()
                .map(entry -> LedgerTestDataFactory.createTestDTO())
                .toList();

        when(ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(1L, testPageable))
                .thenReturn(entryPage);
        when(ledgerMapper.toDTOList(entries)).thenReturn(dtoList);

        // When
        Page<LedgerEntryDTO> result = ledgerService.getEntriesForWallet(1L, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        verify(ledgerEntryRepository).findByWalletIdOrderByCreatedAtDesc(1L, testPageable);
        verify(ledgerMapper).toDTOList(entries);
    }

    @Test
    @DisplayName("Should get all entries for wallet")
    void getAllEntriesForWallet_ShouldReturnAllEntries() {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 3);
        List<LedgerEntryDTO> dtoList = entries.stream()
                .map(entry -> LedgerTestDataFactory.createTestDTO())
                .toList();

        when(ledgerEntryRepository.findByWalletIdOrderByCreatedAtAsc(1L)).thenReturn(entries);
        when(ledgerMapper.toDTOList(entries)).thenReturn(dtoList);

        // When
        List<LedgerEntryDTO> result = ledgerService.getAllEntriesForWallet(1L);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(ledgerEntryRepository).findByWalletIdOrderByCreatedAtAsc(1L);
        verify(ledgerMapper).toDTOList(entries);
    }

    @Test
    @DisplayName("Should get entries by reference ID")
    void getEntriesByReference_ShouldReturnMatchingEntries() {
        // Given
        String referenceId = "REF-12345";
        List<LedgerEntry> entries = List.of(testEntry);
        List<LedgerEntryDTO> dtoList = List.of(testDTO);

        when(ledgerEntryRepository.findByReferenceId(referenceId)).thenReturn(entries);
        when(ledgerMapper.toDTOList(entries)).thenReturn(dtoList);

        // When
        List<LedgerEntryDTO> result = ledgerService.getEntriesByReference(referenceId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ledgerEntryRepository).findByReferenceId(referenceId);
        verify(ledgerMapper).toDTOList(entries);
    }

    @Test
    @DisplayName("Should get entries by type for wallet")
    void getEntriesByType_ShouldReturnFilteredEntries() {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 3, LedgerEntryType.DEPOSIT);
        Page<LedgerEntry> entryPage = new PageImpl<>(entries, testPageable, entries.size());
        List<LedgerEntryDTO> dtoList = entries.stream()
                .map(entry -> LedgerTestDataFactory.createTestDTO())
                .toList();

        when(ledgerEntryRepository.findByWalletIdAndEntryTypeOrderByCreatedAtDesc(
                1L, LedgerEntryType.DEPOSIT, testPageable))
                .thenReturn(entryPage);
        when(ledgerMapper.toDTOList(entries)).thenReturn(dtoList);

        // When
        Page<LedgerEntryDTO> result = ledgerService.getEntriesByType(1L, LedgerEntryType.DEPOSIT, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        verify(ledgerEntryRepository).findByWalletIdAndEntryTypeOrderByCreatedAtDesc(
                1L, LedgerEntryType.DEPOSIT, testPageable);
        verify(ledgerMapper).toDTOList(entries);
    }

    @Test
    @DisplayName("Should get latest entry for wallet")
    void getLatestEntryForWallet_ShouldReturnMostRecentEntry() {
        // Given
        when(ledgerEntryRepository.findFirstByWalletIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(testEntry));
        when(ledgerMapper.toDTO(testEntry)).thenReturn(testDTO);

        // When
        Optional<LedgerEntryDTO> result = ledgerService.getLatestEntryForWallet(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testDTO, result.get());
        verify(ledgerEntryRepository).findFirstByWalletIdOrderByCreatedAtDesc(1L);
        verify(ledgerMapper).toDTO(testEntry);
    }

    @Test
    @DisplayName("Should return empty when no entries exist for wallet")
    void getLatestEntryForWallet_ShouldReturnEmptyWhenNoEntries() {
        // Given
        when(ledgerEntryRepository.findFirstByWalletIdOrderByCreatedAtDesc(999L))
                .thenReturn(Optional.empty());

        // When
        Optional<LedgerEntryDTO> result = ledgerService.getLatestEntryForWallet(999L);

        // Then
        assertFalse(result.isPresent());
        verify(ledgerEntryRepository).findFirstByWalletIdOrderByCreatedAtDesc(999L);
        verify(ledgerMapper, never()).toDTO(any());
    }

    // ==============================================
    // BALANCE AND RECONCILIATION TESTS
    // ==============================================

    @Test
    @DisplayName("Should get wallet balance at specific timestamp")
    void getWalletBalanceAt_ShouldReturnBalanceFromSnapshot() {
        // Given
        Instant timestamp = Instant.now();
        when(ledgerEntryRepository.findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                1L, timestamp))
                .thenReturn(Optional.of(testEntry));

        // When
        BigDecimal balance = ledgerService.getWalletBalanceAt(1L, timestamp);

        // Then
        assertEquals(new BigDecimal("1000.00"), balance);
        verify(ledgerEntryRepository).findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                1L, timestamp);
    }

    @Test
    @DisplayName("Should return zero balance when no entries exist before timestamp")
    void getWalletBalanceAt_ShouldReturnZeroWhenNoEntries() {
        // Given
        Instant timestamp = Instant.now();
        when(ledgerEntryRepository.findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                1L, timestamp))
                .thenReturn(Optional.empty());

        // When
        BigDecimal balance = ledgerService.getWalletBalanceAt(1L, timestamp);

        // Then
        assertEquals(BigDecimal.ZERO, balance);
        verify(ledgerEntryRepository).findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                1L, timestamp);
    }

    @Test
    @DisplayName("Should rebuild wallet state from all entries")
    void rebuildWalletState_ShouldCalculateBalanceFromEntries() {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createMixedTransactionHistory(1L);
        when(ledgerEntryRepository.findByWalletIdOrderByCreatedAtAsc(1L)).thenReturn(entries);

        // When
        BigDecimal balance = ledgerService.rebuildWalletState(1L);

        // Then
        // Expected balance: 10000 (deposit) - 1000 (buy) + 1100 (sell) - 500 (withdrawal) - 10 (fee) = 9590
        assertEquals(new BigDecimal("9590.00"), balance);
        verify(ledgerEntryRepository).findByWalletIdOrderByCreatedAtAsc(1L);
    }

    @Test
    @DisplayName("Should return zero balance when rebuilding empty wallet")
    void rebuildWalletState_ShouldReturnZeroForEmptyWallet() {
        // Given
        when(ledgerEntryRepository.findByWalletIdOrderByCreatedAtAsc(999L)).thenReturn(List.of());

        // When
        BigDecimal balance = ledgerService.rebuildWalletState(999L);

        // Then
        assertEquals(BigDecimal.ZERO, balance);
        verify(ledgerEntryRepository).findByWalletIdOrderByCreatedAtAsc(999L);
    }

    @Test
    @DisplayName("Should sum amounts by type for reconciliation")
    void sumAmountsByType_ShouldReturnCorrectSum() {
        // Given
        List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(1L, 3, LedgerEntryType.DEPOSIT);
        when(ledgerEntryRepository.sumAmountByWalletIdAndEntryType(1L, LedgerEntryType.DEPOSIT))
                .thenReturn(new BigDecimal("300.00"));

        // When
        BigDecimal sum = ledgerService.sumAmountsByType(1L, LedgerEntryType.DEPOSIT);

        // Then
        assertEquals(new BigDecimal("300.00"), sum);
        verify(ledgerEntryRepository).sumAmountByWalletIdAndEntryType(1L, LedgerEntryType.DEPOSIT);
    }

    @Test
    @DisplayName("Should count entries for wallet")
    void countEntriesForWallet_ShouldReturnCorrectCount() {
        // Given
        when(ledgerEntryRepository.countByWalletId(1L)).thenReturn(5L);

        // When
        long count = ledgerService.countEntriesForWallet(1L);

        // Then
        assertEquals(5L, count);
        verify(ledgerEntryRepository).countByWalletId(1L);
    }

    @Test
    @DisplayName("Should get entries in date range")
    void getEntriesInDateRange_ShouldReturnFilteredEntries() {
        // Given
        Instant startDate = Instant.now().minusSeconds(3600); // 1 hour ago
        Instant endDate = Instant.now();
        List<LedgerEntry> entries = List.of(testEntry);
        List<LedgerEntryDTO> dtoList = List.of(testDTO);

        when(ledgerEntryRepository.findByWalletIdAndDateRange(1L, startDate, endDate))
                .thenReturn(entries);
        when(ledgerMapper.toDTOList(entries)).thenReturn(dtoList);

        // When
        List<LedgerEntryDTO> result = ledgerService.getEntriesInDateRange(1L, startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ledgerEntryRepository).findByWalletIdAndDateRange(1L, startDate, endDate);
        verify(ledgerMapper).toDTOList(entries);
    }

    @Test
    @DisplayName("Should get all entries in global date range")
    void getAllEntriesInDateRange_ShouldReturnAllMatchingEntries() {
        // Given
        Instant startDate = Instant.now().minusSeconds(7200); // 2 hours ago
        Instant endDate = Instant.now();
        List<LedgerEntry> entries = List.of(testEntry);
        List<LedgerEntryDTO> dtoList = List.of(testDTO);

        when(ledgerEntryRepository.findAllByDateRange(startDate, endDate)).thenReturn(entries);
        when(ledgerMapper.toDTOList(entries)).thenReturn(dtoList);

        // When
        List<LedgerEntryDTO> result = ledgerService.getAllEntriesInDateRange(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ledgerEntryRepository).findAllByDateRange(startDate, endDate);
        verify(ledgerMapper).toDTOList(entries);
    }
}

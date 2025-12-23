package in.winvestco.ledger_service.service;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.dto.CreateLedgerEntryRequest;
import in.winvestco.ledger_service.dto.LedgerEntryDTO;
import in.winvestco.ledger_service.mapper.LedgerMapper;
import in.winvestco.ledger_service.messaging.LedgerEventPublisher;
import in.winvestco.ledger_service.model.LedgerEntry;
import in.winvestco.ledger_service.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    void setUp() {
        testEntry = LedgerEntry.builder()
                .id(1L)
                .walletId(1L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("1000"))
                .balanceBefore(new BigDecimal("0"))
                .balanceAfter(new BigDecimal("1000"))
                .referenceId("REF-1")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void recordEntry_ShouldSaveAndPublishEvent() {
        CreateLedgerEntryRequest request = CreateLedgerEntryRequest.builder()
                .walletId(1L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("1000"))
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("1000"))
                .build();

        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenReturn(testEntry);
        when(ledgerMapper.toDTO(any(LedgerEntry.class))).thenReturn(new LedgerEntryDTO());

        LedgerEntryDTO result = ledgerService.recordEntry(request);

        assertNotNull(result);
        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(ledgerEventPublisher).publishLedgerEntryRecorded(any(LedgerEntry.class));
    }

    @Test
    void getWalletBalanceAt_ShouldReturnBalanceFromSnapshot() {
        Instant timestamp = Instant.now();
        when(ledgerEntryRepository.findFirstByWalletIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(anyLong(),
                any(Instant.class)))
                .thenReturn(Optional.of(testEntry));

        BigDecimal balance = ledgerService.getWalletBalanceAt(1L, timestamp);

        assertEquals(new BigDecimal("1000"), balance);
    }

    @Test
    void rebuildWalletState_ShouldCalculateBalanceFromEntries() {
        when(ledgerEntryRepository.findByWalletIdOrderByCreatedAtAsc(anyLong()))
                .thenReturn(java.util.List.of(testEntry));

        BigDecimal balance = ledgerService.rebuildWalletState(1L);

        assertEquals(new BigDecimal("1000"), balance);
    }
}

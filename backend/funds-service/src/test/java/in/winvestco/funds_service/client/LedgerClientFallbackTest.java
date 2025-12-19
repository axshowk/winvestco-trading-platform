package in.winvestco.funds_service.client;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.funds_service.dto.CreateLedgerEntryRequest;
import in.winvestco.funds_service.dto.LedgerEntryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LedgerClientFallback.
 * Validates fallback behavior for financial operations when ledger-service is
 * unavailable.
 */
class LedgerClientFallbackTest {

    private LedgerClientFallback fallback;
    private CreateLedgerEntryRequest testRequest;

    @BeforeEach
    void setUp() {
        fallback = new LedgerClientFallback();
        testRequest = new CreateLedgerEntryRequest();
        testRequest.setWalletId(1L);
        testRequest.setAmount(new BigDecimal("100.00"));
        testRequest.setEntryType(LedgerEntryType.DEPOSIT);
    }

    @Test
    @DisplayName("recordEntry with idempotency key should throw LedgerServiceUnavailableException")
    void recordEntryWithIdempotencyKeyShouldThrowException() {
        String idempotencyKey = "wallet1:DEPOSIT:txn123";

        assertThatThrownBy(() -> fallback.recordEntry(idempotencyKey, testRequest))
                .isInstanceOf(LedgerClientFallback.LedgerServiceUnavailableException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    @DisplayName("recordEntry without idempotency key should throw LedgerServiceUnavailableException")
    void recordEntryShouldThrowException() {
        assertThatThrownBy(() -> fallback.recordEntry(testRequest))
                .isInstanceOf(LedgerClientFallback.LedgerServiceUnavailableException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    @DisplayName("getEntry should return null for read operations")
    void getEntryShouldReturnNull() {
        LedgerEntryDTO result = fallback.getEntry(1L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getWalletEntries should return empty page")
    void getWalletEntriesShouldReturnEmptyPage() {
        Page<LedgerEntryDTO> result = fallback.getWalletEntries(1L, 0, 50);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getAllWalletEntries should return empty list")
    void getAllWalletEntriesShouldReturnEmptyList() {
        List<LedgerEntryDTO> result = fallback.getAllWalletEntries(1L);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getLatestEntry should return null")
    void getLatestEntryShouldReturnNull() {
        LedgerEntryDTO result = fallback.getLatestEntry(1L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getEntriesByReference should return empty list")
    void getEntriesByReferenceShouldReturnEmptyList() {
        List<LedgerEntryDTO> result = fallback.getEntriesByReference("REF123");

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Write operations should fail fast - financial integrity")
    void writeOperationsShouldFailFast() {
        // Critical: Financial write operations should never silently fail
        // They MUST throw an exception so the caller knows the operation didn't
        // complete

        assertThatCode(() -> fallback.getEntry(1L)).doesNotThrowAnyException();
        assertThatCode(() -> fallback.getWalletEntries(1L, 0, 50)).doesNotThrowAnyException();
        assertThatCode(() -> fallback.getAllWalletEntries(1L)).doesNotThrowAnyException();

        // But write operations MUST throw
        assertThatThrownBy(() -> fallback.recordEntry(testRequest))
                .isInstanceOf(LedgerClientFallback.LedgerServiceUnavailableException.class);
    }
}

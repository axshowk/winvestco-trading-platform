package in.winvestco.funds_service.service;

import in.winvestco.common.enums.WalletStatus;
import in.winvestco.funds_service.client.LedgerClient;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.mapper.FundsMapper;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerClient ledgerClient;

    @Mock
    private FundsMapper fundsMapper;

    @Mock
    private FundsEventPublisher fundsEventPublisher;

    @InjectMocks
    private WalletService walletService;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .availableBalance(new BigDecimal("1000"))
                .lockedBalance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();
    }

    @Test
    void creditFunds_ShouldIncreaseBalanceAndRecordToLedger() {
        when(walletRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        walletService.creditFunds(1L, new BigDecimal("500"), "DEP-123", "DEPOSIT", "Test credit");

        assertEquals(new BigDecimal("1500"), testWallet.getAvailableBalance());
        verify(ledgerClient).recordEntry(any());
        verify(fundsEventPublisher).publishFundsDeposited(eq(1L), eq(testWallet), eq(new BigDecimal("500")), any(),
                eq("DEP-123"), eq("DEPOSIT"));
    }

    @Test
    void debitFunds_WithSufficientBalance_ShouldDecreaseBalance() {
        when(walletRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        walletService.debitFunds(1L, new BigDecimal("400"), "WD-123", "WITHDRAWAL", "Test debit");

        assertEquals(new BigDecimal("600"), testWallet.getAvailableBalance());
        verify(ledgerClient).recordEntry(any());
        verify(fundsEventPublisher).publishFundsWithdrawn(eq(1L), eq(testWallet), eq(new BigDecimal("400")), any(),
                eq("WD-123"), eq("WITHDRAWAL"), any());
    }

    @Test
    void debitFunds_WithInsufficientBalance_ShouldThrowException() {
        when(walletRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(testWallet));

        assertThrows(InsufficientFundsException.class,
                () -> walletService.debitFunds(1L, new BigDecimal("1500"), "WD-123", "WITHDRAWAL", "Test"));
    }

    @Test
    void createWalletForUser_WhenNotExists_ShouldCreateNewWallet() {
        when(walletRepository.existsByUserId(anyLong())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        Wallet result = walletService.createWalletForUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(walletRepository).save(any(Wallet.class));
    }
}

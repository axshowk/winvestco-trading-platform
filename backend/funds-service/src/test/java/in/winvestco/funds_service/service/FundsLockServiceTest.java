package in.winvestco.funds_service.service;

import in.winvestco.common.enums.LockStatus;
import in.winvestco.funds_service.client.LedgerClient;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.exception.DuplicateLockException;
import in.winvestco.funds_service.exception.FundsLockNotFoundException;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.mapper.FundsMapper;
import in.winvestco.funds_service.model.FundsLock;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.repository.FundsLockRepository;
import in.winvestco.funds_service.repository.WalletRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FundsLockService.
 * Tests funds locking, release, and settlement operations.
 */
@ExtendWith(MockitoExtension.class)
class FundsLockServiceTest {

    @Mock
    private FundsLockRepository fundsLockRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerClient ledgerClient;

    @Mock
    private FundsMapper fundsMapper;

    @Mock
    private FundsEventPublisher fundsEventPublisher;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Timer timer;

    @InjectMocks
    private FundsLockService fundsLockService;

    private Wallet testWallet;
    private FundsLock testLock;
    private FundsLockDTO testLockDTO;

    @BeforeEach
    void setUp() {
        testWallet = Wallet.builder()
                .id(1L)
                .userId(100L)
                .availableBalance(new BigDecimal("10000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .currency("INR")
                .build();

        testLock = FundsLock.builder()
                .id(1L)
                .walletId(1L)
                .orderId("ORD-123")
                .amount(new BigDecimal("1000.00"))
                .status(LockStatus.LOCKED)
                .reason("Order placed")
                .createdAt(Instant.now())
                .build();

        testLockDTO = new FundsLockDTO();
        testLockDTO.setId(1L);
        testLockDTO.setOrderId("ORD-123");
        testLockDTO.setAmount(new BigDecimal("1000.00"));
        testLockDTO.setStatus(LockStatus.LOCKED);
    }

    @Nested
    @DisplayName("lockFunds tests")
    class LockFundsTests {

        @Test
        @DisplayName("Should lock funds successfully when sufficient balance")
        void lockFunds_WithSufficientBalance_ShouldSucceed() {
            // Arrange
            when(fundsLockRepository.existsByOrderId("ORD-123")).thenReturn(false);
            when(walletRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(fundsLockRepository.save(any(FundsLock.class))).thenReturn(testLock);
            when(fundsMapper.toFundsLockDTO(any(FundsLock.class))).thenReturn(testLockDTO);

            // Act
            FundsLockDTO result = fundsLockService.lockFunds(100L, "ORD-123", new BigDecimal("1000.00"), "Test lock");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo("ORD-123");
            verify(ledgerClient).recordEntry(any());
            verify(fundsLockRepository).save(any(FundsLock.class));
        }

        @Test
        @DisplayName("Should throw DuplicateLockException for duplicate order")
        void lockFunds_WithDuplicateOrder_ShouldThrowException() {
            // Arrange
            when(fundsLockRepository.existsByOrderId("ORD-123")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> fundsLockService.lockFunds(100L, "ORD-123", new BigDecimal("1000.00"), "Test"))
                    .isInstanceOf(DuplicateLockException.class);

            verify(walletRepository, never()).findByUserIdForUpdate(anyLong());
        }

        @Test
        @DisplayName("Should throw InsufficientFundsException when balance is insufficient")
        void lockFunds_WithInsufficientBalance_ShouldThrowException() {
            // Arrange
            testWallet.setAvailableBalance(new BigDecimal("500.00"));
            when(fundsLockRepository.existsByOrderId("ORD-123")).thenReturn(false);
            when(walletRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.of(testWallet));

            // Act & Assert
            assertThatThrownBy(() -> fundsLockService.lockFunds(100L, "ORD-123", new BigDecimal("1000.00"), "Test"))
                    .isInstanceOf(InsufficientFundsException.class);

            verify(fundsLockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw RuntimeException when wallet not found")
        void lockFunds_WalletNotFound_ShouldThrowException() {
            // Arrange
            when(fundsLockRepository.existsByOrderId("ORD-123")).thenReturn(false);
            when(walletRepository.findByUserIdForUpdate(100L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> fundsLockService.lockFunds(100L, "ORD-123", new BigDecimal("1000.00"), "Test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Wallet not found");
        }
    }

    @Nested
    @DisplayName("releaseFunds tests")
    class ReleaseFundsTests {

        @BeforeEach
        void setUpMocks() {
            // Setup timer mock for metrics recording
            when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(timer);
        }

        @Test
        @DisplayName("Should release locked funds successfully")
        void releaseFunds_WithLockedFunds_ShouldSucceed() {
            // Arrange
            testWallet.setLockedBalance(new BigDecimal("1000.00"));
            when(fundsLockRepository.findByOrderId("ORD-123")).thenReturn(Optional.of(testLock));
            when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(fundsLockRepository.save(any(FundsLock.class))).thenReturn(testLock);
            when(fundsMapper.toFundsLockDTO(any(FundsLock.class))).thenReturn(testLockDTO);

            // Act
            FundsLockDTO result = fundsLockService.releaseFunds("ORD-123", "Order cancelled");

            // Assert
            assertThat(result).isNotNull();
            verify(ledgerClient).recordEntry(any());
            verify(fundsEventPublisher).publishFundsReleased(eq(100L), eq(testWallet), eq(testLock), anyString());
        }

        @Test
        @DisplayName("Should throw FundsLockNotFoundException when lock not found")
        void releaseFunds_LockNotFound_ShouldThrowException() {
            // Arrange
            when(fundsLockRepository.findByOrderId("ORD-999")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> fundsLockService.releaseFunds("ORD-999", "Reason"))
                    .isInstanceOf(FundsLockNotFoundException.class);
        }

        @Test
        @DisplayName("Should return existing lock when not in LOCKED status")
        void releaseFunds_NotLockedStatus_ShouldReturnWithoutChange() {
            // Arrange
            testLock.setStatus(LockStatus.RELEASED);
            when(fundsLockRepository.findByOrderId("ORD-123")).thenReturn(Optional.of(testLock));
            when(fundsMapper.toFundsLockDTO(any(FundsLock.class))).thenReturn(testLockDTO);

            // Act
            FundsLockDTO result = fundsLockService.releaseFunds("ORD-123", "Reason");

            // Assert
            assertThat(result).isNotNull();
            verify(walletRepository, never()).findByIdForUpdate(anyLong());
            verify(ledgerClient, never()).recordEntry(any());
        }
    }

    @Nested
    @DisplayName("settleFunds tests")
    class SettleFundsTests {

        @BeforeEach
        void setUpMocks() {
            when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(timer);
        }

        @Test
        @DisplayName("Should settle locked funds on trade execution")
        void settleFunds_WithLockedFunds_ShouldSucceed() {
            // Arrange
            testWallet.setLockedBalance(new BigDecimal("1000.00"));
            when(fundsLockRepository.findByOrderId("ORD-123")).thenReturn(Optional.of(testLock));
            when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
            when(fundsLockRepository.save(any(FundsLock.class))).thenReturn(testLock);
            when(fundsMapper.toFundsLockDTO(any(FundsLock.class))).thenReturn(testLockDTO);

            // Act
            FundsLockDTO result = fundsLockService.settleFunds("ORD-123", "Trade executed");

            // Assert
            assertThat(result).isNotNull();
            verify(ledgerClient).recordEntry(any());
        }

        @Test
        @DisplayName("Should throw FundsLockNotFoundException for unknown order")
        void settleFunds_LockNotFound_ShouldThrowException() {
            // Arrange
            when(fundsLockRepository.findByOrderId("ORD-999")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> fundsLockService.settleFunds("ORD-999", "Reason"))
                    .isInstanceOf(FundsLockNotFoundException.class);
        }

        @Test
        @DisplayName("Should return existing lock when not in LOCKED status")
        void settleFunds_NotLockedStatus_ShouldReturnWithoutChange() {
            // Arrange
            testLock.setStatus(LockStatus.SETTLED);
            when(fundsLockRepository.findByOrderId("ORD-123")).thenReturn(Optional.of(testLock));
            when(fundsMapper.toFundsLockDTO(any(FundsLock.class))).thenReturn(testLockDTO);

            // Act
            FundsLockDTO result = fundsLockService.settleFunds("ORD-123", "Reason");

            // Assert
            assertThat(result).isNotNull();
            verify(walletRepository, never()).findByIdForUpdate(anyLong());
        }
    }

    @Nested
    @DisplayName("Query operations tests")
    class QueryTests {

        @Test
        @DisplayName("Should get lock by order ID")
        void getLockByOrderId_Exists_ShouldReturnLock() {
            // Arrange
            when(fundsLockRepository.findByOrderId("ORD-123")).thenReturn(Optional.of(testLock));
            when(fundsMapper.toFundsLockDTO(testLock)).thenReturn(testLockDTO);

            // Act
            FundsLockDTO result = fundsLockService.getLockByOrderId("ORD-123");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo("ORD-123");
        }

        @Test
        @DisplayName("Should throw exception when lock not found")
        void getLockByOrderId_NotExists_ShouldThrowException() {
            // Arrange
            when(fundsLockRepository.findByOrderId("ORD-999")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> fundsLockService.getLockByOrderId("ORD-999"))
                    .isInstanceOf(FundsLockNotFoundException.class);
        }

        @Test
        @DisplayName("Should get active locks for wallet")
        void getActiveLocksForWallet_ShouldReturnList() {
            // Arrange
            List<FundsLock> locks = List.of(testLock);
            when(fundsLockRepository.findByWalletIdAndStatus(1L, LockStatus.LOCKED)).thenReturn(locks);
            when(fundsMapper.toFundsLockDTOList(locks)).thenReturn(List.of(testLockDTO));

            // Act
            List<FundsLockDTO> result = fundsLockService.getActiveLocksForWallet(1L);

            // Assert
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when no active locks")
        void getActiveLocksForWallet_NoLocks_ShouldReturnEmptyList() {
            // Arrange
            when(fundsLockRepository.findByWalletIdAndStatus(1L, LockStatus.LOCKED))
                    .thenReturn(Collections.emptyList());
            when(fundsMapper.toFundsLockDTOList(Collections.emptyList())).thenReturn(Collections.emptyList());

            // Act
            List<FundsLockDTO> result = fundsLockService.getActiveLocksForWallet(1L);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get all locks for wallet")
        void getAllLocksForWallet_ShouldReturnAllLocks() {
            // Arrange
            List<FundsLock> locks = List.of(testLock);
            when(fundsLockRepository.findByWalletIdOrderByCreatedAtDesc(1L)).thenReturn(locks);
            when(fundsMapper.toFundsLockDTOList(locks)).thenReturn(List.of(testLockDTO));

            // Act
            List<FundsLockDTO> result = fundsLockService.getAllLocksForWallet(1L);

            // Assert
            assertThat(result).hasSize(1);
        }
    }
}

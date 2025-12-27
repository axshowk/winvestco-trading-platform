package in.winvestco.funds_service.service;

import in.winvestco.common.enums.TransactionStatus;
import in.winvestco.common.enums.TransactionType;
import in.winvestco.common.enums.WalletStatus;
import in.winvestco.funds_service.dto.DepositRequest;
import in.winvestco.funds_service.dto.TransactionDTO;
import in.winvestco.funds_service.dto.WithdrawRequest;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.exception.TransactionNotFoundException;
import in.winvestco.funds_service.mapper.FundsMapper;
import in.winvestco.funds_service.model.Transaction;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService.
 * Tests deposit and withdrawal workflows.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private FundsMapper fundsMapper;

    @InjectMocks
    private TransactionService transactionService;

    private Wallet testWallet;
    private Transaction testDeposit;
    private Transaction testWithdrawal;
    private TransactionDTO testDepositDTO;
    private TransactionDTO testWithdrawalDTO;

    @BeforeEach
    void setUp() {
        testWallet = Wallet.builder()
                .id(1L)
                .userId(100L)
                .availableBalance(new BigDecimal("10000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();

        testDeposit = Transaction.builder()
                .id(1L)
                .walletId(1L)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("5000.00"))
                .status(TransactionStatus.PENDING)
                .externalReference("DEP-ABC123")
                .description("Test deposit")
                .createdAt(Instant.now())
                .build();

        testWithdrawal = Transaction.builder()
                .id(2L)
                .walletId(1L)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("2000.00"))
                .status(TransactionStatus.PENDING)
                .externalReference("WDR-XYZ789")
                .description("Test withdrawal")
                .createdAt(Instant.now())
                .build();

        testDepositDTO = new TransactionDTO();
        testDepositDTO.setId(1L);
        testDepositDTO.setTransactionType(TransactionType.DEPOSIT);
        testDepositDTO.setAmount(new BigDecimal("5000.00"));
        testDepositDTO.setStatus(TransactionStatus.PENDING);
        testDepositDTO.setExternalReference("DEP-ABC123");

        testWithdrawalDTO = new TransactionDTO();
        testWithdrawalDTO.setId(2L);
        testWithdrawalDTO.setTransactionType(TransactionType.WITHDRAWAL);
        testWithdrawalDTO.setAmount(new BigDecimal("2000.00"));
        testWithdrawalDTO.setStatus(TransactionStatus.PENDING);
        testWithdrawalDTO.setExternalReference("WDR-XYZ789");
    }

    @Nested
    @DisplayName("initiateDeposit tests")
    class InitiateDepositTests {

        @Test
        @DisplayName("Should create pending deposit transaction")
        void initiateDeposit_ShouldCreatePendingTransaction() {
            // Arrange
            DepositRequest request = new DepositRequest();
            request.setAmount(new BigDecimal("5000.00"));
            request.setDescription("Test deposit");
            request.setExternalReference("DEP-ABC123");

            when(walletService.getWalletEntityByUserId(100L)).thenReturn(testWallet);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testDeposit);
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testDepositDTO);

            // Act
            TransactionDTO result = transactionService.initiateDeposit(100L, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(result.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should generate external reference if not provided")
        void initiateDeposit_NoExternalRef_ShouldGenerate() {
            // Arrange
            DepositRequest request = new DepositRequest();
            request.setAmount(new BigDecimal("5000.00"));

            when(walletService.getWalletEntityByUserId(100L)).thenReturn(testWallet);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction saved = inv.getArgument(0);
                assertThat(saved.getExternalReference()).startsWith("DEP-");
                return testDeposit;
            });
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testDepositDTO);

            // Act
            transactionService.initiateDeposit(100L, request);

            // Assert
            verify(transactionRepository).save(argThat(tx -> tx.getExternalReference().startsWith("DEP-")));
        }
    }

    @Nested
    @DisplayName("confirmDeposit tests")
    class ConfirmDepositTests {

        @Test
        @DisplayName("Should complete deposit and credit wallet")
        void confirmDeposit_PendingDeposit_ShouldCompleteAndCredit() {
            // Arrange
            when(transactionRepository.findByExternalReference("DEP-ABC123")).thenReturn(Optional.of(testDeposit));
            when(walletService.getWalletById(1L)).thenReturn(testWallet);
            when(walletService.creditFunds(eq(100L), any(), eq("DEP-ABC123"), anyString(), anyString()))
                    .thenReturn(testWallet);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testDeposit);
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testDepositDTO);

            // Act
            TransactionDTO result = transactionService.confirmDeposit("DEP-ABC123");

            // Assert
            assertThat(result).isNotNull();
            verify(walletService).creditFunds(eq(100L), eq(new BigDecimal("5000.00")), eq("DEP-ABC123"), anyString(),
                    anyString());
        }

        @Test
        @DisplayName("Should not re-process non-pending deposits")
        void confirmDeposit_NotPending_ShouldNotReprocess() {
            // Arrange
            testDeposit.complete();
            when(transactionRepository.findByExternalReference("DEP-ABC123")).thenReturn(Optional.of(testDeposit));
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testDepositDTO);

            // Act
            TransactionDTO result = transactionService.confirmDeposit("DEP-ABC123");

            // Assert
            assertThat(result).isNotNull();
            verify(walletService, never()).creditFunds(anyLong(), any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw exception for unknown reference")
        void confirmDeposit_NotFound_ShouldThrowException() {
            // Arrange
            when(transactionRepository.findByExternalReference("UNKNOWN")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> transactionService.confirmDeposit("UNKNOWN"))
                    .isInstanceOf(TransactionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("initiateWithdrawal tests")
    class InitiateWithdrawalTests {

        @Test
        @DisplayName("Should create pending withdrawal with sufficient balance")
        void initiateWithdrawal_SufficientBalance_ShouldSucceed() {
            // Arrange
            WithdrawRequest request = new WithdrawRequest();
            request.setAmount(new BigDecimal("2000.00"));
            request.setDescription("Test withdrawal");

            when(walletService.getWalletEntityByUserId(100L)).thenReturn(testWallet);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testWithdrawal);
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testWithdrawalDTO);

            // Act
            TransactionDTO result = transactionService.initiateWithdrawal(100L, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw InsufficientFundsException when balance too low")
        void initiateWithdrawal_InsufficientBalance_ShouldThrowException() {
            // Arrange
            testWallet.setAvailableBalance(new BigDecimal("1000.00"));
            WithdrawRequest request = new WithdrawRequest();
            request.setAmount(new BigDecimal("5000.00"));

            when(walletService.getWalletEntityByUserId(100L)).thenReturn(testWallet);

            // Act & Assert
            assertThatThrownBy(() -> transactionService.initiateWithdrawal(100L, request))
                    .isInstanceOf(InsufficientFundsException.class);

            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("completeWithdrawal tests")
    class CompleteWithdrawalTests {

        @Test
        @DisplayName("Should complete withdrawal and debit wallet")
        void completeWithdrawal_PendingWithdrawal_ShouldComplete() {
            // Arrange
            when(transactionRepository.findByExternalReference("WDR-XYZ789")).thenReturn(Optional.of(testWithdrawal));
            when(walletService.getWalletById(1L)).thenReturn(testWallet);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testWithdrawal);
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testWithdrawalDTO);

            // Act
            TransactionDTO result = transactionService.completeWithdrawal("WDR-XYZ789");

            // Assert
            assertThat(result).isNotNull();
            verify(walletService).debitFunds(eq(100L), eq(new BigDecimal("2000.00")), eq("WDR-XYZ789"), anyString(),
                    anyString());
        }

        @Test
        @DisplayName("Should not re-process completed withdrawals")
        void completeWithdrawal_AlreadyCompleted_ShouldNotReprocess() {
            // Arrange
            testWithdrawal.complete();
            when(transactionRepository.findByExternalReference("WDR-XYZ789")).thenReturn(Optional.of(testWithdrawal));
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testWithdrawalDTO);

            // Act
            TransactionDTO result = transactionService.completeWithdrawal("WDR-XYZ789");

            // Assert
            assertThat(result).isNotNull();
            verify(walletService, never()).debitFunds(anyLong(), any(), anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("failTransaction tests")
    class FailTransactionTests {

        @Test
        @DisplayName("Should mark transaction as failed")
        void failTransaction_ShouldUpdateStatus() {
            // Arrange
            when(transactionRepository.findByExternalReference("DEP-ABC123")).thenReturn(Optional.of(testDeposit));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testDeposit);
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testDepositDTO);

            // Act
            TransactionDTO result = transactionService.failTransaction("DEP-ABC123", "Payment gateway error");

            // Assert
            assertThat(result).isNotNull();
            verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.FAILED));
        }

        @Test
        @DisplayName("Should throw exception for unknown transaction")
        void failTransaction_NotFound_ShouldThrowException() {
            // Arrange
            when(transactionRepository.findByExternalReference("UNKNOWN")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> transactionService.failTransaction("UNKNOWN", "Reason"))
                    .isInstanceOf(TransactionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelTransaction tests")
    class CancelTransactionTests {

        @Test
        @DisplayName("Should cancel pending transaction")
        void cancelTransaction_Pending_ShouldCancel() {
            // Arrange
            when(transactionRepository.findByExternalReference("DEP-ABC123")).thenReturn(Optional.of(testDeposit));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testDeposit);
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testDepositDTO);

            // Act
            TransactionDTO result = transactionService.cancelTransaction("DEP-ABC123", "User requested");

            // Assert
            assertThat(result).isNotNull();
            verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.CANCELLED));
        }

        @Test
        @DisplayName("Should not cancel non-pending transaction")
        void cancelTransaction_NotPending_ShouldNotCancel() {
            // Arrange
            testDeposit.complete();
            when(transactionRepository.findByExternalReference("DEP-ABC123")).thenReturn(Optional.of(testDeposit));
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testDepositDTO);

            // Act
            TransactionDTO result = transactionService.cancelTransaction("DEP-ABC123", "Too late");

            // Assert
            assertThat(result).isNotNull();
            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Query operations tests")
    class QueryTests {

        @Test
        @DisplayName("Should get transaction by reference")
        void getTransactionByReference_Exists_ShouldReturn() {
            // Arrange
            when(transactionRepository.findByExternalReference("DEP-ABC123")).thenReturn(Optional.of(testDeposit));
            when(fundsMapper.toTransactionDTO(testDeposit)).thenReturn(testDepositDTO);

            // Act
            TransactionDTO result = transactionService.getTransactionByReference("DEP-ABC123");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getExternalReference()).isEqualTo("DEP-ABC123");
        }

        @Test
        @DisplayName("Should throw exception for unknown reference")
        void getTransactionByReference_NotFound_ShouldThrowException() {
            // Arrange
            when(transactionRepository.findByExternalReference("UNKNOWN")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> transactionService.getTransactionByReference("UNKNOWN"))
                    .isInstanceOf(TransactionNotFoundException.class);
        }

        @Test
        @DisplayName("Should get paginated transactions for user")
        void getTransactionsForUser_ShouldReturnPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Transaction> txPage = new PageImpl<>(List.of(testDeposit, testWithdrawal));

            when(walletService.getWalletEntityByUserId(100L)).thenReturn(testWallet);
            when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(txPage);
            when(fundsMapper.toTransactionDTO(any(Transaction.class))).thenReturn(testDepositDTO, testWithdrawalDTO);

            // Act
            Page<TransactionDTO> result = transactionService.getTransactionsForUser(100L, pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
        }
    }
}

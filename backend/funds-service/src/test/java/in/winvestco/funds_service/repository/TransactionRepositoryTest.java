package in.winvestco.funds_service.repository;

import in.winvestco.common.enums.TransactionStatus;
import in.winvestco.common.enums.TransactionType;
import in.winvestco.funds_service.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private Transaction transaction1;
    private Transaction transaction2;
    private Transaction transaction3;
    private static final Long WALLET_ID_1 = 1L;
    private static final Long WALLET_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        transaction1 = Transaction.builder()
                .walletId(WALLET_ID_1)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("5000.00"))
                .status(TransactionStatus.COMPLETED)
                .externalReference("ref-001")
                .description("Initial deposit")
                .build();

        transaction2 = Transaction.builder()
                .walletId(WALLET_ID_1)
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("1000.00"))
                .status(TransactionStatus.PENDING)
                .externalReference("ref-002")
                .description("Withdrawal request")
                .build();

        transaction3 = Transaction.builder()
                .walletId(WALLET_ID_2)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("3000.00"))
                .status(TransactionStatus.COMPLETED)
                .externalReference("ref-003")
                .description("Deposit")
                .build();

        entityManager.persist(transaction1);
        entityManager.persist(transaction2);
        entityManager.persist(transaction3);
        entityManager.flush();
    }

    @Test
    void findByWalletIdOrderByCreatedAtDesc_ShouldReturnPagedTransactions() {
        Page<Transaction> transactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc(
                WALLET_ID_1, PageRequest.of(0, 10));

        assertEquals(2, transactions.getTotalElements());
        assertEquals(2, transactions.getContent().size());
    }

    @Test
    void findByExternalReference_WhenExists_ShouldReturnTransaction() {
        Optional<Transaction> found = transactionRepository.findByExternalReference("ref-001");

        assertTrue(found.isPresent());
        assertEquals("ref-001", found.get().getExternalReference());
        assertEquals(TransactionType.DEPOSIT, found.get().getTransactionType());
    }

    @Test
    void findByExternalReference_WhenNotExists_ShouldReturnEmpty() {
        Optional<Transaction> found = transactionRepository.findByExternalReference("non-existent");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByWalletIdAndStatus_ShouldReturnMatchingTransactions() {
        List<Transaction> transactions = transactionRepository.findByWalletIdAndStatus(
                WALLET_ID_1, TransactionStatus.COMPLETED);

        assertEquals(1, transactions.size());
        assertEquals(TransactionStatus.COMPLETED, transactions.get(0).getStatus());
    }

    @Test
    void findByWalletIdAndTransactionTypeOrderByCreatedAtDesc_ShouldReturnPaged() {
        Page<Transaction> transactions = transactionRepository
                .findByWalletIdAndTransactionTypeOrderByCreatedAtDesc(
                        WALLET_ID_1, TransactionType.DEPOSIT, PageRequest.of(0, 10));

        assertEquals(1, transactions.getTotalElements());
        assertEquals(TransactionType.DEPOSIT, transactions.getContent().get(0).getTransactionType());
    }

    @Test
    void findByStatus_ShouldReturnAllWithStatus() {
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);

        assertEquals(1, pendingTransactions.size());
        assertEquals(TransactionStatus.PENDING, pendingTransactions.get(0).getStatus());
    }

    @Test
    void countByWalletIdAndStatus_ShouldReturnCorrectCount() {
        long count = transactionRepository.countByWalletIdAndStatus(WALLET_ID_1, TransactionStatus.COMPLETED);

        assertEquals(1, count);
    }

    @Test
    void save_ShouldPersistTransaction() {
        Transaction newTransaction = Transaction.builder()
                .walletId(3L)
                .transactionType(TransactionType.LOCK)
                .amount(new BigDecimal("500.00"))
                .status(TransactionStatus.COMPLETED)
                .externalReference("ref-004")
                .description("Fund lock")
                .build();

        Transaction saved = transactionRepository.save(newTransaction);

        assertNotNull(saved.getId());
        assertEquals("ref-004", saved.getExternalReference());
    }

    @Test
    void transaction_ShouldUpdateStatus() {
        Transaction transaction = transactionRepository.findByExternalReference("ref-002").orElseThrow();
        transaction.setStatus(TransactionStatus.COMPLETED);
        entityManager.flush();

        Transaction updated = transactionRepository.findByExternalReference("ref-002").orElseThrow();
        assertEquals(TransactionStatus.COMPLETED, updated.getStatus());
    }
}

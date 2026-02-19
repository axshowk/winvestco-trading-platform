package in.winvestco.funds_service.repository;

import in.winvestco.common.enums.LockStatus;
import in.winvestco.funds_service.model.FundsLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class FundsLockRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FundsLockRepository fundsLockRepository;

    private FundsLock lock1;
    private FundsLock lock2;
    private FundsLock lock3;
    private static final Long WALLET_ID_1 = 1L;
    private static final Long WALLET_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        lock1 = FundsLock.builder()
                .walletId(WALLET_ID_1)
                .orderId("order-123")
                .amount(new BigDecimal("500.00"))
                .status(LockStatus.LOCKED)
                .reason("Order placement")
                .build();

        lock2 = FundsLock.builder()
                .walletId(WALLET_ID_1)
                .orderId("order-456")
                .amount(new BigDecimal("1000.00"))
                .status(LockStatus.RELEASED)
                .reason("Order cancelled")
                .build();

        lock3 = FundsLock.builder()
                .walletId(WALLET_ID_2)
                .orderId("order-789")
                .amount(new BigDecimal("250.00"))
                .status(LockStatus.LOCKED)
                .reason("Order placement")
                .build();

        entityManager.persist(lock1);
        entityManager.persist(lock2);
        entityManager.persist(lock3);
        entityManager.flush();
    }

    @Test
    void findByOrderId_WhenExists_ShouldReturnLock() {
        Optional<FundsLock> found = fundsLockRepository.findByOrderId("order-123");

        assertTrue(found.isPresent());
        assertEquals("order-123", found.get().getOrderId());
        assertEquals(new BigDecimal("500.00"), found.get().getAmount());
    }

    @Test
    void findByOrderId_WhenNotExists_ShouldReturnEmpty() {
        Optional<FundsLock> found = fundsLockRepository.findByOrderId("non-existent");

        assertTrue(found.isEmpty());
    }

    @Test
    void existsByOrderId_WhenExists_ShouldReturnTrue() {
        boolean exists = fundsLockRepository.existsByOrderId("order-123");

        assertTrue(exists);
    }

    @Test
    void existsByOrderId_WhenNotExists_ShouldReturnFalse() {
        boolean exists = fundsLockRepository.existsByOrderId("non-existent");

        assertFalse(exists);
    }

    @Test
    void findByWalletIdAndStatus_ShouldReturnMatchingLocks() {
        List<FundsLock> locks = fundsLockRepository.findByWalletIdAndStatus(WALLET_ID_1, LockStatus.LOCKED);

        assertEquals(1, locks.size());
        assertEquals("order-123", locks.get(0).getOrderId());
    }

    @Test
    void findByWalletIdAndStatusOrderByCreatedAtDesc_ShouldReturnOrderedLocks() {
        List<FundsLock> locks = fundsLockRepository.findByWalletIdAndStatusOrderByCreatedAtDesc(WALLET_ID_1, LockStatus.LOCKED);

        assertEquals(1, locks.size());
        assertEquals(LockStatus.LOCKED, locks.get(0).getStatus());
    }

    @Test
    void findByWalletIdOrderByCreatedAtDesc_ShouldReturnAllLocks() {
        List<FundsLock> locks = fundsLockRepository.findByWalletIdOrderByCreatedAtDesc(WALLET_ID_1);

        assertEquals(2, locks.size());
    }

    @Test
    void countByWalletIdAndStatus_ShouldReturnCorrectCount() {
        long count = fundsLockRepository.countByWalletIdAndStatus(WALLET_ID_1, LockStatus.LOCKED);

        assertEquals(1, count);
    }

    @Test
    void save_ShouldPersistLock() {
        FundsLock newLock = FundsLock.builder()
                .walletId(3L)
                .orderId("order-999")
                .amount(new BigDecimal("750.00"))
                .status(LockStatus.LOCKED)
                .reason("New order")
                .build();

        FundsLock saved = fundsLockRepository.save(newLock);

        assertNotNull(saved.getId());
        assertEquals("order-999", saved.getOrderId());
    }

    @Test
    void lock_ShouldRelease() {
        FundsLock lock = fundsLockRepository.findByOrderId("order-123").orElseThrow();
        lock.release("User cancelled");
        entityManager.flush();

        FundsLock updated = fundsLockRepository.findByOrderId("order-123").orElseThrow();
        assertEquals(LockStatus.RELEASED, updated.getStatus());
        assertEquals("User cancelled", updated.getReason());
    }

    @Test
    void lock_ShouldSettle() {
        FundsLock lock = fundsLockRepository.findByOrderId("order-123").orElseThrow();
        lock.settle("Trade executed");
        entityManager.flush();

        FundsLock updated = fundsLockRepository.findByOrderId("order-123").orElseThrow();
        assertEquals(LockStatus.SETTLED, updated.getStatus());
        assertEquals("Trade executed", updated.getReason());
    }
}

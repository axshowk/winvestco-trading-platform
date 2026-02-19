package in.winvestco.funds_service.repository;

import in.winvestco.common.enums.WalletStatus;
import in.winvestco.funds_service.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class WalletRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WalletRepository walletRepository;

    private Wallet wallet1;
    private Wallet wallet2;
    private static final Long USER_ID_1 = 1L;
    private static final Long USER_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        wallet1 = Wallet.builder()
                .userId(USER_ID_1)
                .availableBalance(new BigDecimal("10000.00"))
                .lockedBalance(new BigDecimal("500.00"))
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();

        wallet2 = Wallet.builder()
                .userId(USER_ID_2)
                .availableBalance(new BigDecimal("5000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();

        entityManager.persist(wallet1);
        entityManager.persist(wallet2);
        entityManager.flush();
    }

    @Test
    void findByUserId_WhenExists_ShouldReturnWallet() {
        Optional<Wallet> found = walletRepository.findByUserId(USER_ID_1);

        assertTrue(found.isPresent());
        assertEquals(USER_ID_1, found.get().getUserId());
        assertEquals(new BigDecimal("10000.00"), found.get().getAvailableBalance());
    }

    @Test
    void findByUserId_WhenNotExists_ShouldReturnEmpty() {
        Optional<Wallet> found = walletRepository.findByUserId(999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void existsByUserId_WhenExists_ShouldReturnTrue() {
        boolean exists = walletRepository.existsByUserId(USER_ID_1);

        assertTrue(exists);
    }

    @Test
    void existsByUserId_WhenNotExists_ShouldReturnFalse() {
        boolean exists = walletRepository.existsByUserId(999L);

        assertFalse(exists);
    }

    @Test
    void findByUserIdForUpdate_WhenExists_ShouldReturnWallet() {
        Optional<Wallet> found = walletRepository.findByUserIdForUpdate(USER_ID_1);

        assertTrue(found.isPresent());
        assertEquals(USER_ID_1, found.get().getUserId());
    }

    @Test
    void findByIdForUpdate_WhenExists_ShouldReturnWallet() {
        Long walletId = wallet1.getId();
        Optional<Wallet> found = walletRepository.findByIdForUpdate(walletId);

        assertTrue(found.isPresent());
        assertEquals(walletId, found.get().getId());
    }

    @Test
    void save_ShouldPersistWallet() {
        Wallet newWallet = Wallet.builder()
                .userId(3L)
                .availableBalance(new BigDecimal("1000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet saved = walletRepository.save(newWallet);

        assertNotNull(saved.getId());
        assertEquals(3L, saved.getUserId());
    }

    @Test
    void wallet_ShouldCalculateTotalBalance() {
        Wallet wallet = walletRepository.findByUserId(USER_ID_1).orElseThrow();

        BigDecimal total = wallet.getTotalBalance();

        assertEquals(new BigDecimal("10500.00"), total);
    }

    @Test
    void wallet_ShouldCheckSufficientBalance() {
        Wallet wallet = walletRepository.findByUserId(USER_ID_1).orElseThrow();

        assertTrue(wallet.hasSufficientBalance(new BigDecimal("5000.00")));
        assertFalse(wallet.hasSufficientBalance(new BigDecimal("15000.00")));
    }
}

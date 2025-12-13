package in.winvestco.funds_service.repository;

import in.winvestco.common.enums.LockStatus;
import in.winvestco.funds_service.model.FundsLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FundsLock entity operations.
 */
@Repository
public interface FundsLockRepository extends JpaRepository<FundsLock, Long> {

    /**
     * Find lock by order ID
     */
    Optional<FundsLock> findByOrderId(String orderId);

    /**
     * Check if lock exists for order
     */
    boolean existsByOrderId(String orderId);

    /**
     * Find locks by wallet ID and status
     */
    List<FundsLock> findByWalletIdAndStatus(Long walletId, LockStatus status);

    /**
     * Find all active locks for a wallet
     */
    List<FundsLock> findByWalletIdAndStatusOrderByCreatedAtDesc(Long walletId, LockStatus status);

    /**
     * Find all locks for a wallet
     */
    List<FundsLock> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    /**
     * Count active locks for a wallet
     */
    long countByWalletIdAndStatus(Long walletId, LockStatus status);
}

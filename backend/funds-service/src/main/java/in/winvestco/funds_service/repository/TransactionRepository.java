package in.winvestco.funds_service.repository;

import in.winvestco.common.enums.TransactionStatus;
import in.winvestco.common.enums.TransactionType;
import in.winvestco.funds_service.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Transaction entity operations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find transactions by wallet ID (paginated)
     */
    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Find transaction by external reference
     */
    Optional<Transaction> findByExternalReference(String externalReference);

    /**
     * Find transactions by wallet and status
     */
    List<Transaction> findByWalletIdAndStatus(Long walletId, TransactionStatus status);

    /**
     * Find transactions by wallet and type
     */
    Page<Transaction> findByWalletIdAndTransactionTypeOrderByCreatedAtDesc(
            Long walletId, TransactionType transactionType, Pageable pageable);

    /**
     * Find pending transactions for processing
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Count transactions by wallet and status
     */
    long countByWalletIdAndStatus(Long walletId, TransactionStatus status);
}

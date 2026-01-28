package in.winvestco.portfolio_service.repository;

import in.winvestco.common.enums.PortfolioStatus;
import in.winvestco.portfolio_service.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Portfolio entity operations.
 * Each user has exactly one portfolio.
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Find default portfolio by user ID
     */
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId AND p.isDefault = true")
    Optional<Portfolio> findDefaultByUserId(@Param("userId") Long userId);

    /**
     * Find all portfolios for a user
     */
    java.util.List<Portfolio> findAllByUserId(Long userId);

    /**
     * Find portfolio by user ID and status
     */
    java.util.List<Portfolio> findAllByUserIdAndStatus(Long userId, PortfolioStatus status);

    /**
     * Check if a portfolio exists for a user
     */
    boolean existsByUserId(Long userId);

    /**
     * Find portfolio by ID and user ID (for security validation)
     */
    Optional<Portfolio> findByIdAndUserId(Long id, Long userId);

    /**
     * Find default portfolio with holdings eagerly loaded
     */
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.userId = :userId AND p.isDefault = true")
    Optional<Portfolio> findDefaultByUserIdWithHoldings(@Param("userId") Long userId);

    /**
     * Find portfolio by user ID with holdings eagerly loaded
     */
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.userId = :userId")
    java.util.List<Portfolio> findAllByUserIdWithHoldings(@Param("userId") Long userId);

    /**
     * Find portfolio by ID with holdings eagerly loaded
     */
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.id = :id")
    Optional<Portfolio> findByIdWithHoldings(@Param("id") Long id);

    /**
     * Check if user has a default portfolio
     */
    boolean existsByUserIdAndIsDefaultTrue(Long userId);
}

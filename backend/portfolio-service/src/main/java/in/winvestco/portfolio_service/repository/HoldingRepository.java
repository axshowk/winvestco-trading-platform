package in.winvestco.portfolio_service.repository;

import in.winvestco.portfolio_service.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Holding entity operations.
 */
@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    /**
     * Find all holdings for a portfolio
     */
    List<Holding> findByPortfolioId(Long portfolioId);

    /**
     * Find holding by portfolio ID and symbol
     */
    Optional<Holding> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    /**
     * Check if a holding exists for a portfolio and symbol
     */
    boolean existsByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    /**
     * Find holdings by symbol across all portfolios (for admin/analytics)
     */
    List<Holding> findBySymbol(String symbol);

    /**
     * Delete all holdings for a portfolio
     */
    void deleteByPortfolioId(Long portfolioId);

    /**
     * Count holdings in a portfolio
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * Find holdings for a user's portfolio
     */
    @Query("SELECT h FROM Holding h WHERE h.portfolio.userId = :userId")
    List<Holding> findByUserId(@Param("userId") Long userId);
}

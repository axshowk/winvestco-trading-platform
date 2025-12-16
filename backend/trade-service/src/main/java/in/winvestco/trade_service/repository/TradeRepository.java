package in.winvestco.trade_service.repository;

import in.winvestco.common.enums.TradeStatus;
import in.winvestco.trade_service.model.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Trade entity operations.
 */
@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    /**
     * Find trade by tradeId
     */
    Optional<Trade> findByTradeId(String tradeId);

    /**
     * Find trade by orderId
     */
    Optional<Trade> findByOrderId(String orderId);

    /**
     * Find all trades for a user, ordered by creation date
     */
    Page<Trade> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find trades by user and status
     */
    List<Trade> findByUserIdAndStatusIn(Long userId, List<TradeStatus> statuses);

    /**
     * Find active (non-terminal) trades for a user
     */
    @Query("SELECT t FROM Trade t WHERE t.userId = :userId AND t.status NOT IN :terminalStatuses ORDER BY t.createdAt DESC")
    List<Trade> findActiveTradesByUserId(
            @Param("userId") Long userId, 
            @Param("terminalStatuses") List<TradeStatus> terminalStatuses);

    /**
     * Find trades by symbol and status
     */
    List<Trade> findBySymbolAndStatusIn(String symbol, List<TradeStatus> statuses);

    /**
     * Find trades in PLACED status for a given time period (for monitoring)
     */
    @Query("SELECT t FROM Trade t WHERE t.status = :status AND t.placedAt < :before")
    List<Trade> findStuckTrades(
            @Param("status") TradeStatus status, 
            @Param("before") Instant before);

    /**
     * Count trades by user and status
     */
    long countByUserIdAndStatus(Long userId, TradeStatus status);

    /**
     * Find trades by user in date range
     */
    @Query("SELECT t FROM Trade t WHERE t.userId = :userId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Trade> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Check if trade exists for order
     */
    boolean existsByOrderId(String orderId);
}

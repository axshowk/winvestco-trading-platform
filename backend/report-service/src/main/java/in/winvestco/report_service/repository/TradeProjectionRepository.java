package in.winvestco.report_service.repository;

import in.winvestco.report_service.model.projection.TradeProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeProjectionRepository extends JpaRepository<TradeProjection, Long> {

    Optional<TradeProjection> findByTradeId(String tradeId);

    boolean existsByTradeId(String tradeId);

    List<TradeProjection> findByUserIdOrderByExecutedAtDesc(Long userId);

    Page<TradeProjection> findByUserIdOrderByExecutedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT t FROM TradeProjection t WHERE t.userId = :userId " +
           "AND t.executedAt >= :fromDate AND t.executedAt <= :toDate " +
           "ORDER BY t.executedAt DESC")
    List<TradeProjection> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);

    @Query("SELECT t FROM TradeProjection t WHERE t.userId = :userId " +
           "AND t.symbol = :symbol AND t.executedAt >= :fromDate AND t.executedAt <= :toDate " +
           "ORDER BY t.executedAt DESC")
    List<TradeProjection> findByUserIdAndSymbolAndDateRange(
            @Param("userId") Long userId,
            @Param("symbol") String symbol,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);

    @Query("SELECT DISTINCT t.symbol FROM TradeProjection t WHERE t.userId = :userId")
    List<String> findDistinctSymbolsByUserId(@Param("userId") Long userId);
}

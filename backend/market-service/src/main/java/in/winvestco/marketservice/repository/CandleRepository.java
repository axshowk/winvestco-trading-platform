package in.winvestco.marketservice.repository;

import in.winvestco.marketservice.entity.Candle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Candle entity.
 * Provides methods for querying historical candle data.
 */
@Repository
public interface CandleRepository extends JpaRepository<Candle, Long> {

    /**
     * Find candles for a symbol and interval within a time range.
     */
    List<Candle> findBySymbolAndIntervalTypeAndTimestampBetweenOrderByTimestampAsc(
            String symbol,
            String intervalType,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Find the latest candle for a symbol and interval.
     */
    Optional<Candle> findFirstBySymbolAndIntervalTypeOrderByTimestampDesc(
            String symbol,
            String intervalType);

    /**
     * Find candles for a symbol and interval, limited count, ordered by time desc.
     */
    @Query("SELECT c FROM Candle c WHERE c.symbol = :symbol AND c.intervalType = :intervalType " +
            "ORDER BY c.timestamp DESC LIMIT :limit")
    List<Candle> findRecentCandles(
            @Param("symbol") String symbol,
            @Param("intervalType") String intervalType,
            @Param("limit") int limit);

    /**
     * Check if a candle already exists for a given symbol, interval, and timestamp.
     */
    boolean existsBySymbolAndIntervalTypeAndTimestamp(
            String symbol,
            String intervalType,
            LocalDateTime timestamp);
}

package in.winvestco.marketservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a candlestick (OHLCV) data point.
 * Stores historical price data for charting purposes.
 */
@Entity
@Table(name = "candles", uniqueConstraints = @UniqueConstraint(columnNames = { "symbol", "interval_type",
        "timestamp" }), indexes = @Index(name = "idx_candles_symbol_interval_time", columnList = "symbol, interval_type, timestamp DESC"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String symbol;

    @Column(name = "interval_type", nullable = false, length = 10)
    private String intervalType; // '5m', '15m', '1h', '1d'

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal open;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal high;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal low;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal close;

    @Column(nullable = false)
    @Builder.Default
    private Long volume = 0L;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

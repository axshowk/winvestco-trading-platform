package in.winvestco.report_service.model.projection;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Trade Projection - Local projection populated from Trade domain events.
 * Used for P&L calculations and trade history reports.
 */
@Entity
@Table(name = "trade_projections", indexes = {
    @Index(name = "idx_trade_proj_user_id", columnList = "user_id"),
    @Index(name = "idx_trade_proj_symbol", columnList = "symbol"),
    @Index(name = "idx_trade_proj_executed_at", columnList = "executed_at"),
    @Index(name = "idx_trade_proj_user_date", columnList = "user_id, executed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TradeProjection implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "trade_id", nullable = false, unique = true, length = 36)
    private String tradeId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "side", nullable = false, length = 10)
    private String side; // BUY or SELL

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Calculate trade value
     */
    public BigDecimal getValue() {
        return quantity.multiply(price);
    }

    /**
     * Check if this is a buy trade
     */
    public boolean isBuy() {
        return "BUY".equalsIgnoreCase(side);
    }

    /**
     * Check if this is a sell trade
     */
    public boolean isSell() {
        return "SELL".equalsIgnoreCase(side);
    }
}

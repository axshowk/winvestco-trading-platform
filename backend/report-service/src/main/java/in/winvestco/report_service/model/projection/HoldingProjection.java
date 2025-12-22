package in.winvestco.report_service.model.projection;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Holding Projection - Local projection of current holdings.
 * Updated from TradeExecutedEvent (adjusts quantity/avg price).
 */
@Entity
@Table(name = "holding_projections", indexes = {
    @Index(name = "idx_holding_proj_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HoldingProjection implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "average_price", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal averagePrice = BigDecimal.ZERO;

    @Column(name = "total_invested", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalInvested = BigDecimal.ZERO;

    @Column(name = "last_updated_at", nullable = false)
    @Builder.Default
    private Instant lastUpdatedAt = Instant.now();

    /**
     * Apply a BUY trade - increases quantity and adjusts average price
     */
    public void applyBuy(BigDecimal buyQuantity, BigDecimal buyPrice) {
        BigDecimal totalValue = this.quantity.multiply(this.averagePrice)
                .add(buyQuantity.multiply(buyPrice));
        this.quantity = this.quantity.add(buyQuantity);
        
        if (this.quantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averagePrice = totalValue.divide(this.quantity, 4, java.math.RoundingMode.HALF_UP);
        }
        
        this.totalInvested = this.quantity.multiply(this.averagePrice);
        this.lastUpdatedAt = Instant.now();
    }

    /**
     * Apply a SELL trade - decreases quantity
     */
    public void applySell(BigDecimal sellQuantity) {
        this.quantity = this.quantity.subtract(sellQuantity);
        if (this.quantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantity = BigDecimal.ZERO;
            this.averagePrice = BigDecimal.ZERO;
        }
        this.totalInvested = this.quantity.multiply(this.averagePrice);
        this.lastUpdatedAt = Instant.now();
    }

    /**
     * Calculate current value at given market price
     */
    public BigDecimal getCurrentValue(BigDecimal currentPrice) {
        return this.quantity.multiply(currentPrice);
    }

    /**
     * Calculate unrealized P&L at given market price
     */
    public BigDecimal getUnrealizedPnL(BigDecimal currentPrice) {
        return getCurrentValue(currentPrice).subtract(this.totalInvested);
    }
}

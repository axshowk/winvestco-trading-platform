package in.winvestco.trade_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.TradeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Trade entity representing a trade in the system.
 * 
 * Trade Lifecycle:
 * CREATED → VALIDATED → PLACED → EXECUTING → FILLED → CLOSED
 *                                    ↘ PARTIALLY_FILLED ↗
 */
@Entity
@Table(name = "trades", indexes = {
    @Index(name = "idx_trades_trade_id", columnList = "trade_id", unique = true),
    @Index(name = "idx_trades_order_id", columnList = "order_id"),
    @Index(name = "idx_trades_user_id", columnList = "user_id"),
    @Index(name = "idx_trades_status", columnList = "status"),
    @Index(name = "idx_trades_symbol", columnList = "symbol")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "trade_id", nullable = false, unique = true, length = 36)
    private String tradeId;

    @NotNull
    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private OrderSide side;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 20)
    private OrderType tradeType;

    @NotNull
    @Positive
    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 18, scale = 4)
    private BigDecimal price;

    @Column(name = "executed_quantity", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal executedQuantity = BigDecimal.ZERO;

    @Column(name = "average_price", precision = 18, scale = 4)
    private BigDecimal averagePrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TradeStatus status = TradeStatus.CREATED;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "placed_at")
    private Instant placedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    // ==================== Lifecycle Methods ====================

    /**
     * Get remaining quantity to be executed
     */
    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(executedQuantity != null ? executedQuantity : BigDecimal.ZERO);
    }

    /**
     * Check if trade is fully filled
     */
    public boolean isFullyFilled() {
        return getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if trade is in a terminal state
     */
    public boolean isTerminal() {
        return status == TradeStatus.CLOSED ||
               status == TradeStatus.CANCELLED ||
               status == TradeStatus.FAILED;
    }

    /**
     * Check if trade can be cancelled
     */
    public boolean isCancellable() {
        return status == TradeStatus.CREATED ||
               status == TradeStatus.VALIDATED ||
               status == TradeStatus.PLACED ||
               status == TradeStatus.EXECUTING ||
               status == TradeStatus.PARTIALLY_FILLED;
    }

    /**
     * Check if trade can be placed (sent to execution)
     */
    public boolean canBePlaced() {
        return status == TradeStatus.VALIDATED;
    }

    /**
     * Calculate total trade value
     */
    public BigDecimal getTotalValue() {
        if (price == null) {
            return null;
        }
        return quantity.multiply(price);
    }

    /**
     * Calculate executed value
     */
    public BigDecimal getExecutedValue() {
        if (averagePrice == null || executedQuantity == null) {
            return BigDecimal.ZERO;
        }
        return executedQuantity.multiply(averagePrice);
    }
}

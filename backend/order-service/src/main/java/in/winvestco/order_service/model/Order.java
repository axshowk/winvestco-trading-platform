package in.winvestco.order_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.OrderValidity;
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
 * Order entity representing a trading order.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_order_id", columnList = "order_id", unique = true),
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_symbol", columnList = "symbol")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "order_id", nullable = false, unique = true, length = 36)
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
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @NotNull
    @Positive
    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 18, scale = 4)
    private BigDecimal price;

    @Column(name = "stop_price", precision = 18, scale = 4)
    private BigDecimal stopPrice;

    @Column(name = "filled_quantity", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(name = "average_price", precision = 18, scale = 4)
    private BigDecimal averagePrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.NEW;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "validity", nullable = false, length = 10)
    @Builder.Default
    private OrderValidity validity = OrderValidity.DAY;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Get remaining quantity to be filled
     */
    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(filledQuantity != null ? filledQuantity : BigDecimal.ZERO);
    }

    /**
     * Check if order is fully filled
     */
    public boolean isFullyFilled() {
        return getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if order is in a terminal state
     */
    public boolean isTerminal() {
        return status == OrderStatus.FILLED ||
               status == OrderStatus.CANCELLED ||
               status == OrderStatus.REJECTED ||
               status == OrderStatus.EXPIRED;
    }

    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return status == OrderStatus.NEW ||
               status == OrderStatus.VALIDATED ||
               status == OrderStatus.FUNDS_LOCKED ||
               status == OrderStatus.PENDING ||
               status == OrderStatus.PARTIALLY_FILLED;
    }

    /**
     * Calculate total order value
     */
    public BigDecimal getTotalValue() {
        if (price == null) {
            return null;
        }
        return quantity.multiply(price);
    }
}

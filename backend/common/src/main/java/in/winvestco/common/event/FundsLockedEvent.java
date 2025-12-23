package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted by funds-service when funds are successfully locked for an
 * order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FundsLockedEvent extends BaseEvent {

    private String orderId;
    private Long userId;
    private Long walletId;
    private BigDecimal lockedAmount;
    private String lockId;
    private Instant lockedAt;

    // Trading details for trade creation
    private String symbol;
    private OrderSide side;
    private OrderType orderType;
    private BigDecimal quantity;
    private BigDecimal price;

    // Alias for backwards compatibility
    public BigDecimal getAmount() {
        return lockedAmount;
    }
}

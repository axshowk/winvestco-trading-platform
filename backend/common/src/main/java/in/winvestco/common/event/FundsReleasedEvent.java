package in.winvestco.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when locked funds are released (order
 * cancelled/expired/rejected).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FundsReleasedEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long walletId;
    private String orderId; // Order that was cancelled/expired
    private String lockId;
    private BigDecimal releasedAmount;
    private String releaseReason; // "ORDER_CANCELLED", "ORDER_EXPIRED", "ORDER_REJECTED"
    private Instant releasedAt;
}

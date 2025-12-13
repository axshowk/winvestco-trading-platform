package in.winvestco.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted by funds-service when funds are successfully locked for an
 * order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundsLockedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId;
    private Long userId;
    private Long walletId;
    private BigDecimal lockedAmount;
    private String lockId;
    private Instant lockedAt;
}

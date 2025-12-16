package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a trade fails due to validation or system error.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tradeId;
    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private BigDecimal quantity;
    private String failureReason;
    private String errorCode;
    private TradeStatus previousStatus;
    private Instant failedAt;
}

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
 * Event emitted when a trade is cancelled by user or system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeCancelledEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tradeId;
    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal executedQuantity;
    private String cancelledBy; // USER or SYSTEM
    private String cancelReason;
    private TradeStatus previousStatus;
    private Instant cancelledAt;
}

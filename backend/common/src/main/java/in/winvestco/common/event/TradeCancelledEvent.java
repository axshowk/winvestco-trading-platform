package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a trade is cancelled by user or system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class TradeCancelledEvent extends BaseEvent {
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

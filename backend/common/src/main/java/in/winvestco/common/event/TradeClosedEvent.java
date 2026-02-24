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
 * Event emitted when a trade is fully settled and closed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class TradeClosedEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;

    private String tradeId;
    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private BigDecimal executedQuantity;
    private BigDecimal averagePrice;
    private BigDecimal totalValue;
    private TradeStatus status;
    private Instant closedAt;
}

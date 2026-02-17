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
 * Event emitted when a trade is sent to the execution engine.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TradePlacedEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;

    private String tradeId;
    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal price;
    private TradeStatus status;
    private Instant placedAt;
}

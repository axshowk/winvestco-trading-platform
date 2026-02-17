package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a trade is executed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class TradeExecutedEvent extends BaseEvent {

    private String tradeId;
    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private BigDecimal executedQuantity;
    private BigDecimal executedPrice;
    private BigDecimal totalValue;
    private boolean isPartialFill;
    private Instant executedAt;
}

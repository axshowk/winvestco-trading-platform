package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a trade is executed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeExecutedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

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

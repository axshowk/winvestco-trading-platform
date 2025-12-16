package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when a new trade is created from an order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tradeId;
    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private OrderType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private TradeStatus status;
    private Instant createdAt;
}

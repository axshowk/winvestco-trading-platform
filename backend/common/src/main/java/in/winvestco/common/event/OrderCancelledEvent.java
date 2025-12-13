package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when an order is cancelled by user or system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelledEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private OrderType orderType;
    private BigDecimal quantity;
    private BigDecimal price;
    private String cancelReason;
    private String cancelledBy; // "USER" or "SYSTEM"
    private Instant cancelledAt;
}

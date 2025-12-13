package in.winvestco.common.event;

import in.winvestco.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when an order is filled (completely or partially).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFilledEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId;
    private Long userId;
    private String symbol;
    private BigDecimal filledQuantity;
    private BigDecimal totalQuantity;
    private BigDecimal averagePrice;
    private OrderStatus status; // FILLED or PARTIALLY_FILLED
    private Instant filledAt;
}

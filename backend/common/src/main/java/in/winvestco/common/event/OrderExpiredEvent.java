package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.OrderValidity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when an order expires (EOD for DAY orders, GTD expiry, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderExpiredEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private OrderType orderType;
    private BigDecimal quantity;
    private BigDecimal filledQuantity; // Amount filled before expiry
    private BigDecimal price;
    private OrderValidity validity;
    private Instant expiredAt;
}

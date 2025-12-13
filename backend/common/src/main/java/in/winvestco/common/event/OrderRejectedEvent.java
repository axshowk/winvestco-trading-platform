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
 * Event emitted when an order is rejected (validation failure, insufficient
 * funds, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRejectedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private OrderType orderType;
    private BigDecimal quantity;
    private BigDecimal price;
    private String rejectionReason;
    private String rejectedBy; // "VALIDATION", "FUNDS", "MARKET", "SYSTEM"
    private Instant rejectedAt;
}

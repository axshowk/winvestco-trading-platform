package in.winvestco.common.event;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when an order is rejected.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderRejectedEvent extends BaseEvent {

    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private OrderType orderType;
    private BigDecimal quantity;
    private BigDecimal price;
    private String rejectionReason;
    private String rejectedBy;
    private Instant rejectedAt;
}

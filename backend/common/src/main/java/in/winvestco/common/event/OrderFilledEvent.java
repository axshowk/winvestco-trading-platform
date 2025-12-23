package in.winvestco.common.event;

import in.winvestco.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted when an order is partially or fully filled.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderFilledEvent extends BaseEvent {

    private String orderId;
    private Long userId;
    private String symbol;
    private BigDecimal filledQuantity;
    private BigDecimal totalQuantity;
    private BigDecimal averagePrice;
    private OrderStatus status;
    private Instant filledAt;
}

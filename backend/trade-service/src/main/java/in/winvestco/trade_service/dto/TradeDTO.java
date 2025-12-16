package in.winvestco.trade_service.dto;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object for Trade responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDTO {
    private String tradeId;
    private String orderId;
    private Long userId;
    private String symbol;
    private OrderSide side;
    private OrderType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal executedQuantity;
    private BigDecimal averagePrice;
    private BigDecimal remainingQuantity;
    private TradeStatus status;
    private String failureReason;
    private Instant createdAt;
    private Instant validatedAt;
    private Instant placedAt;
    private Instant executedAt;
    private Instant closedAt;
}

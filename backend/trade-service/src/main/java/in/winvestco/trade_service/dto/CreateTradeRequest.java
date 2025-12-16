package in.winvestco.trade_service.dto;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a trade (usually from event processing).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTradeRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Side is required")
    private OrderSide side;

    @NotNull(message = "Trade type is required")
    private OrderType tradeType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    private BigDecimal price;
}

package in.winvestco.order_service.dto;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.OrderValidity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Side is required")
    private OrderSide side;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    /**
     * Price for LIMIT and STOP_LIMIT orders
     */
    private BigDecimal price;

    /**
     * Stop price for STOP_LOSS and STOP_LIMIT orders
     */
    private BigDecimal stopPrice;

    /**
     * Order validity (defaults to DAY if not specified)
     */
    @Builder.Default
    private OrderValidity validity = OrderValidity.DAY;
}

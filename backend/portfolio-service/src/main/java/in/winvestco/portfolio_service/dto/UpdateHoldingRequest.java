package in.winvestco.portfolio_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating a holding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHoldingRequest {
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
    
    @NotNull(message = "Average price is required")
    @Positive(message = "Average price must be positive")
    private BigDecimal averagePrice;
}

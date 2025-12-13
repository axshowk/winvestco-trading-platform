package in.winvestco.portfolio_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for adding a holding to a portfolio
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddHoldingRequest {
    
    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    private String symbol;
    
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    private String companyName;
    
    @Size(max = 10, message = "Exchange must not exceed 10 characters")
    @Builder.Default
    private String exchange = "NSE";
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
    
    @NotNull(message = "Average price is required")
    @Positive(message = "Average price must be positive")
    private BigDecimal averagePrice;
}

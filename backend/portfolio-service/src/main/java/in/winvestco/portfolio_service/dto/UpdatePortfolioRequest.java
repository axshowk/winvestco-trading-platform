package in.winvestco.portfolio_service.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating portfolio details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePortfolioRequest {
    
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

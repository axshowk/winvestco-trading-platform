package in.winvestco.funds_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for releasing locked funds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseFundsRequest {
    
    @NotBlank(message = "Order ID is required")
    @Size(max = 100, message = "Order ID must not exceed 100 characters")
    private String orderId;
    
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}

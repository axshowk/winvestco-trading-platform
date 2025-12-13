package in.winvestco.portfolio_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for Holding response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingDTO {
    private Long id;
    private Long portfolioId;
    private String symbol;
    private String companyName;
    private String exchange;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal totalInvested;
    
    // Market data (populated from market-service)
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercentage;
    private BigDecimal dayChange;
    private BigDecimal dayChangePercentage;
    
    private Instant createdAt;
    private Instant updatedAt;
}

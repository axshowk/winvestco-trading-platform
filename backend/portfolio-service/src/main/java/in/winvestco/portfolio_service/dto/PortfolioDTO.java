package in.winvestco.portfolio_service.dto;

import in.winvestco.common.enums.PortfolioStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for Portfolio response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDTO {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private PortfolioStatus status;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercentage;
    private List<HoldingDTO> holdings;
    private Instant createdAt;
    private Instant updatedAt;
}

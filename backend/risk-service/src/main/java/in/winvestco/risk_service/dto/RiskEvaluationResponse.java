package in.winvestco.risk_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvaluationResponse {
    private String symbol;
    private RiskLevel riskLevel;
    private String reasoning;
}

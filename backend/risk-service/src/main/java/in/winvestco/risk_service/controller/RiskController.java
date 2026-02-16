package in.winvestco.risk_service.controller;

import in.winvestco.risk_service.dto.RiskEvaluationRequest;
import in.winvestco.risk_service.dto.RiskEvaluationResponse;
import in.winvestco.risk_service.service.RiskAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskAnalysisService riskAnalysisService;

    @PostMapping("/evaluate-news")
    public ResponseEntity<RiskEvaluationResponse> evaluateNewsRisk(@RequestBody RiskEvaluationRequest request) {
        return ResponseEntity.ok(riskAnalysisService.evaluateRisk(request));
    }
}

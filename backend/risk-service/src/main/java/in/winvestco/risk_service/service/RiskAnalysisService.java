package in.winvestco.risk_service.service;

import in.winvestco.risk_service.agent.NewsRiskAgent;
import in.winvestco.risk_service.dto.RiskEvaluationRequest;
import in.winvestco.risk_service.dto.RiskEvaluationResponse;
import in.winvestco.risk_service.dto.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RiskAnalysisService {

    private final NewsSourceService newsSourceService;
    private final NewsRiskAgent newsRiskAgent;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RiskEvaluationResponse evaluateRisk(RiskEvaluationRequest request) {
        long startNanos = System.nanoTime();
        log.info("Evaluating risk for symbol: {}", request.getSymbol());

        List<String> news = newsSourceService.getNewsForSymbol(request.getSymbol());
        String newsContent = String.join("\n", news);
        
        log.debug("Fetched news for {}: {}", request.getSymbol(), newsContent);

        try {
            // Context injection: Add symbol to the prompt content
            String prompt = "Symbol: " + request.getSymbol() + "\nNews:\n" + newsContent;
            
            String rawResponse = newsRiskAgent.evaluateRisk(prompt);
            log.debug("Raw AI response: {}", rawResponse);

            // Clean the response: Remove markdown code blocks if present
            String cleanedJson = rawResponse;
            if (rawResponse.contains("```json")) {
                cleanedJson = rawResponse.substring(rawResponse.indexOf("```json") + 7);
                if (cleanedJson.contains("```")) {
                    cleanedJson = cleanedJson.substring(0, cleanedJson.indexOf("```"));
                }
            } else if (rawResponse.contains("```")) {
                cleanedJson = rawResponse.substring(rawResponse.indexOf("```") + 3);
                if (cleanedJson.contains("```")) {
                    cleanedJson = cleanedJson.substring(0, cleanedJson.indexOf("```"));
                }
            }
            
            cleanedJson = cleanedJson.trim();
            log.debug("Cleaned JSON: {}", cleanedJson);

            RiskEvaluationResponse response = objectMapper.readValue(cleanedJson, RiskEvaluationResponse.class);
            
            // Ensure symbol is set in response if AI missed it
            if (response.getSymbol() == null) {
                response.setSymbol(request.getSymbol());
            }

            meterRegistry.counter("risk.evaluation.count",
                    "symbol", request.getSymbol(),
                    "risk_level", response.getRiskLevel().name()).increment();
            meterRegistry.timer("risk.evaluation.duration")
                    .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            
            log.info("Risk evaluation for {}: {} - {}", request.getSymbol(), response.getRiskLevel(), response.getReasoning());
            return response;
        } catch (Exception e) {
            log.error("Error during AI risk evaluation. Raw response might be invalid.", e);
            meterRegistry.counter("risk.evaluation.count",
                    "symbol", request.getSymbol(),
                    "risk_level", "ERROR").increment();
            return RiskEvaluationResponse.builder()
                    .symbol(request.getSymbol())
                    .riskLevel(RiskLevel.HIGH)
                    .reasoning("AI Evaluation Failed: " + e.getMessage())
                    .build();
        }
    }
}

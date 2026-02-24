package in.winvestco.risk_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.risk_service.agent.NewsRiskAgent;
import in.winvestco.risk_service.dto.RiskEvaluationRequest;
import in.winvestco.risk_service.dto.RiskEvaluationResponse;
import in.winvestco.risk_service.dto.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAnalysisServiceTest {

    @Mock
    private NewsSourceService newsSourceService;

    @Mock
    private NewsRiskAgent newsRiskAgent;

    @InjectMocks
    private RiskAnalysisService riskAnalysisService;

    private RiskEvaluationRequest request;

    @BeforeEach
    void setUp() {
        // Use a real ObjectMapper for JSON parsing tests
        ObjectMapper objectMapper = new ObjectMapper();
        riskAnalysisService = new RiskAnalysisService(newsSourceService, newsRiskAgent, objectMapper);

        request = RiskEvaluationRequest.builder()
                .symbol("RELIANCE")
                .build();
    }

    @Test
    @DisplayName("evaluateRisk - valid JSON response from AI → parsed correctly")
    void evaluateRisk_ValidJson_ShouldReturnParsedResponse() {
        when(newsSourceService.getNewsForSymbol("RELIANCE")).thenReturn(List.of("Good earnings report"));
        when(newsRiskAgent.evaluateRisk(anyString())).thenReturn(
                "{\"symbol\":\"RELIANCE\",\"riskLevel\":\"LOW\",\"reasoning\":\"Positive earnings\"}");

        RiskEvaluationResponse response = riskAnalysisService.evaluateRisk(request);

        assertEquals("RELIANCE", response.getSymbol());
        assertEquals(RiskLevel.LOW, response.getRiskLevel());
        assertEquals("Positive earnings", response.getReasoning());
    }

    @Test
    @DisplayName("evaluateRisk - JSON wrapped in ```json code block → cleaned and parsed")
    void evaluateRisk_JsonInCodeBlock_ShouldCleanAndParse() {
        when(newsSourceService.getNewsForSymbol("RELIANCE")).thenReturn(List.of("News headline"));
        when(newsRiskAgent.evaluateRisk(anyString())).thenReturn(
                "```json\n{\"symbol\":\"RELIANCE\",\"riskLevel\":\"MEDIUM\",\"reasoning\":\"Mixed signals\"}\n```");

        RiskEvaluationResponse response = riskAnalysisService.evaluateRisk(request);

        assertEquals("RELIANCE", response.getSymbol());
        assertEquals(RiskLevel.MEDIUM, response.getRiskLevel());
    }

    @Test
    @DisplayName("evaluateRisk - JSON wrapped in ``` code block (no language) → cleaned and parsed")
    void evaluateRisk_JsonInGenericCodeBlock_ShouldCleanAndParse() {
        when(newsSourceService.getNewsForSymbol("RELIANCE")).thenReturn(List.of("News headline"));
        when(newsRiskAgent.evaluateRisk(anyString())).thenReturn(
                "```\n{\"symbol\":\"RELIANCE\",\"riskLevel\":\"HIGH\",\"reasoning\":\"Lawsuit filed\"}\n```");

        RiskEvaluationResponse response = riskAnalysisService.evaluateRisk(request);

        assertEquals(RiskLevel.HIGH, response.getRiskLevel());
    }

    @Test
    @DisplayName("evaluateRisk - AI response missing symbol → defaults to request symbol")
    void evaluateRisk_MissingSymbol_ShouldDefaultToRequestSymbol() {
        when(newsSourceService.getNewsForSymbol("RELIANCE")).thenReturn(List.of("News"));
        when(newsRiskAgent.evaluateRisk(anyString())).thenReturn(
                "{\"riskLevel\":\"LOW\",\"reasoning\":\"All clear\"}");

        RiskEvaluationResponse response = riskAnalysisService.evaluateRisk(request);

        assertEquals("RELIANCE", response.getSymbol());
    }

    @Test
    @DisplayName("evaluateRisk - AI throws exception → returns fallback HIGH risk response")
    void evaluateRisk_AiException_ShouldReturnFallbackHighRisk() {
        when(newsSourceService.getNewsForSymbol("RELIANCE")).thenReturn(List.of("News"));
        when(newsRiskAgent.evaluateRisk(anyString())).thenThrow(new RuntimeException("AI service down"));

        RiskEvaluationResponse response = riskAnalysisService.evaluateRisk(request);

        assertEquals("RELIANCE", response.getSymbol());
        assertEquals(RiskLevel.HIGH, response.getRiskLevel());
        assertTrue(response.getReasoning().contains("AI Evaluation Failed"));
    }

    @Test
    @DisplayName("evaluateRisk - invalid JSON from AI → returns fallback HIGH risk response")
    void evaluateRisk_InvalidJson_ShouldReturnFallbackHighRisk() {
        when(newsSourceService.getNewsForSymbol("RELIANCE")).thenReturn(List.of("News"));
        when(newsRiskAgent.evaluateRisk(anyString())).thenReturn("This is not valid JSON at all");

        RiskEvaluationResponse response = riskAnalysisService.evaluateRisk(request);

        assertEquals("RELIANCE", response.getSymbol());
        assertEquals(RiskLevel.HIGH, response.getRiskLevel());
        assertTrue(response.getReasoning().contains("AI Evaluation Failed"));
    }
}

package in.winvestco.risk_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.risk_service.dto.RiskEvaluationRequest;
import in.winvestco.risk_service.dto.RiskEvaluationResponse;
import in.winvestco.risk_service.dto.RiskLevel;
import in.winvestco.risk_service.service.RiskAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskController.class)
@AutoConfigureMockMvc(addFilters = false)
class RiskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskAnalysisService riskAnalysisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/risk/evaluate-news - Should return risk evaluation")
    void evaluateNewsRisk_ShouldReturnRiskEvaluation() throws Exception {
        RiskEvaluationResponse response = RiskEvaluationResponse.builder()
                .symbol("RELIANCE")
                .riskLevel(RiskLevel.LOW)
                .reasoning("Positive news")
                .build();

        when(riskAnalysisService.evaluateRisk(any(RiskEvaluationRequest.class))).thenReturn(response);

        RiskEvaluationRequest request = RiskEvaluationRequest.builder()
                .symbol("RELIANCE")
                .build();

        mockMvc.perform(post("/api/v1/risk/evaluate-news")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("RELIANCE"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.reasoning").value("Positive news"));
    }
}

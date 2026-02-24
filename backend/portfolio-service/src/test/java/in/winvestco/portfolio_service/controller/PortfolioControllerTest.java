package in.winvestco.portfolio_service.controller;

import in.winvestco.portfolio_service.dto.AddHoldingRequest;
import in.winvestco.portfolio_service.dto.CreatePortfolioRequest;
import in.winvestco.portfolio_service.dto.HoldingDTO;
import in.winvestco.portfolio_service.dto.PortfolioDTO;
import in.winvestco.portfolio_service.dto.UpdateHoldingRequest;
import in.winvestco.portfolio_service.dto.UpdatePortfolioRequest;
import in.winvestco.portfolio_service.service.HoldingService;
import in.winvestco.portfolio_service.service.PortfolioService;
import in.winvestco.portfolio_service.service.PortfolioWebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.enums.PortfolioStatus;
import in.winvestco.common.enums.PortfolioType;
import in.winvestco.common.util.LoggingUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PortfolioController.class, excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@TestPropertySource(properties = {
        "spring.jpa.open-in-view=false"
})
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private HoldingService holdingService;

    @MockBean
    private PortfolioWebSocketService webSocketService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private LoggingUtils loggingUtils;

    private PortfolioDTO samplePortfolio() {
        return PortfolioDTO.builder()
                .id(1L)
                .userId(1L)
                .name("My Portfolio")
                .status(PortfolioStatus.ACTIVE)
                .portfolioType(PortfolioType.MAIN)
                .isDefault(true)
                .totalInvested(new BigDecimal("50000"))
                .currentValue(new BigDecimal("55000"))
                .profitLoss(new BigDecimal("5000"))
                .build();
    }

    private HoldingDTO sampleHolding() {
        return HoldingDTO.builder()
                .id(10L)
                .portfolioId(1L)
                .symbol("RELIANCE")
                .companyName("Reliance Industries")
                .quantity(new BigDecimal("100"))
                .averagePrice(new BigDecimal("2500"))
                .totalInvested(new BigDecimal("250000"))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/portfolios should return default portfolio")
    void getDefaultPortfolio_shouldReturn200() throws Exception {
        when(portfolioService.getPortfolioByUserId(1L)).thenReturn(samplePortfolio());

        mockMvc.perform(get("/api/v1/portfolios")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Portfolio"));
    }

    @Test
    @DisplayName("GET /api/v1/portfolios/all should return all portfolios")
    void getAllPortfolios_shouldReturn200() throws Exception {
        when(portfolioService.getAllPortfoliosByUserId(1L)).thenReturn(List.of(samplePortfolio()));

        mockMvc.perform(get("/api/v1/portfolios/all")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("My Portfolio"));
    }

    @Test
    @DisplayName("POST /api/v1/portfolios should create portfolio")
    void createPortfolio_shouldReturn201() throws Exception {
        when(portfolioService.createPortfolio(eq(1L), any(CreatePortfolioRequest.class)))
                .thenReturn(samplePortfolio());

        CreatePortfolioRequest request = CreatePortfolioRequest.builder()
                .name("New Portfolio")
                .portfolioType(PortfolioType.MAIN)
                .build();

        mockMvc.perform(post("/api/v1/portfolios")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/v1/portfolios/{id} should return portfolio by ID")
    void getPortfolioById_shouldReturn200() throws Exception {
        when(portfolioService.getPortfolioById(1L, 1L)).thenReturn(samplePortfolio());

        mockMvc.perform(get("/api/v1/portfolios/1")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/portfolios should update portfolio")
    void updatePortfolio_shouldReturn200() throws Exception {
        when(portfolioService.updatePortfolio(eq(1L), any(UpdatePortfolioRequest.class)))
                .thenReturn(samplePortfolio());

        UpdatePortfolioRequest request = UpdatePortfolioRequest.builder()
                .name("Updated Name")
                .description("Updated Desc")
                .build();

        mockMvc.perform(put("/api/v1/portfolios")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/portfolios/holdings should return holdings list")
    void getHoldings_shouldReturn200() throws Exception {
        when(holdingService.getHoldingsByUserId(1L)).thenReturn(List.of(sampleHolding()));

        mockMvc.perform(get("/api/v1/portfolios/holdings")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("RELIANCE"));
    }

    @Test
    @DisplayName("POST /api/v1/portfolios/holdings should add holding")
    void addHolding_shouldReturn201() throws Exception {
        when(holdingService.addHolding(eq(1L), any(AddHoldingRequest.class))).thenReturn(sampleHolding());

        AddHoldingRequest request = AddHoldingRequest.builder()
                .symbol("TCS")
                .quantity(new BigDecimal("50"))
                .averagePrice(new BigDecimal("3500"))
                .build();

        mockMvc.perform(post("/api/v1/portfolios/holdings")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/v1/portfolios/holdings/{symbol} should return holding")
    void getHoldingBySymbol_shouldReturn200() throws Exception {
        when(holdingService.getHoldingBySymbol(1L, "RELIANCE")).thenReturn(sampleHolding());

        mockMvc.perform(get("/api/v1/portfolios/holdings/RELIANCE")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("RELIANCE"));
    }

    @Test
    @DisplayName("PUT /api/v1/portfolios/holdings/{id} should update holding")
    void updateHolding_shouldReturn200() throws Exception {
        when(holdingService.updateHolding(eq(1L), eq(10L), any(UpdateHoldingRequest.class)))
                .thenReturn(sampleHolding());

        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .quantity(new BigDecimal("200"))
                .averagePrice(new BigDecimal("2600"))
                .build();

        mockMvc.perform(put("/api/v1/portfolios/holdings/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/portfolios/holdings/{id} should return 204")
    void removeHolding_shouldReturn204() throws Exception {
        doNothing().when(holdingService).removeHolding(1L, 10L);

        mockMvc.perform(delete("/api/v1/portfolios/holdings/10")
                .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/portfolios/archive should archive portfolio")
    void archivePortfolio_shouldReturn200() throws Exception {
        when(portfolioService.archivePortfolio(1L)).thenReturn(samplePortfolio());

        mockMvc.perform(post("/api/v1/portfolios/archive")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/portfolios/reactivate should reactivate portfolio")
    void reactivatePortfolio_shouldReturn200() throws Exception {
        when(portfolioService.reactivatePortfolio(1L)).thenReturn(samplePortfolio());

        mockMvc.perform(post("/api/v1/portfolios/reactivate")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request should return 401")
    void unauthenticatedRequest_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isUnauthorized());
    }
}

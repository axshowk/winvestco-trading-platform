package in.winvestco.portfolio_service.service;

import in.winvestco.common.enums.PortfolioStatus;
import in.winvestco.portfolio_service.dto.PortfolioDTO;
import in.winvestco.portfolio_service.dto.UpdatePortfolioRequest;
import in.winvestco.portfolio_service.mapper.PortfolioMapper;
import in.winvestco.portfolio_service.model.Portfolio;
import in.winvestco.portfolio_service.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioMapper portfolioMapper;

    @InjectMocks
    private PortfolioService portfolioService;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testPortfolio = Portfolio.builder()
                .id(1L)
                .userId(1L)
                .name("My Portfolio")
                .status(PortfolioStatus.ACTIVE)
                .totalInvested(new BigDecimal("10000"))
                .currentValue(new BigDecimal("12000"))
                .build();
    }

    @Test
    void createPortfolioForUser_WhenNotExists_ShouldCreateNew() {
        when(portfolioRepository.existsByUserId(anyLong())).thenReturn(false);
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        Portfolio result = portfolioService.createPortfolioForUser(1L, "test@example.com");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    void getPortfolioByUserId_ShouldReturnEnrichedDTO() {
        when(portfolioRepository.findByUserIdWithHoldings(anyLong())).thenReturn(Optional.of(testPortfolio));
        PortfolioDTO dto = new PortfolioDTO();
        dto.setTotalInvested(new BigDecimal("10000"));
        dto.setCurrentValue(new BigDecimal("12000"));
        when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(dto);

        PortfolioDTO result = portfolioService.getPortfolioByUserId(1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("2000"), result.getProfitLoss());
        assertEquals(new BigDecimal("20.0000"), result.getProfitLossPercentage());
    }

    @Test
    void updatePortfolio_ShouldUpdateDetails() {
        when(portfolioRepository.findByUserId(anyLong())).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);
        when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(new PortfolioDTO());

        UpdatePortfolioRequest request = new UpdatePortfolioRequest();
        request.setName("New Name");
        request.setDescription("New Desc");

        PortfolioDTO result = portfolioService.updatePortfolio(1L, request);

        assertNotNull(result);
        assertEquals("New Name", testPortfolio.getName());
        assertEquals("New Desc", testPortfolio.getDescription());
    }
}

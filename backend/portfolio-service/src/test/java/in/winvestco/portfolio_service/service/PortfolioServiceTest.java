package in.winvestco.portfolio_service.service;

import in.winvestco.common.enums.PortfolioStatus;
import in.winvestco.common.enums.PortfolioType;
import in.winvestco.portfolio_service.client.MarketServiceClient;
import in.winvestco.portfolio_service.dto.CreatePortfolioRequest;
import in.winvestco.portfolio_service.dto.HoldingDTO;
import in.winvestco.portfolio_service.dto.PortfolioDTO;
import in.winvestco.portfolio_service.dto.StockQuoteDTO;
import in.winvestco.portfolio_service.dto.UpdatePortfolioRequest;
import in.winvestco.portfolio_service.exception.PortfolioNotFoundException;
import in.winvestco.portfolio_service.mapper.PortfolioMapper;
import in.winvestco.portfolio_service.model.Holding;
import in.winvestco.portfolio_service.model.Portfolio;
import in.winvestco.portfolio_service.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    @Mock
    private MarketServiceClient marketServiceClient;

    @InjectMocks
    private PortfolioService portfolioService;

    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testPortfolio = Portfolio.builder()
                .id(1L)
                .userId(1L)
                .name("My Portfolio")
                .description("Test portfolio")
                .status(PortfolioStatus.ACTIVE)
                .isDefault(true)
                .totalInvested(new BigDecimal("10000"))
                .currentValue(new BigDecimal("12000"))
                .holdings(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("createPortfolioForUser")
    class CreatePortfolioForUser {

        @Test
        void whenNotExists_shouldCreateNew() {
            when(portfolioRepository.existsByUserId(1L)).thenReturn(false);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

            Portfolio result = portfolioService.createPortfolioForUser(1L, "test@example.com");

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        void whenAlreadyExists_shouldReturnExisting() {
            when(portfolioRepository.existsByUserId(1L)).thenReturn(true);
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));

            Portfolio result = portfolioService.createPortfolioForUser(1L, "test@example.com");

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        void whenAlreadyExistsButNotFound_shouldReturnNull() {
            when(portfolioRepository.existsByUserId(1L)).thenReturn(true);
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.empty());

            Portfolio result = portfolioService.createPortfolioForUser(1L, "test@example.com");

            assertNull(result);
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("createPortfolio")
    class CreatePortfolio {

        @Test
        void firstPortfolio_shouldAutoSetDefault() {
            when(portfolioRepository.existsByUserId(1L)).thenReturn(false);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);
            when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(new PortfolioDTO());

            CreatePortfolioRequest request = CreatePortfolioRequest.builder()
                    .name("Test")
                    .portfolioType(PortfolioType.MAIN)
                    .isDefault(false)
                    .build();

            PortfolioDTO result = portfolioService.createPortfolio(1L, request);

            assertNotNull(result);
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        void newDefaultPortfolio_shouldUnsetPreviousDefault() {
            Portfolio oldDefault = Portfolio.builder().id(2L).userId(1L).isDefault(true).build();
            when(portfolioRepository.existsByUserId(1L)).thenReturn(true);
            when(portfolioRepository.findDefaultByUserId(1L)).thenReturn(Optional.of(oldDefault));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);
            when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(new PortfolioDTO());

            CreatePortfolioRequest request = CreatePortfolioRequest.builder()
                    .name("New Default")
                    .portfolioType(PortfolioType.MAIN)
                    .isDefault(true)
                    .build();

            portfolioService.createPortfolio(1L, request);

            assertFalse(oldDefault.getIsDefault());
            verify(portfolioRepository, times(2)).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("getPortfolioByUserId")
    class GetPortfolioByUserId {

        @Test
        void shouldReturnEnrichedDTO() {
            when(portfolioRepository.findDefaultByUserIdWithHoldings(1L)).thenReturn(Optional.of(testPortfolio));
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
        void whenNoDefault_shouldFallbackToFirstPortfolio() {
            when(portfolioRepository.findDefaultByUserIdWithHoldings(1L)).thenReturn(Optional.empty());
            when(portfolioRepository.findAllByUserId(1L)).thenReturn(List.of(testPortfolio));
            PortfolioDTO dto = new PortfolioDTO();
            dto.setTotalInvested(new BigDecimal("10000"));
            dto.setCurrentValue(new BigDecimal("12000"));
            when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(dto);

            PortfolioDTO result = portfolioService.getPortfolioByUserId(1L);

            assertNotNull(result);
        }

        @Test
        void whenNotFound_shouldThrowException() {
            when(portfolioRepository.findDefaultByUserIdWithHoldings(1L)).thenReturn(Optional.empty());
            when(portfolioRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());

            assertThrows(PortfolioNotFoundException.class,
                    () -> portfolioService.getPortfolioByUserId(1L));
        }
    }

    @Nested
    @DisplayName("getAllPortfoliosByUserId")
    class GetAllPortfoliosByUserId {

        @Test
        void withPortfolios_shouldReturnList() {
            when(portfolioRepository.findAllByUserId(1L)).thenReturn(List.of(testPortfolio));
            PortfolioDTO dto = new PortfolioDTO();
            dto.setTotalInvested(BigDecimal.ZERO);
            dto.setCurrentValue(BigDecimal.ZERO);
            when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(dto);

            List<PortfolioDTO> result = portfolioService.getAllPortfoliosByUserId(1L);

            assertEquals(1, result.size());
        }

        @Test
        void withNoPortfolios_shouldReturnEmptyList() {
            when(portfolioRepository.findAllByUserId(1L)).thenReturn(Collections.emptyList());

            List<PortfolioDTO> result = portfolioService.getAllPortfoliosByUserId(1L);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getPortfolioById")
    class GetPortfolioById {

        @Test
        void withValidOwnership_shouldReturnDTO() {
            when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testPortfolio));
            PortfolioDTO dto = new PortfolioDTO();
            dto.setTotalInvested(BigDecimal.ZERO);
            dto.setCurrentValue(BigDecimal.ZERO);
            when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(dto);

            PortfolioDTO result = portfolioService.getPortfolioById(1L, 1L);

            assertNotNull(result);
        }

        @Test
        void withInvalidOwnership_shouldThrowException() {
            when(portfolioRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class,
                    () -> portfolioService.getPortfolioById(1L, 2L));
        }
    }

    @Test
    @DisplayName("updatePortfolio should update name and description")
    void updatePortfolio_shouldUpdateDetails() {
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
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

    @Test
    @DisplayName("updatePortfolioTotals should sum holding totalInvested")
    void updatePortfolioTotals_shouldCalculateSum() {
        Holding h1 = Holding.builder().totalInvested(new BigDecimal("5000")).build();
        Holding h2 = Holding.builder().totalInvested(new BigDecimal("3000")).build();
        testPortfolio.setHoldings(List.of(h1, h2));

        when(portfolioRepository.findByIdWithHoldings(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);

        portfolioService.updatePortfolioTotals(1L);

        assertEquals(new BigDecimal("8000"), testPortfolio.getTotalInvested());
        verify(portfolioRepository).save(testPortfolio);
    }

    @Test
    @DisplayName("archivePortfolio should set status to ARCHIVED")
    void archivePortfolio_shouldSetArchived() {
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);
        when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(new PortfolioDTO());

        portfolioService.archivePortfolio(1L);

        assertEquals(PortfolioStatus.ARCHIVED, testPortfolio.getStatus());
    }

    @Test
    @DisplayName("reactivatePortfolio should set status to ACTIVE")
    void reactivatePortfolio_shouldSetActive() {
        testPortfolio.setStatus(PortfolioStatus.ARCHIVED);
        when(portfolioRepository.findDefaultByUserId(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(testPortfolio);
        when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(new PortfolioDTO());

        portfolioService.reactivatePortfolio(1L);

        assertEquals(PortfolioStatus.ACTIVE, testPortfolio.getStatus());
    }

    @Nested
    @DisplayName("setDefaultPortfolio")
    class SetDefaultPortfolio {

        @Test
        void shouldUnsetOldDefaultAndSetNew() {
            Portfolio oldDefault = Portfolio.builder().id(2L).userId(1L).isDefault(true).build();
            Portfolio newDefault = Portfolio.builder().id(3L).userId(1L).isDefault(false).build();

            when(portfolioRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(newDefault));
            when(portfolioRepository.findDefaultByUserId(1L)).thenReturn(Optional.of(oldDefault));
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArgument(0));
            when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(new PortfolioDTO());

            portfolioService.setDefaultPortfolio(1L, 3L);

            assertFalse(oldDefault.getIsDefault());
            assertTrue(newDefault.getIsDefault());
        }
    }

    @Test
    @DisplayName("hasPortfolio should delegate to repository")
    void hasPortfolio_shouldReturnResult() {
        when(portfolioRepository.existsByUserId(1L)).thenReturn(true);
        assertTrue(portfolioService.hasPortfolio(1L));

        when(portfolioRepository.existsByUserId(2L)).thenReturn(false);
        assertFalse(portfolioService.hasPortfolio(2L));
    }

    @Test
    @DisplayName("getPortfolioEntityByUserId should return entity or throw")
    void getPortfolioEntityByUserId_shouldReturnOrThrow() {
        when(portfolioRepository.findDefaultByUserId(1L)).thenReturn(Optional.of(testPortfolio));
        assertEquals(testPortfolio, portfolioService.getPortfolioEntityByUserId(1L));

        when(portfolioRepository.findDefaultByUserId(2L)).thenReturn(Optional.empty());
        assertThrows(PortfolioNotFoundException.class,
                () -> portfolioService.getPortfolioEntityByUserId(2L));
    }

    @Test
    @DisplayName("enrichWithMarketData should populate holding market values")
    void getPortfolioByUserId_withHoldings_shouldEnrichWithMarketData() {
        Holding holding = Holding.builder()
                .symbol("RELIANCE")
                .quantity(new BigDecimal("10"))
                .averagePrice(new BigDecimal("2000"))
                .totalInvested(new BigDecimal("20000"))
                .build();
        holding.setPortfolio(testPortfolio);
        testPortfolio.setHoldings(List.of(holding));

        HoldingDTO holdingDTO = HoldingDTO.builder()
                .symbol("RELIANCE")
                .quantity(new BigDecimal("10"))
                .totalInvested(new BigDecimal("20000"))
                .build();

        PortfolioDTO dto = new PortfolioDTO();
        dto.setHoldings(List.of(holdingDTO));
        dto.setTotalInvested(new BigDecimal("20000"));

        when(portfolioRepository.findDefaultByUserIdWithHoldings(1L)).thenReturn(Optional.of(testPortfolio));
        when(portfolioMapper.toDTO(any(Portfolio.class))).thenReturn(dto);

        StockQuoteDTO quote = StockQuoteDTO.builder()
                .symbol("RELIANCE")
                .lastPrice(new BigDecimal("2500"))
                .change(new BigDecimal("50"))
                .pChange(new BigDecimal("2.04"))
                .build();
        when(marketServiceClient.getBulkQuotes(anyList())).thenReturn(List.of(quote));

        PortfolioDTO result = portfolioService.getPortfolioByUserId(1L);

        assertNotNull(result);
        HoldingDTO enrichedHolding = result.getHoldings().get(0);
        assertEquals(new BigDecimal("2500"), enrichedHolding.getCurrentPrice());
        assertEquals(new BigDecimal("25000"), enrichedHolding.getCurrentValue());
        assertEquals(new BigDecimal("5000"), enrichedHolding.getProfitLoss());
    }
}

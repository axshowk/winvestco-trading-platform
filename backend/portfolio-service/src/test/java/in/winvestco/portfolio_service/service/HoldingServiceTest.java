package in.winvestco.portfolio_service.service;

import in.winvestco.portfolio_service.dto.AddHoldingRequest;
import in.winvestco.portfolio_service.dto.HoldingDTO;
import in.winvestco.portfolio_service.dto.UpdateHoldingRequest;
import in.winvestco.portfolio_service.exception.HoldingNotFoundException;
import in.winvestco.portfolio_service.exception.PortfolioNotFoundException;
import in.winvestco.portfolio_service.mapper.PortfolioMapper;
import in.winvestco.portfolio_service.model.Holding;
import in.winvestco.portfolio_service.model.Portfolio;
import in.winvestco.portfolio_service.repository.HoldingRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private PortfolioMapper portfolioMapper;

    @InjectMocks
    private HoldingService holdingService;

    private Portfolio testPortfolio;
    private Holding testHolding;
    private HoldingDTO testHoldingDTO;

    @BeforeEach
    void setUp() {
        testPortfolio = Portfolio.builder()
                .id(1L)
                .userId(1L)
                .name("My Portfolio")
                .build();

        testHolding = Holding.builder()
                .id(10L)
                .portfolio(testPortfolio)
                .symbol("RELIANCE")
                .companyName("Reliance Industries")
                .exchange("NSE")
                .quantity(new BigDecimal("100"))
                .averagePrice(new BigDecimal("2500"))
                .totalInvested(new BigDecimal("250000"))
                .build();

        testHoldingDTO = HoldingDTO.builder()
                .id(10L)
                .portfolioId(1L)
                .symbol("RELIANCE")
                .quantity(new BigDecimal("100"))
                .averagePrice(new BigDecimal("2500"))
                .totalInvested(new BigDecimal("250000"))
                .build();
    }

    @Test
    @DisplayName("getHoldingsByUserId should return mapped DTOs")
    void getHoldingsByUserId_shouldReturnList() {
        when(holdingRepository.findByUserId(1L)).thenReturn(List.of(testHolding));
        when(portfolioMapper.toHoldingDTOList(anyList())).thenReturn(List.of(testHoldingDTO));

        List<HoldingDTO> result = holdingService.getHoldingsByUserId(1L);

        assertEquals(1, result.size());
        assertEquals("RELIANCE", result.get(0).getSymbol());
    }

    @Test
    @DisplayName("getHoldingsByPortfolioId should return mapped DTOs")
    void getHoldingsByPortfolioId_shouldReturnList() {
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(testHolding));
        when(portfolioMapper.toHoldingDTOList(anyList())).thenReturn(List.of(testHoldingDTO));

        List<HoldingDTO> result = holdingService.getHoldingsByPortfolioId(1L);

        assertEquals(1, result.size());
    }

    @Nested
    @DisplayName("addHolding")
    class AddHolding {

        @Test
        void success_shouldCreateNewHolding() {
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
            when(holdingRepository.existsByPortfolioIdAndSymbol(1L, "TCS")).thenReturn(false);
            when(holdingRepository.save(any(Holding.class))).thenAnswer(i -> {
                Holding h = i.getArgument(0);
                h.setId(11L);
                return h;
            });
            when(portfolioMapper.toDTO(any(Holding.class))).thenReturn(testHoldingDTO);

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .symbol("TCS")
                    .companyName("TCS Limited")
                    .quantity(new BigDecimal("50"))
                    .averagePrice(new BigDecimal("3500"))
                    .build();

            HoldingDTO result = holdingService.addHolding(1L, request);

            assertNotNull(result);
            verify(holdingRepository).save(any(Holding.class));
            verify(portfolioService).updatePortfolioTotals(1L);
        }

        @Test
        void duplicateSymbol_shouldThrowException() {
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
            when(holdingRepository.existsByPortfolioIdAndSymbol(1L, "RELIANCE")).thenReturn(true);

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .symbol("RELIANCE")
                    .quantity(new BigDecimal("10"))
                    .averagePrice(new BigDecimal("2500"))
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> holdingService.addHolding(1L, request));
        }

        @Test
        void noPortfolio_shouldThrowException() {
            when(portfolioRepository.findByUserId(99L)).thenReturn(Optional.empty());

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .symbol("TCS")
                    .quantity(new BigDecimal("10"))
                    .averagePrice(new BigDecimal("3500"))
                    .build();

            assertThrows(PortfolioNotFoundException.class,
                    () -> holdingService.addHolding(99L, request));
        }
    }

    @Nested
    @DisplayName("updateHolding")
    class UpdateHolding {

        @Test
        void success_shouldUpdateQuantityAndPrice() {
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
            when(holdingRepository.findById(10L)).thenReturn(Optional.of(testHolding));
            when(holdingRepository.save(any(Holding.class))).thenReturn(testHolding);
            when(portfolioMapper.toDTO(any(Holding.class))).thenReturn(testHoldingDTO);

            UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                    .quantity(new BigDecimal("200"))
                    .averagePrice(new BigDecimal("2600"))
                    .build();

            HoldingDTO result = holdingService.updateHolding(1L, 10L, request);

            assertNotNull(result);
            assertEquals(new BigDecimal("200"), testHolding.getQuantity());
            assertEquals(new BigDecimal("2600"), testHolding.getAveragePrice());
            verify(portfolioService).updatePortfolioTotals(1L);
        }

        @Test
        void wrongPortfolio_shouldThrowException() {
            Portfolio otherPortfolio = Portfolio.builder().id(99L).userId(1L).build();
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(otherPortfolio));
            when(holdingRepository.findById(10L)).thenReturn(Optional.of(testHolding));

            UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                    .quantity(new BigDecimal("200"))
                    .averagePrice(new BigDecimal("2600"))
                    .build();

            assertThrows(HoldingNotFoundException.class,
                    () -> holdingService.updateHolding(1L, 10L, request));
        }
    }

    @Test
    @DisplayName("addToHolding should calculate weighted average price")
    void addToHolding_shouldCalculateWeightedAverage() {
        // Original: 100 shares @ 2500 = 250000
        // Adding: 50 shares @ 3000 = 150000
        // New: 150 shares, average = (250000 + 150000) / 150 = 2666.6667
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
        when(holdingRepository.findByPortfolioIdAndSymbol(1L, "RELIANCE")).thenReturn(Optional.of(testHolding));
        when(holdingRepository.save(any(Holding.class))).thenReturn(testHolding);
        when(portfolioMapper.toDTO(any(Holding.class))).thenReturn(testHoldingDTO);

        holdingService.addToHolding(1L, "RELIANCE", new BigDecimal("50"), new BigDecimal("3000"));

        assertEquals(new BigDecimal("150"), testHolding.getQuantity());
        assertEquals(new BigDecimal("2666.6667"), testHolding.getAveragePrice());
        verify(portfolioService).updatePortfolioTotals(1L);
    }

    @Nested
    @DisplayName("reduceHolding")
    class ReduceHolding {

        @Test
        void partialSell_shouldReduceQuantity() {
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
            when(holdingRepository.findByPortfolioIdAndSymbol(1L, "RELIANCE")).thenReturn(Optional.of(testHolding));
            when(holdingRepository.save(any(Holding.class))).thenReturn(testHolding);
            when(portfolioMapper.toDTO(any(Holding.class))).thenReturn(testHoldingDTO);

            HoldingDTO result = holdingService.reduceHolding(1L, "RELIANCE", new BigDecimal("30"));

            assertNotNull(result);
            assertEquals(new BigDecimal("70"), testHolding.getQuantity());
            verify(portfolioService).updatePortfolioTotals(1L);
        }

        @Test
        void fullSell_shouldDeleteHolding() {
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
            when(holdingRepository.findByPortfolioIdAndSymbol(1L, "RELIANCE")).thenReturn(Optional.of(testHolding));

            HoldingDTO result = holdingService.reduceHolding(1L, "RELIANCE", new BigDecimal("100"));

            assertNull(result);
            verify(holdingRepository).delete(testHolding);
            verify(portfolioService).updatePortfolioTotals(1L);
        }

        @Test
        void overSell_shouldThrowException() {
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
            when(holdingRepository.findByPortfolioIdAndSymbol(1L, "RELIANCE")).thenReturn(Optional.of(testHolding));

            assertThrows(IllegalArgumentException.class,
                    () -> holdingService.reduceHolding(1L, "RELIANCE", new BigDecimal("200")));
        }
    }

    @Test
    @DisplayName("removeHolding should delete and update totals")
    void removeHolding_shouldDeleteAndUpdateTotals() {
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
        when(holdingRepository.findById(10L)).thenReturn(Optional.of(testHolding));

        holdingService.removeHolding(1L, 10L);

        verify(holdingRepository).delete(testHolding);
        verify(portfolioService).updatePortfolioTotals(1L);
    }

    @Nested
    @DisplayName("getHoldingBySymbol")
    class GetHoldingBySymbol {

        @Test
        void found_shouldReturnDTO() {
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
            when(holdingRepository.findByPortfolioIdAndSymbol(1L, "RELIANCE")).thenReturn(Optional.of(testHolding));
            when(portfolioMapper.toDTO(any(Holding.class))).thenReturn(testHoldingDTO);

            HoldingDTO result = holdingService.getHoldingBySymbol(1L, "reliance");

            assertNotNull(result);
            assertEquals("RELIANCE", result.getSymbol());
        }

        @Test
        void notFound_shouldThrowException() {
            when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(testPortfolio));
            when(holdingRepository.findByPortfolioIdAndSymbol(1L, "UNKNOWN")).thenReturn(Optional.empty());

            assertThrows(HoldingNotFoundException.class,
                    () -> holdingService.getHoldingBySymbol(1L, "UNKNOWN"));
        }
    }
}

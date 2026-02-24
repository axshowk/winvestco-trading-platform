package in.winvestco.portfolio_service.repository;

import in.winvestco.common.enums.PortfolioStatus;
import in.winvestco.common.enums.PortfolioType;
import in.winvestco.portfolio_service.model.Holding;
import in.winvestco.portfolio_service.model.Portfolio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class HoldingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HoldingRepository holdingRepository;

    private Portfolio portfolio;
    private Holding holdingReliance;
    private Holding holdingTCS;

    @BeforeEach
    void setUp() {
        portfolio = Portfolio.builder()
                .userId(1L)
                .name("Test Portfolio")
                .status(PortfolioStatus.ACTIVE)
                .portfolioType(PortfolioType.MAIN)
                .isDefault(true)
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .build();
        entityManager.persistAndFlush(portfolio);

        holdingReliance = Holding.builder()
                .portfolio(portfolio)
                .symbol("RELIANCE")
                .companyName("Reliance Industries")
                .exchange("NSE")
                .quantity(new BigDecimal("100"))
                .averagePrice(new BigDecimal("2500"))
                .totalInvested(new BigDecimal("250000"))
                .build();
        entityManager.persistAndFlush(holdingReliance);

        holdingTCS = Holding.builder()
                .portfolio(portfolio)
                .symbol("TCS")
                .companyName("TCS Limited")
                .exchange("NSE")
                .quantity(new BigDecimal("50"))
                .averagePrice(new BigDecimal("3500"))
                .totalInvested(new BigDecimal("175000"))
                .build();
        entityManager.persistAndFlush(holdingTCS);
    }

    @Test
    @DisplayName("findByPortfolioId should return all holdings for portfolio")
    void findByPortfolioId_shouldReturnAll() {
        List<Holding> result = holdingRepository.findByPortfolioId(portfolio.getId());

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findByPortfolioIdAndSymbol should return matching holding")
    void findByPortfolioIdAndSymbol_shouldReturnMatch() {
        Optional<Holding> result = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), "RELIANCE");

        assertTrue(result.isPresent());
        assertEquals("RELIANCE", result.get().getSymbol());
        assertEquals(0, result.get().getQuantity().compareTo(new BigDecimal("100")));
    }

    @Test
    @DisplayName("findByPortfolioIdAndSymbol with no match should return empty")
    void findByPortfolioIdAndSymbol_noMatch_shouldReturnEmpty() {
        Optional<Holding> result = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), "INFY");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("existsByPortfolioIdAndSymbol should return correct boolean")
    void existsByPortfolioIdAndSymbol_shouldReturnCorrectly() {
        assertTrue(holdingRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), "RELIANCE"));
        assertFalse(holdingRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), "INFY"));
    }

    @Test
    @DisplayName("findBySymbol should find across all portfolios")
    void findBySymbol_shouldFindAcrossPortfolios() {
        // Create a second portfolio with RELIANCE holding
        Portfolio portfolio2 = Portfolio.builder()
                .userId(2L)
                .name("Other")
                .status(PortfolioStatus.ACTIVE)
                .portfolioType(PortfolioType.MAIN)
                .isDefault(true)
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .build();
        entityManager.persistAndFlush(portfolio2);

        Holding otherReliance = Holding.builder()
                .portfolio(portfolio2)
                .symbol("RELIANCE")
                .companyName("Reliance Industries")
                .quantity(new BigDecimal("20"))
                .averagePrice(new BigDecimal("2600"))
                .totalInvested(new BigDecimal("52000"))
                .build();
        entityManager.persistAndFlush(otherReliance);

        List<Holding> result = holdingRepository.findBySymbol("RELIANCE");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("countByPortfolioId should count correctly")
    void countByPortfolioId_shouldCount() {
        long count = holdingRepository.countByPortfolioId(portfolio.getId());

        assertEquals(2, count);
    }

    @Test
    @DisplayName("deleteByPortfolioId should remove all holdings")
    void deleteByPortfolioId_shouldRemoveAll() {
        holdingRepository.deleteByPortfolioId(portfolio.getId());
        entityManager.flush();

        List<Holding> remaining = holdingRepository.findByPortfolioId(portfolio.getId());
        assertTrue(remaining.isEmpty());
    }

    @Test
    @DisplayName("findByUserId should find holdings via portfolio userId")
    void findByUserId_shouldFindViaPortfolio() {
        List<Holding> result = holdingRepository.findByUserId(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findByUserId with no holdings should return empty")
    void findByUserId_noHoldings_shouldReturnEmpty() {
        List<Holding> result = holdingRepository.findByUserId(999L);

        assertTrue(result.isEmpty());
    }
}

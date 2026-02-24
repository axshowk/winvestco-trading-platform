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
class PortfolioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Portfolio defaultPortfolio;
    private Portfolio secondPortfolio;

    @BeforeEach
    void setUp() {
        defaultPortfolio = Portfolio.builder()
                .userId(1L)
                .name("Default Portfolio")
                .description("My default")
                .status(PortfolioStatus.ACTIVE)
                .portfolioType(PortfolioType.MAIN)
                .isDefault(true)
                .totalInvested(new BigDecimal("50000"))
                .currentValue(new BigDecimal("55000"))
                .build();
        entityManager.persistAndFlush(defaultPortfolio);

        secondPortfolio = Portfolio.builder()
                .userId(1L)
                .name("Side Portfolio")
                .description("For experiments")
                .status(PortfolioStatus.ACTIVE)
                .portfolioType(PortfolioType.MAIN)
                .isDefault(false)
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .build();
        entityManager.persistAndFlush(secondPortfolio);
    }

    @Test
    @DisplayName("findDefaultByUserId should return default portfolio")
    void findDefaultByUserId_shouldReturnDefault() {
        Optional<Portfolio> result = portfolioRepository.findDefaultByUserId(1L);

        assertTrue(result.isPresent());
        assertTrue(result.get().getIsDefault());
        assertEquals("Default Portfolio", result.get().getName());
    }

    @Test
    @DisplayName("findDefaultByUserId with no default should return empty")
    void findDefaultByUserId_noDefault_shouldReturnEmpty() {
        Optional<Portfolio> result = portfolioRepository.findDefaultByUserId(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findAllByUserId should return all user portfolios")
    void findAllByUserId_shouldReturnAll() {
        List<Portfolio> result = portfolioRepository.findAllByUserId(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findAllByUserIdAndStatus should filter by status")
    void findAllByUserIdAndStatus_shouldFilter() {
        List<Portfolio> active = portfolioRepository.findAllByUserIdAndStatus(1L, PortfolioStatus.ACTIVE);
        assertEquals(2, active.size());

        List<Portfolio> archived = portfolioRepository.findAllByUserIdAndStatus(1L, PortfolioStatus.ARCHIVED);
        assertTrue(archived.isEmpty());
    }

    @Test
    @DisplayName("existsByUserId should return correct boolean")
    void existsByUserId_shouldReturnCorrectly() {
        assertTrue(portfolioRepository.existsByUserId(1L));
        assertFalse(portfolioRepository.existsByUserId(999L));
    }

    @Test
    @DisplayName("findByIdAndUserId should validate ownership")
    void findByIdAndUserId_shouldValidateOwnership() {
        Optional<Portfolio> result = portfolioRepository.findByIdAndUserId(defaultPortfolio.getId(), 1L);
        assertTrue(result.isPresent());

        // Wrong user
        Optional<Portfolio> wrongUser = portfolioRepository.findByIdAndUserId(defaultPortfolio.getId(), 999L);
        assertFalse(wrongUser.isPresent());
    }

    @Test
    @DisplayName("findDefaultByUserIdWithHoldings should eagerly load holdings")
    void findDefaultByUserIdWithHoldings_shouldLoadHoldings() {
        // Add a holding to the default portfolio via the entity method
        Holding holding = Holding.builder()
                .portfolio(defaultPortfolio)
                .symbol("RELIANCE")
                .companyName("Reliance Industries")
                .quantity(new BigDecimal("10"))
                .averagePrice(new BigDecimal("2500"))
                .totalInvested(new BigDecimal("25000"))
                .build();
        defaultPortfolio.addHolding(holding);
        entityManager.persistAndFlush(holding);
        entityManager.clear();

        Optional<Portfolio> result = portfolioRepository.findDefaultByUserIdWithHoldings(1L);

        assertTrue(result.isPresent());
        assertFalse(result.get().getHoldings().isEmpty());
        assertEquals("RELIANCE", result.get().getHoldings().get(0).getSymbol());
    }

    @Test
    @DisplayName("existsByUserIdAndIsDefaultTrue should check default flag")
    void existsByUserIdAndIsDefaultTrue_shouldCheck() {
        assertTrue(portfolioRepository.existsByUserIdAndIsDefaultTrue(1L));
        assertFalse(portfolioRepository.existsByUserIdAndIsDefaultTrue(999L));
    }

    @Test
    @DisplayName("findByUserId should return a portfolio")
    void findByUserId_shouldReturn() {
        // Use a unique userId to avoid non-unique result
        Portfolio uniquePortfolio = Portfolio.builder()
                .userId(999L)
                .name("Unique Portfolio")
                .status(PortfolioStatus.ACTIVE)
                .portfolioType(PortfolioType.MAIN)
                .isDefault(true)
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .build();
        entityManager.persistAndFlush(uniquePortfolio);

        Optional<Portfolio> result = portfolioRepository.findByUserId(999L);

        assertTrue(result.isPresent());
        assertEquals(999L, result.get().getUserId());
    }

    @Test
    @DisplayName("findByIdWithHoldings should eagerly load")
    void findByIdWithHoldings_shouldEagerLoad() {
        Holding holding = Holding.builder()
                .portfolio(defaultPortfolio)
                .symbol("TCS")
                .companyName("TCS Limited")
                .quantity(new BigDecimal("5"))
                .averagePrice(new BigDecimal("3500"))
                .totalInvested(new BigDecimal("17500"))
                .build();
        defaultPortfolio.addHolding(holding);
        entityManager.persistAndFlush(holding);
        entityManager.clear();

        Optional<Portfolio> result = portfolioRepository.findByIdWithHoldings(defaultPortfolio.getId());

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getHoldings().size());
    }
}

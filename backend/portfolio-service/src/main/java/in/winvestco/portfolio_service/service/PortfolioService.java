package in.winvestco.portfolio_service.service;

import in.winvestco.common.enums.PortfolioStatus;

import in.winvestco.portfolio_service.dto.CreatePortfolioRequest;
import in.winvestco.portfolio_service.dto.PortfolioDTO;
import in.winvestco.portfolio_service.dto.UpdatePortfolioRequest;
import in.winvestco.portfolio_service.exception.PortfolioNotFoundException;
import in.winvestco.portfolio_service.mapper.PortfolioMapper;
import in.winvestco.portfolio_service.model.Holding;
import in.winvestco.portfolio_service.model.Portfolio;
import in.winvestco.portfolio_service.repository.PortfolioRepository;
import in.winvestco.portfolio_service.client.MarketServiceClient;
import in.winvestco.portfolio_service.dto.HoldingDTO;
import in.winvestco.portfolio_service.dto.StockQuoteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing user portfolios.
 * Each user has exactly one portfolio.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioMapper portfolioMapper;
    private final MarketServiceClient marketServiceClient;

    /**
     * Create a demo portfolio for a new user.
     * Called when a UserCreatedEvent is received.
     */
    @Transactional
    public Portfolio createPortfolioForUser(Long userId, String email) {
        log.info("Creating demo portfolio for user: {} ({})", userId, email);

        // Check if portfolio already exists
        if (portfolioRepository.existsByUserId(userId)) {
            log.warn("Portfolio already exists for user: {}", userId);
            return portfolioRepository.findByUserId(userId).orElse(null);
        }

        Portfolio portfolio = Portfolio.builder()
                .userId(userId)
                .name("My Portfolio")
                .description("Welcome to WINVESTCO! This is your personal investment portfolio.")
                .status(PortfolioStatus.ACTIVE)
                .isDefault(true)
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .build();

        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Created primary portfolio {} for user {}", saved.getId(), userId);

        return saved;
    }

    /**
     * Create a new portfolio for a user manually.
     */
    @Transactional
    public PortfolioDTO createPortfolio(Long userId, CreatePortfolioRequest request) {
        log.info("Creating new {} portfolio for user: {}", request.getPortfolioType(), userId);

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());

        // If this is the user's first portfolio, make it default
        if (!portfolioRepository.existsByUserId(userId)) {
            isDefault = true;
        } else if (isDefault) {
            // Unset previous default
            portfolioRepository.findDefaultByUserId(userId).ifPresent(p -> {
                p.setIsDefault(false);
                portfolioRepository.save(p);
            });
        }

        Portfolio portfolio = Portfolio.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .portfolioType(request.getPortfolioType())
                .isDefault(isDefault)
                .status(PortfolioStatus.ACTIVE)
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .build();

        Portfolio saved = portfolioRepository.save(portfolio);
        return portfolioMapper.toDTO(saved);
    }

    /**
     * Get default portfolio by user ID
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolioByUserId(Long userId) {
        log.debug("Fetching default portfolio for user: {}", userId);

        Portfolio portfolio = portfolioRepository.findDefaultByUserIdWithHoldings(userId)
                .orElseGet(() -> portfolioRepository.findAllByUserId(userId).stream().findFirst()
                        .orElseThrow(() -> new PortfolioNotFoundException("userId", userId)));

        PortfolioDTO dto = portfolioMapper.toDTO(portfolio);
        dto = enrichWithMarketData(dto);
        return enrichPortfolioDTO(dto);
    }

    /**
     * Get all portfolios for a user
     */
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getAllPortfoliosByUserId(Long userId) {
        log.debug("Fetching all portfolios for user: {}", userId);

        List<Portfolio> portfolios = portfolioRepository.findAllByUserId(userId);
        if (portfolios.isEmpty()) {
            return Collections.emptyList();
        }

        return portfolios.stream()
                .map(portfolioMapper::toDTO)
                .map(this::enrichWithMarketData)
                .map(this::enrichPortfolioDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get portfolio by ID (with ownership validation)
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolioById(Long portfolioId, Long userId) {
        log.debug("Fetching portfolio {} for user {}", portfolioId, userId);

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        PortfolioDTO dto = portfolioMapper.toDTO(portfolio);
        dto = enrichWithMarketData(dto);
        return enrichPortfolioDTO(dto);
    }

    /**
     * Update portfolio details
     */
    @Transactional
    public PortfolioDTO updatePortfolio(Long userId, UpdatePortfolioRequest request) {
        log.info("Updating portfolio for user: {}", userId);

        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        if (request.getName() != null && !request.getName().isBlank()) {
            portfolio.setName(request.getName());
        }
        if (request.getDescription() != null) {
            portfolio.setDescription(request.getDescription());
        }

        Portfolio updated = portfolioRepository.save(portfolio);
        log.info("Updated portfolio {} for user {}", updated.getId(), userId);

        return portfolioMapper.toDTO(updated);
    }

    /**
     * Update portfolio totals (called after holding changes)
     */
    @Transactional
    public void updatePortfolioTotals(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findByIdWithHoldings(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        BigDecimal totalInvested = portfolio.getHoldings().stream()
                .map(Holding::getTotalInvested)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        portfolio.setTotalInvested(totalInvested);
        portfolioRepository.save(portfolio);

        log.debug("Updated portfolio {} totals: totalInvested={}", portfolioId, totalInvested);
    }

    /**
     * Archive a portfolio
     */
    @Transactional
    public PortfolioDTO archivePortfolio(Long userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        portfolio.setStatus(PortfolioStatus.ARCHIVED);
        Portfolio archived = portfolioRepository.save(portfolio);

        log.info("Archived portfolio {} for user {}", archived.getId(), userId);
        return portfolioMapper.toDTO(archived);
    }

    /**
     * Set a portfolio as default for a user
     */
    @Transactional
    public PortfolioDTO setDefaultPortfolio(Long userId, Long portfolioId) {
        log.info("Setting portfolio {} as default for user {}", portfolioId, userId);

        // Verify ownership
        Portfolio newDefault = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        // Get current default and unset it
        portfolioRepository.findDefaultByUserId(userId).ifPresent(p -> {
            if (!p.getId().equals(portfolioId)) {
                p.setIsDefault(false);
                portfolioRepository.save(p);
            }
        });

        newDefault.setIsDefault(true);
        Portfolio saved = portfolioRepository.save(newDefault);

        return portfolioMapper.toDTO(saved);
    }

    /**
     * Reactivate an archived portfolio
     */
    @Transactional
    public PortfolioDTO reactivatePortfolio(Long userId) {
        Portfolio portfolio = portfolioRepository.findDefaultByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        portfolio.setStatus(PortfolioStatus.ACTIVE);
        Portfolio reactivated = portfolioRepository.save(portfolio);

        log.info("Reactivated portfolio {} for user {}", reactivated.getId(), userId);
        return portfolioMapper.toDTO(reactivated);
    }

    /**
     * Check if user has a portfolio
     */
    @Transactional(readOnly = true)
    public boolean hasPortfolio(Long userId) {
        return portfolioRepository.existsByUserId(userId);
    }

    /**
     * Get portfolio entity by user ID (internal use - returns default)
     */
    @Transactional(readOnly = true)
    public Portfolio getPortfolioEntityByUserId(Long userId) {
        return portfolioRepository.findDefaultByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));
    }

    /**
     * Enrich portfolio with real-time market data
     */
    private PortfolioDTO enrichWithMarketData(PortfolioDTO portfolio) {
        if (portfolio.getHoldings() == null || portfolio.getHoldings().isEmpty()) {
            return portfolio;
        }

        try {
            List<String> symbols = portfolio.getHoldings().stream()
                    .map(HoldingDTO::getSymbol)
                    .distinct()
                    .collect(Collectors.toList());

            if (symbols.isEmpty()) {
                return portfolio;
            }

            // Fetch bulk quotes from market service
            List<StockQuoteDTO> quotes = marketServiceClient.getBulkQuotes(symbols);

            Map<String, StockQuoteDTO> quoteMap = quotes.stream()
                    .filter(q -> q.getLastPrice() != null)
                    .collect(Collectors.toMap(StockQuoteDTO::getSymbol, q -> q, (a, b) -> a));

            BigDecimal totalCurrentValue = BigDecimal.ZERO;

            for (HoldingDTO holding : portfolio.getHoldings()) {
                StockQuoteDTO quote = quoteMap.get(holding.getSymbol());
                if (quote != null) {
                    holding.setCurrentPrice(quote.getLastPrice());
                    holding.setDayChange(quote.getChange());
                    holding.setDayChangePercentage(quote.getPChange());

                    if (holding.getQuantity() != null && quote.getLastPrice() != null) {
                        BigDecimal currentValue = holding.getQuantity().multiply(quote.getLastPrice());
                        holding.setCurrentValue(currentValue);

                        if (holding.getTotalInvested() != null) {
                            holding.setProfitLoss(currentValue.subtract(holding.getTotalInvested()));
                            if (holding.getTotalInvested().compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal plPercent = holding.getProfitLoss()
                                        .divide(holding.getTotalInvested(), 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                                holding.setProfitLossPercentage(plPercent);
                            }
                        }

                        totalCurrentValue = totalCurrentValue.add(currentValue);
                    }
                } else if (holding.getTotalInvested() != null) {
                    // Fallback: Use total invested as current value if no price
                    totalCurrentValue = totalCurrentValue.add(holding.getTotalInvested());
                }
            }

            portfolio.setCurrentValue(totalCurrentValue);

        } catch (Exception e) {
            log.error("Failed to enrich portfolio with market data", e);
            // On failure, rely on stored values or separate logic
        }

        return portfolio;
    }

    /**
     * Enrich portfolio DTO with calculated P&L values
     */
    private PortfolioDTO enrichPortfolioDTO(PortfolioDTO dto) {
        if (dto.getTotalInvested() != null && dto.getCurrentValue() != null) {
            BigDecimal profitLoss = dto.getCurrentValue().subtract(dto.getTotalInvested());
            dto.setProfitLoss(profitLoss);

            if (dto.getTotalInvested().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = profitLoss
                        .divide(dto.getTotalInvested(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                dto.setProfitLossPercentage(percentage);
            }
        }
        return dto;
    }
}

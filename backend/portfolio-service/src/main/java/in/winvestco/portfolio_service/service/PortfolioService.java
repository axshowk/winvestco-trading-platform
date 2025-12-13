package in.winvestco.portfolio_service.service;

import in.winvestco.common.enums.PortfolioStatus;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.portfolio_service.dto.HoldingDTO;
import in.winvestco.portfolio_service.dto.PortfolioDTO;
import in.winvestco.portfolio_service.dto.UpdatePortfolioRequest;
import in.winvestco.portfolio_service.exception.PortfolioNotFoundException;
import in.winvestco.portfolio_service.mapper.PortfolioMapper;
import in.winvestco.portfolio_service.model.Holding;
import in.winvestco.portfolio_service.model.Portfolio;
import in.winvestco.portfolio_service.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
    private final LoggingUtils loggingUtils;

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
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .build();

        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("Created portfolio {} for user {}", saved.getId(), userId);
        
        return saved;
    }

    /**
     * Get portfolio by user ID
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolioByUserId(Long userId) {
        log.debug("Fetching portfolio for user: {}", userId);

        Portfolio portfolio = portfolioRepository.findByUserIdWithHoldings(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        return enrichPortfolioDTO(portfolioMapper.toDTO(portfolio));
    }

    /**
     * Get portfolio by ID (with ownership validation)
     */
    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolioById(Long portfolioId, Long userId) {
        log.debug("Fetching portfolio {} for user {}", portfolioId, userId);

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        return enrichPortfolioDTO(portfolioMapper.toDTO(portfolio));
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
     * Reactivate an archived portfolio
     */
    @Transactional
    public PortfolioDTO reactivatePortfolio(Long userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
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
     * Get portfolio entity by user ID (internal use)
     */
    @Transactional(readOnly = true)
    public Portfolio getPortfolioEntityByUserId(Long userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));
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

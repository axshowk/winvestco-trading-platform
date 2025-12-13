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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service for managing stock holdings within a portfolio.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioService portfolioService;
    private final PortfolioMapper portfolioMapper;

    /**
     * Get all holdings for a user's portfolio
     */
    @Transactional(readOnly = true)
    public List<HoldingDTO> getHoldingsByUserId(Long userId) {
        log.debug("Fetching holdings for user: {}", userId);
        
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        return portfolioMapper.toHoldingDTOList(holdings);
    }

    /**
     * Get all holdings for a portfolio
     */
    @Transactional(readOnly = true)
    public List<HoldingDTO> getHoldingsByPortfolioId(Long portfolioId) {
        log.debug("Fetching holdings for portfolio: {}", portfolioId);
        
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        return portfolioMapper.toHoldingDTOList(holdings);
    }

    /**
     * Add a new holding to user's portfolio
     */
    @Transactional
    public HoldingDTO addHolding(Long userId, AddHoldingRequest request) {
        log.info("Adding holding {} to portfolio for user {}", request.getSymbol(), userId);

        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        // Check if holding already exists
        if (holdingRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), request.getSymbol().toUpperCase())) {
            throw new IllegalArgumentException("Holding for symbol " + request.getSymbol() + " already exists. Use update instead.");
        }

        Holding holding = Holding.builder()
                .portfolio(portfolio)
                .symbol(request.getSymbol().toUpperCase())
                .companyName(request.getCompanyName())
                .exchange(request.getExchange() != null ? request.getExchange() : "NSE")
                .quantity(request.getQuantity())
                .averagePrice(request.getAveragePrice())
                .build();

        holding.calculateTotalInvested();

        Holding saved = holdingRepository.save(holding);
        log.info("Added holding {} (id={}) to portfolio {}", saved.getSymbol(), saved.getId(), portfolio.getId());

        // Update portfolio totals
        portfolioService.updatePortfolioTotals(portfolio.getId());

        return portfolioMapper.toDTO(saved);
    }

    /**
     * Update an existing holding
     */
    @Transactional
    public HoldingDTO updateHolding(Long userId, Long holdingId, UpdateHoldingRequest request) {
        log.info("Updating holding {} for user {}", holdingId, userId);

        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        Holding holding = holdingRepository.findById(holdingId)
                .filter(h -> h.getPortfolio().getId().equals(portfolio.getId()))
                .orElseThrow(() -> new HoldingNotFoundException(holdingId));

        holding.setQuantity(request.getQuantity());
        holding.setAveragePrice(request.getAveragePrice());
        holding.calculateTotalInvested();

        Holding updated = holdingRepository.save(holding);
        log.info("Updated holding {} for user {}", updated.getId(), userId);

        // Update portfolio totals
        portfolioService.updatePortfolioTotals(portfolio.getId());

        return portfolioMapper.toDTO(updated);
    }

    /**
     * Update holding by adding more quantity (averaging)
     */
    @Transactional
    public HoldingDTO addToHolding(Long userId, String symbol, BigDecimal quantity, BigDecimal price) {
        log.info("Adding {} shares of {} at {} for user {}", quantity, symbol, price, userId);

        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        Holding holding = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol.toUpperCase())
                .orElseThrow(() -> new HoldingNotFoundException("symbol", symbol));

        // Calculate new average price
        BigDecimal currentValue = holding.getQuantity().multiply(holding.getAveragePrice());
        BigDecimal addedValue = quantity.multiply(price);
        BigDecimal newQuantity = holding.getQuantity().add(quantity);
        BigDecimal newAveragePrice = currentValue.add(addedValue).divide(newQuantity, 4, RoundingMode.HALF_UP);

        holding.setQuantity(newQuantity);
        holding.setAveragePrice(newAveragePrice);
        holding.calculateTotalInvested();

        Holding updated = holdingRepository.save(holding);

        // Update portfolio totals
        portfolioService.updatePortfolioTotals(portfolio.getId());

        return portfolioMapper.toDTO(updated);
    }

    /**
     * Reduce holding quantity (partial sell)
     */
    @Transactional
    public HoldingDTO reduceHolding(Long userId, String symbol, BigDecimal quantity) {
        log.info("Reducing {} shares of {} for user {}", quantity, symbol, userId);

        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        Holding holding = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol.toUpperCase())
                .orElseThrow(() -> new HoldingNotFoundException("symbol", symbol));

        if (quantity.compareTo(holding.getQuantity()) > 0) {
            throw new IllegalArgumentException("Cannot reduce more than current quantity");
        }

        BigDecimal newQuantity = holding.getQuantity().subtract(quantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // Remove the holding completely
            holdingRepository.delete(holding);
            log.info("Removed holding {} for user {}", symbol, userId);
            portfolioService.updatePortfolioTotals(portfolio.getId());
            return null;
        }

        holding.setQuantity(newQuantity);
        holding.calculateTotalInvested();

        Holding updated = holdingRepository.save(holding);

        // Update portfolio totals
        portfolioService.updatePortfolioTotals(portfolio.getId());

        return portfolioMapper.toDTO(updated);
    }

    /**
     * Remove a holding completely
     */
    @Transactional
    public void removeHolding(Long userId, Long holdingId) {
        log.info("Removing holding {} for user {}", holdingId, userId);

        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        Holding holding = holdingRepository.findById(holdingId)
                .filter(h -> h.getPortfolio().getId().equals(portfolio.getId()))
                .orElseThrow(() -> new HoldingNotFoundException(holdingId));

        holdingRepository.delete(holding);
        log.info("Removed holding {} for user {}", holdingId, userId);

        // Update portfolio totals
        portfolioService.updatePortfolioTotals(portfolio.getId());
    }

    /**
     * Get a specific holding by symbol
     */
    @Transactional(readOnly = true)
    public HoldingDTO getHoldingBySymbol(Long userId, String symbol) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new PortfolioNotFoundException("userId", userId));

        Holding holding = holdingRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol.toUpperCase())
                .orElseThrow(() -> new HoldingNotFoundException("symbol", symbol));

        return portfolioMapper.toDTO(holding);
    }
}

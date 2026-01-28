package in.winvestco.portfolio_service.controller;

import in.winvestco.portfolio_service.dto.AddHoldingRequest;
import in.winvestco.portfolio_service.dto.BuyStockRequest;
import in.winvestco.portfolio_service.dto.CreatePortfolioRequest;
import in.winvestco.portfolio_service.dto.HoldingDTO;
import in.winvestco.portfolio_service.dto.PortfolioDTO;
import in.winvestco.portfolio_service.dto.SellStockRequest;
import in.winvestco.portfolio_service.dto.TradeResponse;
import in.winvestco.portfolio_service.dto.UpdateHoldingRequest;
import in.winvestco.portfolio_service.dto.UpdatePortfolioRequest;
import in.winvestco.portfolio_service.exception.HoldingNotFoundException;
import in.winvestco.portfolio_service.service.HoldingService;
import in.winvestco.portfolio_service.service.PortfolioService;
import in.winvestco.portfolio_service.service.PortfolioWebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Portfolio operations.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio", description = "Portfolio management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final HoldingService holdingService;
    private final PortfolioWebSocketService webSocketService;

    @GetMapping
    @Operation(summary = "Get default portfolio", description = "Retrieve the current user's default portfolio with all holdings")
    @ApiResponse(responseCode = "200", description = "Portfolio retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Portfolio not found")
    public ResponseEntity<PortfolioDTO> getMyDefaultPortfolio(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.debug("Getting default portfolio for user: {}", userId);

        PortfolioDTO portfolio = portfolioService.getPortfolioByUserId(userId);
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all portfolios", description = "Retrieve all portfolios for the current user")
    @ApiResponse(responseCode = "200", description = "Portfolios retrieved successfully")
    public ResponseEntity<List<PortfolioDTO>> getAllPortfolios(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.debug("Getting all portfolios for user: {}", userId);

        List<PortfolioDTO> portfolios = portfolioService.getAllPortfoliosByUserId(userId);
        return ResponseEntity.ok(portfolios);
    }

    @PostMapping
    @Operation(summary = "Create portfolio", description = "Create a new portfolio for different investment strategies")
    @ApiResponse(responseCode = "201", description = "Portfolio created successfully")
    public ResponseEntity<PortfolioDTO> createPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePortfolioRequest request) {
        Long userId = extractUserId(jwt);
        log.info("Creating new portfolio for user: {}", userId);

        PortfolioDTO created = portfolioService.createPortfolio(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{portfolioId}")
    @Operation(summary = "Get portfolio by ID", description = "Retrieve a specific portfolio by ID")
    @ApiResponse(responseCode = "200", description = "Portfolio retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Portfolio not found")
    public ResponseEntity<PortfolioDTO> getPortfolioById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId) {
        Long userId = extractUserId(jwt);
        log.debug("Getting portfolio {} for user: {}", portfolioId, userId);

        PortfolioDTO portfolio = portfolioService.getPortfolioById(portfolioId, userId);
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{portfolioId}/set-default")
    @Operation(summary = "Set default portfolio", description = "Set a portfolio as the default one for the user")
    @ApiResponse(responseCode = "200", description = "Default portfolio updated successfully")
    public ResponseEntity<PortfolioDTO> setDefaultPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId) {
        Long userId = extractUserId(jwt);
        log.info("Setting portfolio {} as default for user {}", portfolioId, userId);

        PortfolioDTO updated = portfolioService.setDefaultPortfolio(userId, portfolioId);
        return ResponseEntity.ok(updated);
    }

    @PutMapping
    @Operation(summary = "Update portfolio", description = "Update portfolio name and description")
    @ApiResponse(responseCode = "200", description = "Portfolio updated successfully")
    @ApiResponse(responseCode = "404", description = "Portfolio not found")
    public ResponseEntity<PortfolioDTO> updatePortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        Long userId = extractUserId(jwt);
        log.info("Updating portfolio for user: {}", userId);

        PortfolioDTO updated = portfolioService.updatePortfolio(userId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/holdings")
    @Operation(summary = "Get all holdings", description = "Retrieve all holdings in the user's portfolio")
    @ApiResponse(responseCode = "200", description = "Holdings retrieved successfully")
    public ResponseEntity<List<HoldingDTO>> getHoldings(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.debug("Getting holdings for user: {}", userId);

        List<HoldingDTO> holdings = holdingService.getHoldingsByUserId(userId);
        return ResponseEntity.ok(holdings);
    }

    @PostMapping("/holdings")
    @Operation(summary = "Add a holding", description = "Add a new stock holding to the portfolio")
    @ApiResponse(responseCode = "201", description = "Holding added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or holding already exists")
    public ResponseEntity<HoldingDTO> addHolding(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddHoldingRequest request) {
        Long userId = extractUserId(jwt);
        log.info("Adding holding {} for user: {}", request.getSymbol(), userId);

        HoldingDTO holding = holdingService.addHolding(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(holding);
    }

    @GetMapping("/holdings/{symbol}")
    @Operation(summary = "Get holding by symbol", description = "Retrieve a specific holding by stock symbol")
    @ApiResponse(responseCode = "200", description = "Holding retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Holding not found")
    public ResponseEntity<HoldingDTO> getHoldingBySymbol(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Stock symbol", example = "RELIANCE") @PathVariable String symbol) {
        Long userId = extractUserId(jwt);
        log.debug("Getting holding {} for user: {}", symbol, userId);

        HoldingDTO holding = holdingService.getHoldingBySymbol(userId, symbol);
        return ResponseEntity.ok(holding);
    }

    @PutMapping("/holdings/{holdingId}")
    @Operation(summary = "Update a holding", description = "Update quantity and average price of a holding")
    @ApiResponse(responseCode = "200", description = "Holding updated successfully")
    @ApiResponse(responseCode = "404", description = "Holding not found")
    public ResponseEntity<HoldingDTO> updateHolding(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Holding ID") @PathVariable Long holdingId,
            @Valid @RequestBody UpdateHoldingRequest request) {
        Long userId = extractUserId(jwt);
        log.info("Updating holding {} for user: {}", holdingId, userId);

        HoldingDTO updated = holdingService.updateHolding(userId, holdingId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/holdings/{holdingId}")
    @Operation(summary = "Remove a holding", description = "Remove a holding completely from the portfolio")
    @ApiResponse(responseCode = "204", description = "Holding removed successfully")
    @ApiResponse(responseCode = "404", description = "Holding not found")
    public ResponseEntity<Void> removeHolding(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Holding ID") @PathVariable Long holdingId) {
        Long userId = extractUserId(jwt);
        log.info("Removing holding {} for user: {}", holdingId, userId);

        holdingService.removeHolding(userId, holdingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/archive")
    @Operation(summary = "Archive portfolio", description = "Archive the user's portfolio (can be reactivated later)")
    @ApiResponse(responseCode = "200", description = "Portfolio archived successfully")
    public ResponseEntity<PortfolioDTO> archivePortfolio(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.info("Archiving portfolio for user: {}", userId);

        PortfolioDTO archived = portfolioService.archivePortfolio(userId);
        return ResponseEntity.ok(archived);
    }

    @PostMapping("/reactivate")
    @Operation(summary = "Reactivate portfolio", description = "Reactivate an archived portfolio")
    @ApiResponse(responseCode = "200", description = "Portfolio reactivated successfully")
    public ResponseEntity<PortfolioDTO> reactivatePortfolio(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        log.info("Reactivating portfolio for user: {}", userId);

        PortfolioDTO reactivated = portfolioService.reactivatePortfolio(userId);
        return ResponseEntity.ok(reactivated);
    }

    @PostMapping("/buy")
    @Operation(summary = "Buy stocks", description = "Buy stocks and add to portfolio. Creates new holding if not exists, or adds to existing holding with weighted average price.")
    @ApiResponse(responseCode = "200", description = "Buy order executed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<TradeResponse> buyStock(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BuyStockRequest request) {
        Long userId = extractUserId(jwt);
        log.info("Buy order: {} shares of {} at {} for user {}",
                request.getQuantity(), request.getSymbol(), request.getPrice(), userId);

        try {
            HoldingDTO holding;
            // Check if holding already exists
            try {
                holding = holdingService.getHoldingBySymbol(userId, request.getSymbol());
                // Holding exists, add to it
                holding = holdingService.addToHolding(userId, request.getSymbol(),
                        request.getQuantity(), request.getPrice());
            } catch (HoldingNotFoundException e) {
                // Create new holding
                AddHoldingRequest addRequest = AddHoldingRequest.builder()
                        .symbol(request.getSymbol().toUpperCase())
                        .companyName(request.getCompanyName())
                        .exchange("NSE")
                        .quantity(request.getQuantity())
                        .averagePrice(request.getPrice())
                        .build();
                holding = holdingService.addHolding(userId, addRequest);
            }

            // Send real-time WebSocket notification
            webSocketService.sendTradeExecutedNotification(userId, request.getSymbol(),
                    "Bought", request.getQuantity(), request.getPrice());

            return ResponseEntity.ok(TradeResponse.success(
                    String.format("Successfully bought %s shares of %s",
                            request.getQuantity(), request.getSymbol()),
                    holding));
        } catch (Exception e) {
            log.error("Buy order failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(TradeResponse.error("Buy order failed: " + e.getMessage()));
        }
    }

    @PostMapping("/sell")
    @Operation(summary = "Sell stocks", description = "Sell stocks from portfolio. Reduces holding quantity or removes if selling all.")
    @ApiResponse(responseCode = "200", description = "Sell order executed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or insufficient shares")
    @ApiResponse(responseCode = "404", description = "Holding not found")
    public ResponseEntity<TradeResponse> sellStock(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SellStockRequest request) {
        Long userId = extractUserId(jwt);
        log.info("Sell order: {} shares of {} for user {}",
                request.getQuantity(), request.getSymbol(), userId);

        try {
            HoldingDTO holding = holdingService.reduceHolding(userId,
                    request.getSymbol(), request.getQuantity());

            if (holding == null) {
                // All shares sold
                webSocketService.sendTradeExecutedNotification(userId, request.getSymbol(),
                        "Sold all", request.getQuantity(),
                        request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);
                return ResponseEntity.ok(TradeResponse.success(
                        String.format("Successfully sold all shares of %s", request.getSymbol()),
                        null));
            }

            // Send real-time WebSocket notification
            webSocketService.sendTradeExecutedNotification(userId, request.getSymbol(),
                    "Sold", request.getQuantity(),
                    request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);

            return ResponseEntity.ok(TradeResponse.success(
                    String.format("Successfully sold %s shares of %s",
                            request.getQuantity(), request.getSymbol()),
                    holding));
        } catch (HoldingNotFoundException e) {
            log.warn("Sell order failed - holding not found: {}", request.getSymbol());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(TradeResponse.error("You don't own any shares of " + request.getSymbol()));
        } catch (IllegalArgumentException e) {
            log.warn("Sell order failed - insufficient shares: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(TradeResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Sell order failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(TradeResponse.error("Sell order failed: " + e.getMessage()));
        }
    }

    /**
     * Extract user ID from JWT token
     */
    private Long extractUserId(Jwt jwt) {
        // Try to get user ID from 'sub' claim or 'userId' claim
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim != null) {
            if (userIdClaim instanceof Long) {
                return (Long) userIdClaim;
            }
            return Long.parseLong(userIdClaim.toString());
        }

        // Fallback to subject claim
        String subject = jwt.getSubject();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Cannot extract user ID from JWT token");
        }
    }
}

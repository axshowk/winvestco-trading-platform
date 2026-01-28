# Portfolio Service Improvements

> **Module**: `portfolio-service`  
> **Analysis Date**: 2026-01-28  
> **Last Updated**: 2026-01-28  
> **Status**: In Progress - 4 of 12 resolved (3 implemented, 1 already exists)

---

## Executive Summary

The portfolio-service is a Spring Boot microservice that manages user investment portfolios and stock holdings. While the current implementation provides basic CRUD operations and P&L calculations, several enhancements are needed to meet modern trading platform standards.

### Current Tech Stack
| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.x with Spring Cloud |
| Database | PostgreSQL with JPA/Hibernate |
| Messaging | RabbitMQ for event-driven communication |
| Service Discovery | Netflix Eureka |
| Security | OAuth2 Resource Server with JWT |
| API Documentation | SpringDoc OpenAPI |
| Observability | Actuator + Prometheus + OpenTelemetry |

### Current Features
- [x] Portfolio CRUD operations
- [x] Holdings management (add, update, remove)
- [x] Buy/Sell stock operations with weighted average pricing
- [x] P&L calculations (overall and day-based)
- [x] Event-driven portfolio creation via RabbitMQ
- [x] JWT-based authentication
- [x] Database migrations with Flyway

---

## Improvement Recommendations

### 1. Real-Time Portfolio Updates ✅ COMPLETED

**Priority**: 🔴 CRITICAL  
**Impact**: User Experience  
**Effort**: Medium (2-3 days)  
**Status**: ✅ Implemented on 2026-01-28

#### Implementation Summary

WebSocket support has been fully implemented for real-time portfolio updates.

#### Files Created

| File | Description |
|------|-------------|
| [WebSocketConfig.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/config/WebSocketConfig.java) | WebSocket configuration at `/ws/portfolio` |
| [PortfolioWebSocketSessionManager.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/websocket/PortfolioWebSocketSessionManager.java) | Session management per user |
| [PortfolioWebSocketHandler.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/websocket/PortfolioWebSocketHandler.java) | Connection lifecycle handler |
| [PortfolioWebSocketService.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/service/PortfolioWebSocketService.java) | Service for sending updates |
| [PortfolioUpdateMessage.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/dto/PortfolioUpdateMessage.java) | DTO for WebSocket messages |
| [usePortfolioWebSocket.js](file:///e:/winvestco-trading-platform/frontend/src/hooks/usePortfolioWebSocket.js) | React hook for WebSocket |

#### Files Modified

| File | Changes |
|------|---------|  
| [pom.xml](file:///e:/winvestco-trading-platform/backend/portfolio-service/pom.xml) | Added `spring-boot-starter-websocket` |
| [SecurityConfig.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/config/SecurityConfig.java) | Permitted `/ws/**` endpoint |
| [PortfolioController.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/controller/PortfolioController.java) | Trade notifications on buy/sell |
| [Portfolio.jsx](file:///e:/winvestco-trading-platform/frontend/src/pages/Portfolio.jsx) | WebSocket hook integration |
| [Portfolio.css](file:///e:/winvestco-trading-platform/frontend/src/pages/Portfolio.css) | Connection status & toast styles |

#### Features Delivered
- ✅ WebSocket endpoint at `/ws/portfolio?userId={id}`
- ✅ Multi-session support (multiple tabs/devices per user)
- ✅ Trade execution notifications (buy/sell)
- ✅ Price update broadcasting capability
- ✅ Frontend connection status indicator (Live/Offline)
- ✅ Trade notification toast messages
- ✅ Auto-reconnect with exponential backoff
- ✅ Heartbeat ping/pong every 30 seconds

---

### 2. Transaction History ⚠️ ALREADY EXISTS

**Priority**: 🔴 CRITICAL → ✅ **NOT NEEDED IN PORTFOLIO-SERVICE**  
**Impact**: Regulatory Compliance, User Experience  
**Status**: ⚠️ Already implemented in `trade-service` and `ledger-service`

#### Analysis

Transaction history is **already maintained** in dedicated services:

| Service | Entity | Purpose |
|---------|--------|---------|
| [trade-service](file:///e:/winvestco-trading-platform/backend/trade-service) | `Trade` | Full trade lifecycle tracking: BUY/SELL with quantities, prices, timestamps |
| [ledger-service](file:///e:/winvestco-trading-platform/backend/ledger-service) | `LedgerEntry` | **IMMUTABLE** audit trail for all money movements |

#### Existing Trade Entity Features
```java
// Trade entity already tracks:
- tradeId, orderId, userId, symbol
- side (BUY/SELL), quantity, price
- executedQuantity, averagePrice
- status (CREATED → FILLED → CLOSED)
- createdAt, validatedAt, placedAt, executedAt, closedAt
```

#### Existing LedgerEntry Features  
```java
// LedgerEntry (IMMUTABLE) tracks:
- walletId, entryType, amount
- balanceBefore, balanceAfter
- referenceId, referenceType, description
- createdAt (NO updatedAt - immutable)
```

#### Existing APIs
- `GET /api/v1/trades/user/{userId}` - User's trade history (trade-service)
- `GET /api/v1/ledger/wallet/{walletId}` - Wallet ledger entries (ledger-service)

#### Recommendation Change
Instead of duplicating transaction storage in portfolio-service, the frontend should:
1. Call `trade-service` for trade history
2. Call `ledger-service` for financial audit trail

**Optional Enhancement**: Add a Feign client in portfolio-service to aggregate trade history if a unified API is needed.

---

### ~~Original Recommendation (NO LONGER NEEDED)~~

~~Add a `Transaction` entity to track all portfolio activities.~~

#### Database Migration
```sql
-- V2__Add_transactions_table.sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id),
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(100),
    transaction_type VARCHAR(20) NOT NULL, -- BUY, SELL, DIVIDEND, SPLIT, BONUS
    quantity NUMERIC(18, 4) NOT NULL,
    price NUMERIC(18, 4) NOT NULL,
    total_amount NUMERIC(18, 4) NOT NULL,
    fees NUMERIC(18, 4) DEFAULT 0,
    taxes NUMERIC(18, 4) DEFAULT 0,
    net_amount NUMERIC(18, 4) NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    settlement_date DATE,
    order_id BIGINT, -- Reference to order-service
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_portfolio ON transactions(portfolio_id);
CREATE INDEX idx_transactions_symbol ON transactions(symbol);
CREATE INDEX idx_transactions_executed_at ON transactions(executed_at);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);
```

#### Entity Model
```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;
    
    @NotBlank
    private String symbol;
    
    private String companyName;
    
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    @NotNull
    @Positive
    private BigDecimal quantity;
    
    @NotNull
    @Positive
    private BigDecimal price;
    
    private BigDecimal totalAmount;
    private BigDecimal fees;
    private BigDecimal taxes;
    private BigDecimal netAmount;
    
    @NotNull
    private Instant executedAt;
    
    private LocalDate settlementDate;
    private Long orderId;
    private String notes;
    
    @CreatedDate
    private Instant createdAt;
}

public enum TransactionType {
    BUY, SELL, DIVIDEND, SPLIT, BONUS, TRANSFER_IN, TRANSFER_OUT
}
```

**Files to Create**:
- `[NEW] model/Transaction.java`
- `[NEW] model/TransactionType.java`
- `[NEW] repository/TransactionRepository.java`
- `[NEW] service/TransactionService.java`
- `[NEW] controller/TransactionController.java`
- `[NEW] dto/TransactionDTO.java`
- `[NEW] dto/TransactionFilterRequest.java`
- `[NEW] resources/db/migration/V2__Add_transactions_table.sql`

**Endpoints to Add**:
- `GET /api/v1/portfolios/transactions` - List all transactions with filtering
- `GET /api/v1/portfolios/transactions/{id}` - Get single transaction
- `GET /api/v1/portfolios/transactions/export` - Export as CSV/PDF

---

### 3. Portfolio Performance Analytics

**Priority**: 🟠 HIGH  
**Impact**: Competitive Feature, User Engagement  
**Effort**: High (4-5 days)

#### Current Gap
Limited analytics - only basic P&L calculations exist. Modern trading platforms provide:
- Annualized returns (XIRR/CAGR)
- Risk metrics
- Diversification analysis
- Benchmark comparisons

#### Recommendation
Add comprehensive analytics endpoints.

#### Analytics DTO
```java
@Data
@Builder
public class PortfolioAnalyticsDTO {
    // Returns
    private BigDecimal absoluteReturn;
    private BigDecimal absoluteReturnPercentage;
    private BigDecimal xirr; // Extended Internal Rate of Return
    private BigDecimal cagr; // Compound Annual Growth Rate
    
    // Risk Metrics
    private BigDecimal volatility;
    private BigDecimal sharpeRatio;
    private BigDecimal beta;
    private BigDecimal maxDrawdown;
    
    // Allocation
    private Map<String, BigDecimal> sectorAllocation;
    private Map<String, BigDecimal> industryAllocation;
    private Map<String, BigDecimal> marketCapAllocation; // Large, Mid, Small cap
    
    // Diversification
    private Integer totalStocks;
    private Integer totalSectors;
    private BigDecimal concentrationRisk; // Top 5 holdings percentage
    
    // Performance Comparison
    private BigDecimal benchmarkReturn; // NIFTY50
    private BigDecimal alpha; // Excess return over benchmark
    
    // Historical Performance
    private List<PerformanceDataPoint> dailyPerformance;
    private Map<String, BigDecimal> periodReturns; // 1D, 1W, 1M, 3M, 6M, 1Y, YTD, ALL
}

@Data
public class PerformanceDataPoint {
    private LocalDate date;
    private BigDecimal portfolioValue;
    private BigDecimal investedAmount;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercentage;
}
```

#### Analytics Service
```java
@Service
public class PortfolioAnalyticsService {
    
    public PortfolioAnalyticsDTO calculateAnalytics(Long portfolioId) {
        // Implementation
    }
    
    public BigDecimal calculateXIRR(List<Transaction> transactions, BigDecimal currentValue) {
        // Newton-Raphson method for XIRR calculation
    }
    
    public Map<String, BigDecimal> calculateSectorAllocation(List<HoldingDTO> holdings) {
        // Map holdings to sectors and calculate percentages
    }
    
    public List<PerformanceDataPoint> getHistoricalPerformance(
            Long portfolioId, LocalDate startDate, LocalDate endDate) {
        // Fetch from snapshots table
    }
}
```

**Endpoints to Add**:
- `GET /api/v1/portfolios/analytics` - Get full analytics
- `GET /api/v1/portfolios/analytics/sector-allocation` - Sector breakdown
- `GET /api/v1/portfolios/analytics/performance?period=1Y` - Historical performance

---

### 4. Redis Caching Layer

**Priority**: 🟠 HIGH  
**Impact**: Performance, Scalability  
**Effort**: Low (1 day)

#### Current Gap
No caching - every request hits the database directly. This impacts:
- Response times
- Database load
- Scalability

#### Recommendation
Add Redis caching for frequently accessed data.

#### Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

#### Cache Configuration
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(cf)
            .cacheDefaults(config)
            .withCacheConfiguration("portfolios", 
                config.entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("holdings",
                config.entryTtl(Duration.ofSeconds(30)))
            .build();
    }
}
```

#### Service Annotations
```java
@Service
public class PortfolioService {
    
    @Cacheable(value = "portfolios", key = "#userId")
    public PortfolioDTO getPortfolioByUserId(Long userId) { ... }
    
    @CacheEvict(value = "portfolios", key = "#userId")
    public PortfolioDTO updatePortfolio(Long userId, UpdatePortfolioRequest request) { ... }
    
    @CacheEvict(value = {"portfolios", "holdings"}, allEntries = true)
    public void updatePortfolioTotals(Long portfolioId) { ... }
}
```

**Files to Create/Modify**:
- `[NEW] config/CacheConfig.java`
- `[MODIFY] service/PortfolioService.java` - Add cache annotations
- `[MODIFY] service/HoldingService.java` - Add cache annotations
- `[MODIFY] application.yml` - Add Redis configuration
- `[MODIFY] pom.xml` - Add Redis dependencies

---

### 5. Market Data Integration ✅ COMPLETED

**Priority**: 🟡 MEDIUM  
**Impact**: Data Accuracy, User Experience  
**Effort**: Medium (2 days)  
**Status**: ✅ Implemented on 2026-01-28

#### Implementation Summary

Integrated `portfolio-service` with `market-service` using Feign Client to providing real-time pricing and P&L calculations on the server side.

#### Files Created/Modified

| File | Changes |
|------|---------|
| [MarketServiceClient.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/client/MarketServiceClient.java) | REST Client for Market Service |
| [MarketServiceFallback.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/client/MarketServiceFallback.java) | Circuit breaker fallback logic |
| [StockQuoteDTO.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/dto/StockQuoteDTO.java) | Data transfer object |
| [PortfolioService.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/service/PortfolioService.java) | Added `enrichWithMarketData` logic |

#### Features Delivered
- Server-side portfolio enrichment
- Real-time P&L calculation based on latest market availability
- Fallback mechanism when market-service is down
- Bulk price fetching for performance

---

#### Enriched Portfolio Service
```java
public PortfolioDTO getEnrichedPortfolio(Long userId) {
    PortfolioDTO portfolio = getPortfolioByUserId(userId);
    
    List<String> symbols = portfolio.getHoldings().stream()
        .map(HoldingDTO::getSymbol)
        .toList();
    
    Map<String, StockQuoteDTO> quotes = marketClient.getBulkQuotes(symbols)
        .stream()
        .collect(Collectors.toMap(StockQuoteDTO::getSymbol, q -> q));
    
    BigDecimal totalCurrentValue = BigDecimal.ZERO;
    
    for (HoldingDTO holding : portfolio.getHoldings()) {
        StockQuoteDTO quote = quotes.get(holding.getSymbol());
        if (quote != null && quote.getLastPrice() != null) {
            holding.setCurrentPrice(quote.getLastPrice());
            holding.setDayChange(quote.getChange());
            holding.setDayChangePercentage(quote.getPChange());
            
            BigDecimal currentValue = holding.getQuantity()
                .multiply(quote.getLastPrice());
            holding.setCurrentValue(currentValue);
            holding.setProfitLoss(currentValue.subtract(holding.getTotalInvested()));
            
            totalCurrentValue = totalCurrentValue.add(currentValue);
        }
    }
    
    portfolio.setCurrentValue(totalCurrentValue);
    enrichPortfolioDTO(portfolio);
    
    return portfolio;
}
```

**Files to Create/Modify**:
- `[NEW] client/MarketServiceClient.java`
- `[NEW] client/MarketServiceFallback.java`
- `[NEW] dto/StockQuoteDTO.java`
- `[MODIFY] service/PortfolioService.java` - Add enrichment method

---

### 6. Multiple Portfolios Support ✅ COMPLETED

**Priority**: 🟡 MEDIUM  
**Impact**: User Flexibility  
**Effort**: Medium (2-3 days)  
**Status**: ✅ Implemented on 2026-01-28

#### Implementation Summary

Database and API support for multiple portfolios per user has been implemented. Users can now create multiple portfolios (e.g., MAIN, PAPER_TRADING) and set one as default.

#### Files Created/Modified

| File | Changes |
|------|---------|
| [V2__Add_multiple_portfolios_support.sql](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/resources/db/migration/V2__Add_multiple_portfolios_support.sql) | Migration script for schema change |
| [PortfolioType.java](file:///e:/winvestco-trading-platform/backend/common/src/main/java/in/winvestco/common/enums/PortfolioType.java) | New enum for portfolio strategies |
| [Portfolio.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/model/Portfolio.java) | Entity updated with type and default flag |
| [PortfolioRepository.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/repository/PortfolioRepository.java) | Added default and list query methods |
| [PortfolioDTO.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/dto/PortfolioDTO.java) | DTO updated with new fields |
| [CreatePortfolioRequest.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/dto/CreatePortfolioRequest.java) | New request DTO |
| [PortfolioService.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/service/PortfolioService.java) | Implemented multiple portfolio logic |
| [PortfolioController.java](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/controller/PortfolioController.java) | Added endpoints for portfolio management |

#### Features Delivered
- Support for multiple portfolios per user (previously 1:1)
- Portfolio categorization (Main, Paper Trading, Watchlist, Retirement, Custom)
- Default portfolio management
- List all portfolios API
- Create new portfolio API
- Set default portfolio API
- Enhanced security (ownership validation on all IDs)

---

---

### 7. Portfolio Snapshots for Historical Tracking

**Priority**: 🟡 MEDIUM  
**Impact**: Historical Analysis, Reporting  
**Effort**: Medium (2 days)

#### Current Gap
No historical portfolio value tracking. Cannot show portfolio growth over time.

#### Recommendation
Create daily/hourly snapshots of portfolio state.

#### Database Migration
```sql
-- V3__Add_portfolio_snapshots.sql
CREATE TABLE portfolio_snapshots (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id),
    snapshot_date DATE NOT NULL,
    snapshot_time TIME,
    total_invested NUMERIC(18, 4),
    market_value NUMERIC(18, 4),
    day_profit_loss NUMERIC(18, 4),
    total_profit_loss NUMERIC(18, 4),
    total_profit_loss_percentage NUMERIC(10, 4),
    holdings_count INTEGER,
    holdings_snapshot JSONB, -- Store complete holdings state
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(portfolio_id, snapshot_date)
);

CREATE INDEX idx_snapshots_portfolio ON portfolio_snapshots(portfolio_id);
CREATE INDEX idx_snapshots_date ON portfolio_snapshots(snapshot_date);
```

#### Snapshot Scheduler
```java
@Component
@RequiredArgsConstructor
public class PortfolioSnapshotScheduler {
    
    private final PortfolioSnapshotService snapshotService;
    
    // Run at market close (3:30 PM IST)
    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void createDailySnapshots() {
        snapshotService.createSnapshotsForAllPortfolios();
    }
}
```

**Files to Create**:
- `[NEW] model/PortfolioSnapshot.java`
- `[NEW] repository/PortfolioSnapshotRepository.java`
- `[NEW] service/PortfolioSnapshotService.java`
- `[NEW] scheduler/PortfolioSnapshotScheduler.java`
- `[NEW] resources/db/migration/V3__Add_portfolio_snapshots.sql`

---

### 8. Enhanced Error Handling

**Priority**: 🟡 MEDIUM  
**Impact**: Reliability, User Experience  
**Effort**: Low (1 day)

#### Current Gap
Basic exception handling with generic error messages.

#### Recommendation
Implement domain-specific exceptions with error codes.

#### Exception Classes
```java
// Base exception
public abstract class PortfolioException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;
    
    public PortfolioException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
}

// Specific exceptions
public class InsufficientHoldingsException extends PortfolioException {
    public InsufficientHoldingsException(String symbol, BigDecimal requested, BigDecimal available) {
        super("INSUFFICIENT_HOLDINGS", 
              String.format("Cannot sell %s shares of %s. Available: %s", 
                           requested, symbol, available));
        getDetails().put("symbol", symbol);
        getDetails().put("requested", requested);
        getDetails().put("available", available);
    }
}

public class DuplicateHoldingException extends PortfolioException {
    public DuplicateHoldingException(String symbol) {
        super("DUPLICATE_HOLDING", 
              "Holding for " + symbol + " already exists in portfolio");
    }
}

public class PortfolioLimitExceededException extends PortfolioException {
    public PortfolioLimitExceededException(int limit) {
        super("PORTFOLIO_LIMIT_EXCEEDED",
              "Maximum portfolio limit of " + limit + " reached");
    }
}
```

#### Global Exception Handler
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(PortfolioException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioException(PortfolioException ex) {
        log.warn("Portfolio exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .timestamp(Instant.now())
                .path(getCurrentRequestPath())
                .build());
    }
    
    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PortfolioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .code("PORTFOLIO_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build());
    }
}

@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private Map<String, Object> details;
    private Instant timestamp;
    private String path;
    private String traceId;
}
```

---

### 9. Pagination for Holdings

**Priority**: 🟡 MEDIUM  
**Impact**: Scalability  
**Effort**: Low (0.5 day)

#### Current Gap
All holdings are fetched at once, which won't scale for users with many holdings.

#### Recommendation
Add pagination and sorting support.

#### Updated Repository
```java
public interface HoldingRepository extends JpaRepository<Holding, Long> {
    
    Page<Holding> findByPortfolioId(Long portfolioId, Pageable pageable);
    
    @Query("SELECT h FROM Holding h WHERE h.portfolio.userId = :userId")
    Page<Holding> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
```

#### Updated Controller
```java
@GetMapping("/holdings")
public ResponseEntity<Page<HoldingDTO>> getHoldings(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "symbol") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDir) {
    
    Long userId = extractUserId(jwt);
    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    
    Page<HoldingDTO> holdings = holdingService.getHoldingsByUserId(userId, pageable);
    return ResponseEntity.ok(holdings);
}
```

---

### 10. Circuit Breaker & Resilience

**Priority**: 🟢 LOW  
**Impact**: Stability, Fault Tolerance  
**Effort**: Low (1 day)

#### Current Gap
No protection against cascading failures when dependent services are down.

#### Recommendation
Add Resilience4j for circuit breaker, retry, and rate limiting.

#### Dependencies
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

#### Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      marketService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      marketService:
        maxAttempts: 3
        waitDuration: 500ms
        exponentialBackoffMultiplier: 2
  ratelimiter:
    instances:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
```

#### Service Integration
```java
@Service
public class MarketDataService {
    
    @CircuitBreaker(name = "marketService", fallbackMethod = "getDefaultQuotes")
    @Retry(name = "marketService")
    @RateLimiter(name = "default")
    public List<StockQuoteDTO> fetchMarketPrices(List<String> symbols) {
        return marketServiceClient.getBulkQuotes(symbols);
    }
    
    private List<StockQuoteDTO> getDefaultQuotes(List<String> symbols, Exception e) {
        log.warn("Market service unavailable, returning cached/default prices", e);
        return symbols.stream()
            .map(s -> cachedPriceService.getCachedQuote(s))
            .toList();
    }
}
```

---

### 11. Audit Trail & Event Sourcing

**Priority**: 🟢 LOW  
**Impact**: Compliance, Debugging  
**Effort**: Medium (2 days)

#### Current Gap
No audit trail for portfolio changes.

#### Recommendation
Implement event logging for all portfolio mutations.

#### Event Log Table
```sql
CREATE TABLE portfolio_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    portfolio_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_payload JSONB NOT NULL,
    user_id BIGINT NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_portfolio ON portfolio_events(portfolio_id);
CREATE INDEX idx_events_type ON portfolio_events(event_type);
CREATE INDEX idx_events_created ON portfolio_events(created_at);
```

#### Event Types
```java
public enum PortfolioEventType {
    PORTFOLIO_CREATED,
    PORTFOLIO_UPDATED,
    PORTFOLIO_ARCHIVED,
    PORTFOLIO_REACTIVATED,
    HOLDING_ADDED,
    HOLDING_UPDATED,
    HOLDING_REMOVED,
    BUY_EXECUTED,
    SELL_EXECUTED
}
```

---

### 12. Test Coverage Improvements

**Priority**: 🟢 LOW  
**Impact**: Quality Assurance  
**Effort**: Medium (2-3 days)

#### Current Gap
Only 3 unit tests exist. No integration tests, controller tests, or edge case coverage.

#### Recommendations

**Target Coverage**: 80%+

**Test Categories**:
1. Unit Tests (Service layer)
2. Integration Tests (Repository layer with TestContainers)
3. Controller Tests (MockMvc)
4. Edge Case Tests

#### Integration Test Example
```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PortfolioIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void shouldCalculateWeightedAverageOnMultipleBuys() {
        // First buy: 10 shares at ₹100
        // Second buy: 10 shares at ₹120
        // Expected average: ₹110
    }
    
    @Test
    void shouldRejectSellWhenInsufficientShares() {
        // ...
    }
}
```

---

## Frontend Improvements

### Current Issues in Portfolio.jsx

1. **Sequential API calls** for market prices (N+1 problem)
2. **No error boundary** for graceful error handling
3. **No loading skeletons** for better UX
4. **Manual refresh only** - no real-time updates
5. **No virtualization** for long holdings lists

### Recommendations

#### Batch Market Price Fetching
```javascript
const fetchMarketPrices = async (holdings) => {
    const symbols = holdings.map(h => h.symbol);
    try {
        const response = await fetch('/api/v1/market/stocks/bulk', {
            method: 'POST',
            body: JSON.stringify(symbols),
            headers: getAuthHeaders()
        });
        const data = await response.json();
        const priceMap = {};
        data.forEach(quote => {
            priceMap[quote.symbol] = {
                ltp: quote.lastPrice,
                change: quote.change,
                pChange: quote.pChange
            };
        });
        setMarketPrices(priceMap);
    } catch (err) {
        console.error('Failed to fetch market prices:', err);
    }
};
```

#### WebSocket Integration
```javascript
useEffect(() => {
    if (!isAuthenticated) return;
    
    const ws = new WebSocket(`${WS_URL}/portfolio`);
    
    ws.onopen = () => {
        ws.send(JSON.stringify({ type: 'SUBSCRIBE', userId }));
    };
    
    ws.onmessage = (event) => {
        const update = JSON.parse(event.data);
        if (update.type === 'PRICE_UPDATE') {
            setMarketPrices(prev => ({
                ...prev,
                [update.symbol]: update.price
            }));
        }
    };
    
    return () => ws.close();
}, [isAuthenticated, userId]);
```

---

## Priority Matrix

| Priority | Improvement | Impact | Effort | ROI | Status |
|----------|-------------|--------|--------|-----|--------|
| 🔴 Critical | Real-time WebSocket updates | High | Medium | ⭐⭐⭐⭐⭐ | ✅ Done |
| 🔴 Critical | Transaction history | High | Medium | ⭐⭐⭐⭐⭐ | ⚠️ Exists in trade-service/ledger-service |
| 🟠 High | Portfolio analytics | High | High | ⭐⭐⭐⭐ | ⏳ Pending |
| 🟠 High | Redis caching | High | Low | ⭐⭐⭐⭐⭐ | ⏳ Pending |
| 🟡 Medium | Market data integration | Medium | Medium | ⭐⭐⭐⭐ | ✅ Done |
| 🟡 Medium | Multiple portfolios | Medium | Medium | ⭐⭐⭐ | ✅ Done |
| 🟡 Medium | Portfolio snapshots | Medium | Medium | ⭐⭐⭐⭐ | ⏳ Pending |
| 🟡 Medium | Enhanced error handling | Medium | Low | ⭐⭐⭐⭐ | ⏳ Pending |
| 🟡 Medium | Pagination | Low | Low | ⭐⭐⭐⭐ | ⏳ Pending |
| 🟢 Low | Circuit breaker | Medium | Low | ⭐⭐⭐ | ⏳ Pending |
| 🟢 Low | Audit trail | Low | Medium | ⭐⭐⭐ | ⚠️ Exists in ledger-service |
| 🟢 Low | Test coverage | Medium | Medium | ⭐⭐⭐ | ⏳ Pending |

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [x] ~~WebSocket support~~ ✅ Completed 2026-01-28
- [x] ~~Transaction history~~ ⚠️ Already exists in `trade-service` + `ledger-service`
- [ ] Redis caching
- [ ] Enhanced error handling

### Phase 2: Real-Time Features (Week 3-4)
- [x] ~~WebSocket support~~ ✅ (moved from Phase 2, completed early)
- [x] ~~Frontend WebSocket integration~~ ✅ Completed 2026-01-28
- [x] ~~Market data integration~~ ✅ Completed 2026-01-28
- [ ] Real-time price broadcasting (backend trigger needed)

### Phase 3: Analytics & History (Week 5-6)
- [ ] Portfolio snapshots
- [ ] Performance analytics (XIRR, sector allocation)
- [ ] Historical charts


### Phase 4: Advanced Features (Week 7-8)
- [x] ~~Multiple portfolios~~ ✅ Completed 2026-01-28
- [ ] Pagination
- [ ] Circuit breaker
- [ ] Audit trail

### Phase 5: Quality (Week 9)
- [ ] Test coverage improvement
- [ ] Performance optimization
- [ ] Documentation

---

## Related Services

When implementing these improvements, coordinate with:

| Service | Integration Point |
|---------|------------------|
| `market-service` | Real-time prices, bulk quotes |
| `order-service` | Trade execution, order history |
| `notification-service` | Portfolio alerts, P&L notifications |
| `user-service` | User preferences, settings |
| `schedule-service` | Snapshot scheduling |

---

## References

- [Current Portfolio Service](file:///e:/winvestco-trading-platform/backend/portfolio-service)
- [Portfolio Controller](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/controller/PortfolioController.java)
- [Portfolio Service](file:///e:/winvestco-trading-platform/backend/portfolio-service/src/main/java/in/winvestco/portfolio_service/service/PortfolioService.java)
- [Frontend Portfolio Page](file:///e:/winvestco-trading-platform/frontend/src/pages/Portfolio.jsx)

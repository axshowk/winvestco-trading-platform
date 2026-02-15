package in.winvestco.trade_service.client;

import in.winvestco.common.grpc.market.MarketDataServiceGrpc;
import in.winvestco.common.grpc.market.QuoteRequest;
import in.winvestco.common.grpc.market.QuoteResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client for fetching market data from market-service.
 *
 * Replaces the synchronous Feign REST call in MockExecutionEngine
 * with a faster binary gRPC unary call.
 *
 * Uses grpc-spring-boot-starter's @GrpcClient for auto-configuration
 * and service discovery via Eureka.
 *
 * All calls use a 3-second deadline to prevent indefinite blocking
 * if market-service is unresponsive.
 */
@Component
@Slf4j
public class MarketDataGrpcClient {

    private static final long DEADLINE_SECONDS = 3;

    @GrpcClient("market-service")
    private MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataStub;

    /**
     * Get current market price for a stock symbol via gRPC.
     *
     * @param symbol Stock symbol (e.g., "RELIANCE", "TCS")
     * @return The last traded price, or null if not found or error
     */
    public BigDecimal getQuote(String symbol) {
        try {
            log.debug("gRPC GetQuote request for symbol: {}", symbol);

            QuoteResponse response = marketDataStub
                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getQuote(
                            QuoteRequest.newBuilder()
                                    .setSymbol(symbol)
                                    .build());

            if (response.getFound()) {
                BigDecimal price = BigDecimal.valueOf(response.getQuote().getLastPrice());
                log.debug("gRPC GetQuote response for {}: {}", symbol, price);
                return price;
            }

            log.warn("gRPC: No quote found for symbol: {}", symbol);
            return null;

        } catch (Exception e) {
            log.warn("gRPC GetQuote failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }
}

package in.winvestco.marketservice.grpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.grpc.market.MarketDataServiceGrpc;
import in.winvestco.common.grpc.market.MarketDataSubscription;
import in.winvestco.common.grpc.market.MarketDataUpdate;
import in.winvestco.common.grpc.market.QuoteRequest;
import in.winvestco.common.grpc.market.QuoteResponse;
import in.winvestco.marketservice.service.MarketDataService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * gRPC server implementation for market data streaming.
 * 
 * Provides:
 * - GetQuote: Fast unary RPC for fetching current stock price (replaces
 * REST/Feign)
 * - SubscribeMarketData: Server-streaming RPC for real-time price updates
 */
@GrpcService
@Slf4j
public class MarketDataGrpcService extends MarketDataServiceGrpc.MarketDataServiceImplBase {

    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;

    // Symbol → Set of active stream observers
    private final ConcurrentHashMap<String, Set<StreamObserver<MarketDataUpdate>>> symbolSubscribers = new ConcurrentHashMap<>();

    // Observers subscribed to ALL symbols
    private final Set<StreamObserver<MarketDataUpdate>> allSymbolSubscribers = new CopyOnWriteArraySet<>();

    public MarketDataGrpcService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== Unary RPC: GetQuote ====================

    @Override
    public void getQuote(QuoteRequest request, StreamObserver<QuoteResponse> responseObserver) {
        String symbol = request.getSymbol();
        log.debug("gRPC GetQuote request for symbol: {}", symbol);

        try {
            String quoteJson = marketDataService.getStockQuote(symbol);

            if (quoteJson == null || quoteJson.isEmpty()) {
                responseObserver.onNext(QuoteResponse.newBuilder()
                        .setFound(false)
                        .build());
                responseObserver.onCompleted();
                return;
            }

            JsonNode stockNode = objectMapper.readTree(quoteJson);
            MarketDataUpdate update = jsonNodeToUpdate(stockNode, symbol);

            responseObserver.onNext(QuoteResponse.newBuilder()
                    .setQuote(update)
                    .setFound(true)
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing GetQuote for symbol: {}", symbol, e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to fetch quote for " + symbol)
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    // ==================== Server-Streaming RPC: SubscribeMarketData
    // ====================

    @Override
    public void subscribeMarketData(MarketDataSubscription request,
            StreamObserver<MarketDataUpdate> responseObserver) {
        if (request.getSubscribeAll()) {
            log.info("gRPC client subscribed to ALL market data updates");
            allSymbolSubscribers.add(responseObserver);
        } else {
            for (String symbol : request.getSymbolsList()) {
                String upperSymbol = symbol.toUpperCase();
                log.info("gRPC client subscribed to market data for: {}", upperSymbol);
                symbolSubscribers
                        .computeIfAbsent(upperSymbol, k -> new CopyOnWriteArraySet<>())
                        .add(responseObserver);
            }
        }

        // Send initial snapshot for subscribed symbols
        if (!request.getSubscribeAll()) {
            for (String symbol : request.getSymbolsList()) {
                sendCurrentQuote(symbol.toUpperCase(), responseObserver);
            }
        }

        // Note: Stream stays open. The observer will be removed when the client
        // disconnects
        // or when pushUpdate detects a broken stream.
    }

    // ==================== Push Updates (called by scheduler) ====================

    /**
     * Push market data updates to all subscribers watching a specific symbol.
     * Called by MarketDataScheduler after fresh data is fetched from NSE.
     */
    public void pushUpdate(String symbol, MarketDataUpdate update) {
        String upperSymbol = symbol.toUpperCase();

        // Push to symbol-specific subscribers
        Set<StreamObserver<MarketDataUpdate>> observers = symbolSubscribers.get(upperSymbol);
        if (observers != null && !observers.isEmpty()) {
            pushToObservers(observers, update, upperSymbol);
        }

        // Push to all-symbol subscribers
        if (!allSymbolSubscribers.isEmpty()) {
            pushToObservers(allSymbolSubscribers, update, upperSymbol);
        }
    }

    /**
     * Push updates for all stocks in an index response.
     * Parses the index JSON and creates MarketDataUpdate for each constituent
     * stock.
     */
    public void pushUpdatesForIndex(String indexName, String indexJson) {
        try {
            JsonNode root = objectMapper.readTree(indexJson);
            JsonNode dataArray = root.path("data");

            if (!dataArray.isArray()) {
                return;
            }

            int pushedCount = 0;
            for (JsonNode stockNode : dataArray) {
                String symbol = stockNode.path("symbol").asText();
                if (symbol != null && !symbol.isEmpty() && !symbol.startsWith("NIFTY")) {
                    MarketDataUpdate update = jsonNodeToUpdate(stockNode, symbol);
                    pushUpdate(symbol, update);
                    pushedCount++;
                }
            }

            if (pushedCount > 0) {
                log.debug("Pushed gRPC updates for {} stocks from index: {}", pushedCount, indexName);
            }

        } catch (Exception e) {
            log.warn("Error pushing gRPC updates for index {}: {}", indexName, e.getMessage());
        }
    }

    /**
     * Get the count of active subscribers (useful for monitoring).
     */
    public int getActiveSubscriberCount() {
        int count = allSymbolSubscribers.size();
        for (Set<StreamObserver<MarketDataUpdate>> observers : symbolSubscribers.values()) {
            count += observers.size();
        }
        return count;
    }

    // ==================== Helper Methods ====================

    private void pushToObservers(Set<StreamObserver<MarketDataUpdate>> observers,
            MarketDataUpdate update, String symbol) {
        for (StreamObserver<MarketDataUpdate> observer : observers) {
            try {
                observer.onNext(update);
            } catch (Exception e) {
                log.debug("Removing disconnected gRPC subscriber for symbol: {}", symbol);
                observers.remove(observer);
                // Also remove from allSymbolSubscribers if present
                allSymbolSubscribers.remove(observer);
            }
        }
    }

    private void sendCurrentQuote(String symbol, StreamObserver<MarketDataUpdate> observer) {
        try {
            String quoteJson = marketDataService.getStockQuote(symbol);
            if (quoteJson != null) {
                JsonNode stockNode = objectMapper.readTree(quoteJson);
                MarketDataUpdate update = jsonNodeToUpdate(stockNode, symbol);
                observer.onNext(update);
            }
        } catch (Exception e) {
            log.warn("Failed to send initial snapshot for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Convert a JSON stock node (from NSE API response) to a MarketDataUpdate
     * protobuf message.
     */
    MarketDataUpdate jsonNodeToUpdate(JsonNode stockNode, String symbol) {
        return MarketDataUpdate.newBuilder()
                .setSymbol(symbol.toUpperCase())
                .setLastPrice(getDouble(stockNode, "lastPrice"))
                .setOpen(getDouble(stockNode, "open"))
                .setHigh(getDouble(stockNode, "dayHigh"))
                .setLow(getDouble(stockNode, "dayLow"))
                .setClose(getDouble(stockNode, "previousClose"))
                .setChange(getDouble(stockNode, "change"))
                .setChangePercent(getDouble(stockNode, "pChange"))
                .setVolume(getLong(stockNode, "totalTradedVolume"))
                .setTimestamp(System.currentTimeMillis())
                .setExchange("NSE")
                .build();
    }

    private double getDouble(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isMissingNode() && !value.isNull()) {
            try {
                String text = value.asText().replace(",", "");
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private long getLong(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isMissingNode() && !value.isNull()) {
            try {
                return value.asLong();
            } catch (Exception e) {
                return 0L;
            }
        }
        return 0L;
    }
}

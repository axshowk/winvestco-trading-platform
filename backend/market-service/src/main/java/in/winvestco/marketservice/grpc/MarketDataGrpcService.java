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
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC server implementation for market data streaming.
 *
 * Provides:
 * - GetQuote: Fast unary RPC for fetching current stock price
 * - SubscribeMarketData: Server-streaming RPC for real-time price updates
 * - Uses ConcurrentHashMap.newKeySet() instead of CopyOnWriteArraySet for O(1)
 * add/remove
 * - Proactive subscriber cleanup via
 * ServerCallStreamObserver.setOnCancelHandler()
 * - Accepts pre-parsed JsonNode to avoid redundant JSON parsing in the hot path
 */
@GrpcService
@Slf4j
public class MarketDataGrpcService extends MarketDataServiceGrpc.MarketDataServiceImplBase {

    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;

    // ConcurrentHashMap.newKeySet() — O(1) add/remove, no array copies on mutation
    private final ConcurrentHashMap<String, Set<StreamObserver<MarketDataUpdate>>> symbolSubscribers = new ConcurrentHashMap<>();

    private final Set<StreamObserver<MarketDataUpdate>> allSymbolSubscribers = ConcurrentHashMap.newKeySet();

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

        // Register proactive cleanup when client disconnects or cancels
        if (responseObserver instanceof ServerCallStreamObserver) {
            ServerCallStreamObserver<MarketDataUpdate> serverObserver = (ServerCallStreamObserver<MarketDataUpdate>) responseObserver;

            serverObserver.setOnCancelHandler(() -> {
                log.info("gRPC client cancelled subscription, cleaning up observer");
                removeObserverFromAll(responseObserver);
            });
        }

        if (request.getSubscribeAll()) {
            log.info("gRPC client subscribed to ALL market data updates");
            allSymbolSubscribers.add(responseObserver);
        } else {
            for (String symbol : request.getSymbolsList()) {
                String upperSymbol = symbol.toUpperCase();
                log.info("gRPC client subscribed to market data for: {}", upperSymbol);
                symbolSubscribers
                        .computeIfAbsent(upperSymbol, k -> ConcurrentHashMap.newKeySet())
                        .add(responseObserver);
            }
        }

        // Send initial snapshot for subscribed symbols
        if (!request.getSubscribeAll()) {
            for (String symbol : request.getSymbolsList()) {
                sendCurrentQuote(symbol.toUpperCase(), responseObserver);
            }
        }
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
     * Push updates from a pre-parsed JsonNode to avoid re-parsing JSON.
     * The scheduler parses JSON once and passes the JsonNode directly.
     */
    public void pushUpdatesFromParsedIndex(String indexName, JsonNode root) {
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

    /**
     * Remove an observer from ALL subscriber sets.
     * Called by the onCancel handler when a client disconnects.
     */
    private void removeObserverFromAll(StreamObserver<MarketDataUpdate> observer) {
        allSymbolSubscribers.remove(observer);
        for (Set<StreamObserver<MarketDataUpdate>> observers : symbolSubscribers.values()) {
            observers.remove(observer);
        }
    }

    private void pushToObservers(Set<StreamObserver<MarketDataUpdate>> observers,
            MarketDataUpdate update, String symbol) {
        for (StreamObserver<MarketDataUpdate> observer : observers) {
            try {
                observer.onNext(update);
            } catch (Exception e) {
                log.debug("Removing disconnected gRPC subscriber for symbol: {}", symbol);
                observers.remove(observer);
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

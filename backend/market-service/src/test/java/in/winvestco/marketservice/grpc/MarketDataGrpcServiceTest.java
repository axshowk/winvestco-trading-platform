package in.winvestco.marketservice.grpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.grpc.market.MarketDataUpdate;
import in.winvestco.common.grpc.market.MarketDataSubscription;
import in.winvestco.common.grpc.market.QuoteRequest;
import in.winvestco.common.grpc.market.QuoteResponse;
import in.winvestco.marketservice.service.MarketDataService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketDataGrpcService.
 * Tests the gRPC server logic for GetQuote and SubscribeMarketData RPCs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataGrpcService Tests")
class MarketDataGrpcServiceTest {

    @Mock
    private MarketDataService marketDataService;

    private MarketDataGrpcService grpcService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        grpcService = new MarketDataGrpcService(marketDataService);
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GetQuote RPC Tests")
    class GetQuoteTests {

        @Test
        @DisplayName("Should return quote when stock exists")
        @SuppressWarnings("unchecked")
        void getQuote_WhenStockExists_ShouldReturnQuote() {
            String stockJson = "{\"symbol\":\"RELIANCE\",\"lastPrice\":2500.50,\"open\":2450.0," +
                    "\"dayHigh\":2520.0,\"dayLow\":2440.0,\"previousClose\":2460.0," +
                    "\"change\":40.50,\"pChange\":1.65,\"totalTradedVolume\":5000000}";

            when(marketDataService.getStockQuote("RELIANCE")).thenReturn(stockJson);

            StreamObserver<QuoteResponse> responseObserver = mock(StreamObserver.class);
            QuoteRequest request = QuoteRequest.newBuilder().setSymbol("RELIANCE").build();

            grpcService.getQuote(request, responseObserver);

            ArgumentCaptor<QuoteResponse> captor = ArgumentCaptor.forClass(QuoteResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();
            verify(responseObserver, never()).onError(any());

            QuoteResponse response = captor.getValue();
            assertThat(response.getFound()).isTrue();
            assertThat(response.getQuote().getSymbol()).isEqualTo("RELIANCE");
            assertThat(response.getQuote().getLastPrice()).isEqualTo(2500.50);
            assertThat(response.getQuote().getOpen()).isEqualTo(2450.0);
            assertThat(response.getQuote().getExchange()).isEqualTo("NSE");
        }

        @Test
        @DisplayName("Should return not-found when stock doesn't exist")
        @SuppressWarnings("unchecked")
        void getQuote_WhenStockNotFound_ShouldReturnNotFound() {
            when(marketDataService.getStockQuote("NONEXISTENT")).thenReturn(null);

            StreamObserver<QuoteResponse> responseObserver = mock(StreamObserver.class);
            QuoteRequest request = QuoteRequest.newBuilder().setSymbol("NONEXISTENT").build();

            grpcService.getQuote(request, responseObserver);

            ArgumentCaptor<QuoteResponse> captor = ArgumentCaptor.forClass(QuoteResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();
            verify(responseObserver, never()).onError(any());

            QuoteResponse response = captor.getValue();
            assertThat(response.getFound()).isFalse();
        }

        @Test
        @DisplayName("Should return not-found when stock quote is empty")
        @SuppressWarnings("unchecked")
        void getQuote_WhenEmptyQuote_ShouldReturnNotFound() {
            when(marketDataService.getStockQuote("EMPTY")).thenReturn("");

            StreamObserver<QuoteResponse> responseObserver = mock(StreamObserver.class);
            QuoteRequest request = QuoteRequest.newBuilder().setSymbol("EMPTY").build();

            grpcService.getQuote(request, responseObserver);

            ArgumentCaptor<QuoteResponse> captor = ArgumentCaptor.forClass(QuoteResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();

            assertThat(captor.getValue().getFound()).isFalse();
        }
    }

    @Nested
    @DisplayName("SubscribeMarketData RPC Tests")
    class SubscribeTests {

        @Test
        @DisplayName("Should register subscriber and send initial snapshot")
        @SuppressWarnings("unchecked")
        void subscribe_ShouldRegisterAndSendSnapshot() {
            String stockJson = "{\"symbol\":\"TCS\",\"lastPrice\":3500.0,\"open\":3480.0," +
                    "\"dayHigh\":3520.0,\"dayLow\":3470.0,\"previousClose\":3490.0," +
                    "\"change\":10.0,\"pChange\":0.29,\"totalTradedVolume\":2000000}";
            when(marketDataService.getStockQuote("TCS")).thenReturn(stockJson);

            StreamObserver<MarketDataUpdate> observer = mock(StreamObserver.class);
            MarketDataSubscription request = MarketDataSubscription.newBuilder()
                    .addSymbols("TCS")
                    .build();

            grpcService.subscribeMarketData(request, observer);

            // Should send initial snapshot
            verify(observer, atLeastOnce()).onNext(any(MarketDataUpdate.class));

            // Verify subscriber count
            assertThat(grpcService.getActiveSubscriberCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should deliver updates to subscribers via pushUpdate")
        @SuppressWarnings("unchecked")
        void pushUpdate_ShouldDeliverToSubscribers() {
            // No initial snapshot data
            when(marketDataService.getStockQuote("INFY")).thenReturn(null);

            StreamObserver<MarketDataUpdate> observer = mock(StreamObserver.class);
            MarketDataSubscription request = MarketDataSubscription.newBuilder()
                    .addSymbols("INFY")
                    .build();

            grpcService.subscribeMarketData(request, observer);

            // Push an update
            MarketDataUpdate update = MarketDataUpdate.newBuilder()
                    .setSymbol("INFY")
                    .setLastPrice(1500.0)
                    .setExchange("NSE")
                    .build();

            grpcService.pushUpdate("INFY", update);

            // Verify the update was delivered
            ArgumentCaptor<MarketDataUpdate> captor = ArgumentCaptor.forClass(MarketDataUpdate.class);
            verify(observer, atLeastOnce()).onNext(captor.capture());

            boolean found = captor.getAllValues().stream()
                    .anyMatch(u -> u.getSymbol().equals("INFY") && u.getLastPrice() == 1500.0);
            assertThat(found).isTrue();
        }

        @Test
        @DisplayName("Should not deliver updates for unsubscribed symbols")
        @SuppressWarnings("unchecked")
        void pushUpdate_ShouldNotDeliverToWrongSymbol() {
            when(marketDataService.getStockQuote("RELIANCE")).thenReturn(null);

            StreamObserver<MarketDataUpdate> observer = mock(StreamObserver.class);
            MarketDataSubscription request = MarketDataSubscription.newBuilder()
                    .addSymbols("RELIANCE")
                    .build();

            grpcService.subscribeMarketData(request, observer);

            // Push update for a different symbol
            MarketDataUpdate update = MarketDataUpdate.newBuilder()
                    .setSymbol("TCS")
                    .setLastPrice(3500.0)
                    .build();

            grpcService.pushUpdate("TCS", update);

            // Verify observer was NOT called with this update (only initial snapshot call if any)
            verify(observer, never()).onNext(argThat(u -> u.getSymbol().equals("TCS")));
        }
    }

    @Nested
    @DisplayName("PushUpdatesFromParsedIndex Tests")
    class PushUpdatesFromParsedIndexTests {

        @Test
        @DisplayName("Should push updates for each stock from pre-parsed index JsonNode")
        @SuppressWarnings("unchecked")
        void pushUpdatesFromParsedIndex_ShouldPushToSubscribers() throws Exception {
            // Subscribe to RELIANCE
            when(marketDataService.getStockQuote("RELIANCE")).thenReturn(null);

            StreamObserver<MarketDataUpdate> observer = mock(StreamObserver.class);
            MarketDataSubscription request = MarketDataSubscription.newBuilder()
                    .addSymbols("RELIANCE")
                    .build();
            grpcService.subscribeMarketData(request, observer);

            // Pre-parse index data (simulates what scheduler now does)
            String indexJson = "{\"data\":[" +
                    "{\"symbol\":\"NIFTY 50\",\"lastPrice\":22000}," +
                    "{\"symbol\":\"RELIANCE\",\"lastPrice\":2500.50,\"open\":2450,\"dayHigh\":2520," +
                    "\"dayLow\":2440,\"previousClose\":2460,\"change\":40.5,\"pChange\":1.65," +
                    "\"totalTradedVolume\":5000000}," +
                    "{\"symbol\":\"TCS\",\"lastPrice\":3500}" +
                    "]}";
            JsonNode parsedRoot = objectMapper.readTree(indexJson);

            grpcService.pushUpdatesFromParsedIndex("NIFTY 50", parsedRoot);

            // Verify RELIANCE update was delivered
            ArgumentCaptor<MarketDataUpdate> captor = ArgumentCaptor.forClass(MarketDataUpdate.class);
            verify(observer, atLeastOnce()).onNext(captor.capture());

            boolean relianceFound = captor.getAllValues().stream()
                    .anyMatch(u -> u.getSymbol().equals("RELIANCE") && u.getLastPrice() == 2500.50);
            assertThat(relianceFound).isTrue();
        }

        @Test
        @DisplayName("Should handle empty data array gracefully")
        @SuppressWarnings("unchecked")
        void pushUpdatesFromParsedIndex_EmptyData_ShouldNotThrow() throws Exception {
            JsonNode emptyRoot = objectMapper.readTree("{\"data\":[]}");
            // Should not throw
            grpcService.pushUpdatesFromParsedIndex("NIFTY 50", emptyRoot);
        }
    }

    @Nested
    @DisplayName("JsonNodeToUpdate Tests")
    class JsonNodeToUpdateTests {

        @Test
        @DisplayName("Should correctly map JSON fields to MarketDataUpdate")
        void jsonNodeToUpdate_ShouldMapCorrectly() throws Exception {
            String json = "{\"symbol\":\"HDFC\",\"lastPrice\":2800.75,\"open\":2790," +
                    "\"dayHigh\":2810,\"dayLow\":2780,\"previousClose\":2795," +
                    "\"change\":5.75,\"pChange\":0.21,\"totalTradedVolume\":3500000}";
            JsonNode node = objectMapper.readTree(json);

            MarketDataUpdate update = grpcService.jsonNodeToUpdate(node, "HDFC");

            assertThat(update.getSymbol()).isEqualTo("HDFC");
            assertThat(update.getLastPrice()).isEqualTo(2800.75);
            assertThat(update.getOpen()).isEqualTo(2790.0);
            assertThat(update.getHigh()).isEqualTo(2810.0);
            assertThat(update.getLow()).isEqualTo(2780.0);
            assertThat(update.getClose()).isEqualTo(2795.0);
            assertThat(update.getChange()).isEqualTo(5.75);
            assertThat(update.getChangePercent()).isEqualTo(0.21);
            assertThat(update.getVolume()).isEqualTo(3500000L);
            assertThat(update.getExchange()).isEqualTo("NSE");
            assertThat(update.getTimestamp()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle missing fields gracefully with defaults")
        void jsonNodeToUpdate_MissingFields_ShouldUseDefaults() throws Exception {
            String json = "{\"symbol\":\"UNKNOWN\"}";
            JsonNode node = objectMapper.readTree(json);

            MarketDataUpdate update = grpcService.jsonNodeToUpdate(node, "UNKNOWN");

            assertThat(update.getSymbol()).isEqualTo("UNKNOWN");
            assertThat(update.getLastPrice()).isEqualTo(0.0);
            assertThat(update.getVolume()).isEqualTo(0L);
        }
    }
}

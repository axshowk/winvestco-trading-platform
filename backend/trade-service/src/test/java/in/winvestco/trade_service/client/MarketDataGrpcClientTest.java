package in.winvestco.trade_service.client;

import in.winvestco.common.grpc.market.MarketDataServiceGrpc;
import in.winvestco.common.grpc.market.MarketDataUpdate;
import in.winvestco.common.grpc.market.QuoteRequest;
import in.winvestco.common.grpc.market.QuoteResponse;
import io.grpc.StatusRuntimeException;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MarketDataGrpcClient.
 * Tests the gRPC client wrapper for market data quote lookups.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataGrpcClient Tests")
class MarketDataGrpcClientTest {

    @Mock
    private MarketDataServiceGrpc.MarketDataServiceBlockingStub marketDataStub;

    private MarketDataGrpcClient grpcClient;

    @BeforeEach
    void setUp() {
        grpcClient = new MarketDataGrpcClient();
        ReflectionTestUtils.setField(grpcClient, "marketDataStub", marketDataStub);
    }

    @Test
    @DisplayName("Should return price when gRPC call succeeds and stock is found")
    void getQuote_WhenFound_ShouldReturnPrice() {
        MarketDataUpdate update = MarketDataUpdate.newBuilder()
                .setSymbol("RELIANCE")
                .setLastPrice(2500.50)
                .build();

        QuoteResponse response = QuoteResponse.newBuilder()
                .setQuote(update)
                .setFound(true)
                .build();

        when(marketDataStub.getQuote(any(QuoteRequest.class))).thenReturn(response);

        BigDecimal result = grpcClient.getQuote("RELIANCE");

        assertThat(result).isNotNull();
        assertThat(result.doubleValue()).isEqualTo(2500.50);
    }

    @Test
    @DisplayName("Should return null when stock is not found")
    void getQuote_WhenNotFound_ShouldReturnNull() {
        QuoteResponse response = QuoteResponse.newBuilder()
                .setFound(false)
                .build();

        when(marketDataStub.getQuote(any(QuoteRequest.class))).thenReturn(response);

        BigDecimal result = grpcClient.getQuote("NONEXISTENT");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when gRPC call throws exception")
    void getQuote_WhenError_ShouldReturnNull() {
        when(marketDataStub.getQuote(any(QuoteRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        BigDecimal result = grpcClient.getQuote("RELIANCE");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when gRPC call throws internal error")
    void getQuote_WhenInternalError_ShouldReturnNull() {
        when(marketDataStub.getQuote(any(QuoteRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.INTERNAL.withDescription("server error")));

        BigDecimal result = grpcClient.getQuote("TCS");

        assertThat(result).isNull();
    }
}

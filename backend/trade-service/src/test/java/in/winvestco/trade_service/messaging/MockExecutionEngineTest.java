package in.winvestco.trade_service.messaging;

import com.rabbitmq.client.Channel;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.TradeStatus;
import in.winvestco.common.event.TradePlacedEvent;
import in.winvestco.trade_service.client.MarketServiceClient;
import in.winvestco.trade_service.config.MockExecutionProperties;
import in.winvestco.trade_service.dto.TradeDTO;
import in.winvestco.trade_service.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MockExecutionEngine.
 */
@ExtendWith(MockitoExtension.class)
class MockExecutionEngineTest {

    @Mock
    private TradeService tradeService;

    @Mock
    private MarketServiceClient marketServiceClient;

    @Mock
    private Channel channel;

    @InjectMocks
    private MockExecutionEngine mockExecutionEngine;

    private MockExecutionProperties properties;
    private TradePlacedEvent testEvent;

    @BeforeEach
    void setUp() {
        // Set up properties
        properties = new MockExecutionProperties();
        properties.setEnabled(true);
        properties.setMinDelayMs(1); // Very low for fast tests
        properties.setMaxDelayMs(2);
        properties.setMaxSlippagePercent(0.1);
        properties.setAutoClose(true);
        properties.setPartialFillEnabled(true);
        properties.setPartialFillPercent(70);

        // Inject properties into engine
        ReflectionTestUtils.setField(mockExecutionEngine, "properties", properties);

        // Set up test event
        testEvent = TradePlacedEvent.builder()
                .tradeId("TRADE-001")
                .orderId("ORDER-001")
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .quantity(BigDecimal.TEN)
                .price(new BigDecimal("2500.00"))
                .status(TradeStatus.PLACED)
                .placedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should execute trade when mock execution is enabled")
    void shouldExecuteTradeWhenEnabled() throws Exception {
        // Given
        properties.setPartialFillEnabled(false);
        when(marketServiceClient.getStockQuote("RELIANCE")).thenReturn(
                "{\"symbol\":\"RELIANCE\",\"lastPrice\":\"2505.50\"}");
        when(tradeService.handleExecutionUpdate(anyString(), any(), any(), anyBoolean()))
                .thenReturn(mockTradeDTO());
        when(tradeService.closeTrade(anyString())).thenReturn(mockTradeDTO());

        // When
        mockExecutionEngine.asyncExecuteTrade(testEvent);

        // Then
        verify(tradeService).handleExecutionUpdate(eq("TRADE-001"), eq(BigDecimal.TEN), any(BigDecimal.class),
                eq(false));
        verify(tradeService).closeTrade("TRADE-001");
    }

    @Test
    @DisplayName("Should not execute trade when mock execution is disabled")
    void shouldNotExecuteWhenDisabled() throws IOException {
        // Given
        properties.setEnabled(false);

        // When
        mockExecutionEngine.handleTradePlaced(testEvent, channel, 1L);

        // Then
        verify(tradeService, never()).handleExecutionUpdate(anyString(), any(), any(), anyBoolean());
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Should apply slippage to execution price")
    void shouldApplySlippage() throws Exception {
        // Given
        properties.setPartialFillEnabled(false);
        properties.setMaxSlippagePercent(1.0); // 1% max slippage for visibility
        when(marketServiceClient.getStockQuote("RELIANCE")).thenReturn(
                "{\"symbol\":\"RELIANCE\",\"lastPrice\":\"2500.00\"}");
        when(tradeService.handleExecutionUpdate(anyString(), any(), any(), anyBoolean()))
                .thenReturn(mockTradeDTO());
        when(tradeService.closeTrade(anyString())).thenReturn(mockTradeDTO());

        // When
        mockExecutionEngine.asyncExecuteTrade(testEvent);

        // Then
        ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(tradeService).handleExecutionUpdate(eq("TRADE-001"), any(), priceCaptor.capture(), eq(false));

        BigDecimal executionPrice = priceCaptor.getValue();
        BigDecimal marketPrice = new BigDecimal("2500.00");

        // For BUY order, price should be >= market price (slippage adds to price)
        assertThat(executionPrice.compareTo(marketPrice)).isGreaterThanOrEqualTo(0);
        // But not more than 1% higher
        BigDecimal maxPrice = marketPrice.multiply(new BigDecimal("1.01"));
        assertThat(executionPrice.compareTo(maxPrice)).isLessThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should use fallback price when market service unavailable")
    void shouldUseFallbackPriceWhenMarketUnavailable() throws Exception {
        // Given
        properties.setPartialFillEnabled(false);
        when(marketServiceClient.getStockQuote("RELIANCE")).thenReturn(null);
        when(tradeService.handleExecutionUpdate(anyString(), any(), any(), anyBoolean()))
                .thenReturn(mockTradeDTO());
        when(tradeService.closeTrade(anyString())).thenReturn(mockTradeDTO());

        // When
        mockExecutionEngine.asyncExecuteTrade(testEvent);

        // Then
        ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(tradeService).handleExecutionUpdate(eq("TRADE-001"), any(), priceCaptor.capture(), eq(false));

        BigDecimal executionPrice = priceCaptor.getValue();
        BigDecimal orderPrice = new BigDecimal("2500.00");

        // Execution price should be based on order price (with slippage)
        assertThat(executionPrice.compareTo(orderPrice)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should execute partial fills when enabled")
    void shouldExecutePartialFills() throws Exception {
        // Given
        properties.setPartialFillEnabled(true);
        properties.setPartialFillPercent(70);
        when(marketServiceClient.getStockQuote("RELIANCE")).thenReturn(
                "{\"symbol\":\"RELIANCE\",\"lastPrice\":\"2500.00\"}");
        when(tradeService.handleExecutionUpdate(anyString(), any(), any(), anyBoolean()))
                .thenReturn(mockTradeDTO());
        when(tradeService.closeTrade(anyString())).thenReturn(mockTradeDTO());

        // When
        mockExecutionEngine.asyncExecuteTrade(testEvent);

        // Then - should have 2 execution calls (70% + 30%)
        ArgumentCaptor<BigDecimal> qtyCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Boolean> partialCaptor = ArgumentCaptor.forClass(Boolean.class);

        verify(tradeService, times(2)).handleExecutionUpdate(
                eq("TRADE-001"),
                qtyCaptor.capture(),
                any(BigDecimal.class),
                partialCaptor.capture());

        // First call should be partial (70% of 10 = 7)
        assertThat(qtyCaptor.getAllValues().get(0)).isEqualByComparingTo(new BigDecimal("7"));
        assertThat(partialCaptor.getAllValues().get(0)).isTrue();

        // Second call should be full (remaining 30% = 3)
        assertThat(qtyCaptor.getAllValues().get(1)).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(partialCaptor.getAllValues().get(1)).isFalse();

        verify(tradeService).closeTrade("TRADE-001");
    }

    @Test
    @DisplayName("Should not auto-close when autoClose is disabled")
    void shouldNotAutoCloseWhenDisabled() throws Exception {
        // Given
        properties.setPartialFillEnabled(false);
        properties.setAutoClose(false);
        when(marketServiceClient.getStockQuote("RELIANCE")).thenReturn(
                "{\"symbol\":\"RELIANCE\",\"lastPrice\":\"2500.00\"}");
        when(tradeService.handleExecutionUpdate(anyString(), any(), any(), anyBoolean()))
                .thenReturn(mockTradeDTO());

        // When
        mockExecutionEngine.asyncExecuteTrade(testEvent);

        // Then
        verify(tradeService).handleExecutionUpdate(anyString(), any(), any(), anyBoolean());
        verify(tradeService, never()).closeTrade(anyString());
    }

    @Test
    @DisplayName("Should fail trade on execution error")
    void shouldFailTradeOnError() throws Exception {
        // Given
        when(marketServiceClient.getStockQuote("RELIANCE")).thenThrow(new RuntimeException("Service error"));
        when(tradeService.failTrade(anyString(), anyString(), anyString())).thenReturn(mockTradeDTO());

        // When
        mockExecutionEngine.asyncExecuteTrade(testEvent);

        // Then
        verify(tradeService).failTrade(eq("TRADE-001"), contains("Mock execution failed"), eq("EXEC_ERROR"));
    }

    @Test
    @DisplayName("Should apply negative slippage for SELL orders")
    void shouldApplyNegativeSlippageForSellOrders() throws Exception {
        // Given
        testEvent = TradePlacedEvent.builder()
                .tradeId("TRADE-002")
                .orderId("ORDER-002")
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.SELL)
                .quantity(BigDecimal.TEN)
                .price(new BigDecimal("2500.00"))
                .status(TradeStatus.PLACED)
                .placedAt(Instant.now())
                .build();

        properties.setPartialFillEnabled(false);
        properties.setMaxSlippagePercent(1.0); // 1% max slippage
        when(marketServiceClient.getStockQuote("RELIANCE")).thenReturn(
                "{\"symbol\":\"RELIANCE\",\"lastPrice\":\"2500.00\"}");
        when(tradeService.handleExecutionUpdate(anyString(), any(), any(), anyBoolean()))
                .thenReturn(mockTradeDTO());
        when(tradeService.closeTrade(anyString())).thenReturn(mockTradeDTO());

        // When
        mockExecutionEngine.asyncExecuteTrade(testEvent);

        // Then
        ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(tradeService).handleExecutionUpdate(eq("TRADE-002"), any(), priceCaptor.capture(), eq(false));

        BigDecimal executionPrice = priceCaptor.getValue();
        BigDecimal marketPrice = new BigDecimal("2500.00");

        // For SELL order, price should be <= market price (slippage reduces price)
        assertThat(executionPrice.compareTo(marketPrice)).isLessThanOrEqualTo(0);
    }

    private TradeDTO mockTradeDTO() {
        return TradeDTO.builder()
                .tradeId("TRADE-001")
                .orderId("ORDER-001")
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .quantity(BigDecimal.TEN)
                .price(new BigDecimal("2500.00"))
                .executedQuantity(BigDecimal.TEN)
                .averagePrice(new BigDecimal("2500.00"))
                .status(TradeStatus.FILLED)
                .build();
    }
}

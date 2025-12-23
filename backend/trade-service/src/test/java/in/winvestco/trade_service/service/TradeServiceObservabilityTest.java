package in.winvestco.trade_service.service;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.TradeStatus;
import in.winvestco.trade_service.mapper.TradeMapper;
import in.winvestco.trade_service.model.Trade;
import in.winvestco.trade_service.repository.TradeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceObservabilityTest {

    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private TradeMapper tradeMapper;
    @Mock
    private TradeEventPublisher eventPublisher;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private ObservationRegistry observationRegistry;

    @InjectMocks
    private TradeService tradeService;

    @Mock
    private Timer timer;

    @Test
    void handleExecutionUpdate_ShouldRecordLatency() {
        // Arrange
        String tradeId = "trade-1";
        Trade trade = Trade.builder()
                .tradeId(tradeId)
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .status(TradeStatus.PLACED)
                .placedAt(Instant.now().minusSeconds(10))
                .quantity(new BigDecimal("100"))
                .executedQuantity(BigDecimal.ZERO)
                .build();

        when(tradeRepository.findByTradeId(tradeId)).thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);
        when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(timer);

        // Act
        tradeService.handleExecutionUpdate(tradeId, new BigDecimal("100"), new BigDecimal("150"), false);

        // Assert
        verify(meterRegistry).timer(eq("trade.execution.latency"), any(String[].class));
        verify(timer).record(any(java.time.Duration.class));
        verify(observationRegistry).observationConfig();
    }
}

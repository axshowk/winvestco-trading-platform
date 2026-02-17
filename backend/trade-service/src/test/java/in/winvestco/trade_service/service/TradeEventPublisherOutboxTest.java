package in.winvestco.trade_service.service;

import in.winvestco.common.event.TradeCreatedEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.trade_service.model.Trade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeEventPublisherOutboxTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TradeEventPublisher tradeEventPublisher;

    @Test
    void shouldCaptureTradeCreatedEventInOutbox() {
        // Given
        Trade trade = Trade.builder()
                .tradeId("TRD-001")
                .orderId("ORD-001")
                .userId(123L)
                .symbol("AAPL")
                .build();

        // When
        tradeEventPublisher.publishTradeCreated(trade);

        // Then
        verify(outboxService).captureEvent(
                eq("Trade"),
                eq("TRD-001"),
                eq("trade.exchange"),
                eq("trade.created"),
                any(TradeCreatedEvent.class)
        );
    }
}

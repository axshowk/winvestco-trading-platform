package in.winvestco.order_service.messaging;

import in.winvestco.common.event.TradeExecutedEvent;
import in.winvestco.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeEventListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private TradeEventListener tradeEventListener;

    @Test
    void handleTradeExecuted_ShouldCallService() {
        TradeExecutedEvent event = TradeExecutedEvent.builder()
                .orderId("order-123")
                .tradeId("trade-789")
                .executedQuantity(new BigDecimal("10"))
                .executedPrice(new BigDecimal("150.00"))
                .executedAt(Instant.now())
                .isPartialFill(false)
                .build();

        tradeEventListener.handleTradeExecuted(event);

        verify(orderService).handleTradeExecuted(
                "order-123",
                new BigDecimal("10"),
                new BigDecimal("150.00"),
                false
        );
    }
}

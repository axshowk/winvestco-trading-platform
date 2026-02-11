package in.winvestco.order_service.messaging;

import in.winvestco.common.event.FundsLockedEvent;
import in.winvestco.common.event.OrderRejectedEvent;
import in.winvestco.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundsEventListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private FundsEventListener fundsEventListener;

    @Test
    void handleFundsLocked_ShouldCallService() {
        FundsLockedEvent event = FundsLockedEvent.builder()
                .orderId("order-123")
                .lockId("lock-456")
                .lockedAt(Instant.now())
                .build();

        fundsEventListener.handleFundsLocked(event);

        verify(orderService).handleFundsLocked("order-123", "lock-456");
    }

    @Test
    void handleOrderRejected_ShouldCallService() {
        OrderRejectedEvent event = OrderRejectedEvent.builder()
                .orderId("order-123")
                .rejectionReason("Insufficient funds")
                .rejectedBy("FUNDS_SERVICE")
                .rejectedAt(Instant.now())
                .build();

        fundsEventListener.handleOrderRejected(event);

        verify(orderService).handleOrderRejected("order-123", "Insufficient funds");
    }
}

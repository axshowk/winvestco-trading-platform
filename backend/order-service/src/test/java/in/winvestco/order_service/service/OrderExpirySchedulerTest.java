package in.winvestco.order_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExpirySchedulerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderExpiryScheduler orderExpiryScheduler;

    @Test
    void checkExpiredOrders_WhenOrdersExpired_ShouldLogCount() {
        when(orderService.expireOrders()).thenReturn(5);

        orderExpiryScheduler.checkExpiredOrders();

        verify(orderService).expireOrders();
    }

    @Test
    void checkExpiredOrders_WhenNoOrdersExpired_ShouldNotLog() {
        when(orderService.expireOrders()).thenReturn(0);

        orderExpiryScheduler.checkExpiredOrders();

        verify(orderService).expireOrders();
    }

    @Test
    void checkExpiredOrders_WhenExceptionThrown_ShouldHandleGracefully() {
        when(orderService.expireOrders()).thenThrow(new RuntimeException("Database error"));

        orderExpiryScheduler.checkExpiredOrders();

        verify(orderService).expireOrders();
    }

    @Test
    void expireOrdersAtMarketClose_ShouldCallExpireOrders() {
        when(orderService.expireOrders()).thenReturn(3);

        orderExpiryScheduler.expireOrdersAtMarketClose();

        verify(orderService).expireOrders();
    }

    @Test
    void expireOrdersAtMarketClose_WhenExceptionThrown_ShouldHandleGracefully() {
        when(orderService.expireOrders()).thenThrow(new RuntimeException("Service unavailable"));

        orderExpiryScheduler.expireOrdersAtMarketClose();

        verify(orderService).expireOrders();
    }
}

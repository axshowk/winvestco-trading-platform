package in.winvestco.portfolio_service.messaging;

import com.rabbitmq.client.Channel;
import in.winvestco.common.event.UserCreatedEvent;
import in.winvestco.portfolio_service.model.Portfolio;
import in.winvestco.portfolio_service.service.PortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private Channel channel;

    @InjectMocks
    private UserEventListener userEventListener;

    private UserCreatedEvent createEvent(Long userId, String email) {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setUserId(userId);
        event.setEmail(email);
        return event;
    }

    @Test
    @DisplayName("should create portfolio and ack on success")
    void handleUserCreatedEvent_success_shouldCreateAndAck() throws IOException {
        UserCreatedEvent event = createEvent(1L, "user@test.com");
        when(portfolioService.createPortfolioForUser(1L, "user@test.com"))
                .thenReturn(Portfolio.builder().id(1L).userId(1L).build());

        userEventListener.handleUserCreatedEvent(event, channel, 1L);

        verify(portfolioService).createPortfolioForUser(1L, "user@test.com");
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("should nack with requeue on failure")
    void handleUserCreatedEvent_failure_shouldNack() throws IOException {
        UserCreatedEvent event = createEvent(1L, "user@test.com");
        when(portfolioService.createPortfolioForUser(anyLong(), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        userEventListener.handleUserCreatedEvent(event, channel, 1L);

        verify(channel).basicNack(1L, false, true);
    }

    @Test
    @DisplayName("should handle nack failure gracefully")
    void handleUserCreatedEvent_nackFails_shouldNotThrow() throws IOException {
        UserCreatedEvent event = createEvent(1L, "user@test.com");
        when(portfolioService.createPortfolioForUser(anyLong(), anyString()))
                .thenThrow(new RuntimeException("DB error"));
        doThrow(new IOException("Channel closed")).when(channel).basicNack(anyLong(), anyBoolean(), anyBoolean());

        // Should not throw - error is caught and logged
        userEventListener.handleUserCreatedEvent(event, channel, 1L);

        verify(channel).basicNack(1L, false, true);
    }

    @Test
    @DisplayName("should process event with valid data")
    void handleUserCreatedEvent_validData_shouldProcess() throws IOException {
        UserCreatedEvent event = createEvent(42L, "newuser@winvestco.in");
        when(portfolioService.createPortfolioForUser(42L, "newuser@winvestco.in"))
                .thenReturn(Portfolio.builder().id(5L).userId(42L).build());

        userEventListener.handleUserCreatedEvent(event, channel, 99L);

        verify(portfolioService).createPortfolioForUser(42L, "newuser@winvestco.in");
        verify(channel).basicAck(99L, false);
    }
}

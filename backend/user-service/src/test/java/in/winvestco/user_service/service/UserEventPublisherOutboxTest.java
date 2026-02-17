package in.winvestco.user_service.service;

import in.winvestco.common.event.UserCreatedEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventPublisherOutboxTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UserEventPublisher userEventPublisher;

    @Test
    void shouldCaptureUserCreatedEventInOutbox() {
        // Given
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(123L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        // When
        userEventPublisher.publishUserCreated(event);

        // Then
        verify(outboxService).captureEvent(
                eq("User"),
                eq("123"),
                eq("user.exchange"),
                eq("user.created"),
                any(UserCreatedEvent.class)
        );
    }
}

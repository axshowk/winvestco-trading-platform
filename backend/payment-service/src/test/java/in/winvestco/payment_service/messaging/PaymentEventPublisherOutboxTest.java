package in.winvestco.payment_service.messaging;

import in.winvestco.common.event.PaymentCreatedEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.payment_service.model.Payment;
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
class PaymentEventPublisherOutboxTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PaymentEventPublisher paymentEventPublisher;

    @Test
    void shouldCapturePaymentCreatedEventInOutbox() {
        // Given
        Payment payment = Payment.builder()
                .id(456L)
                .userId(123L)
                .amount(new BigDecimal("100.00"))
                .build();

        // When
        paymentEventPublisher.publishPaymentCreated(payment);

        // Then
        verify(outboxService).captureEvent(
                eq("Payment"),
                eq("456"),
                eq("payment.exchange"),
                eq("payment.created"),
                any(PaymentCreatedEvent.class)
        );
    }
}

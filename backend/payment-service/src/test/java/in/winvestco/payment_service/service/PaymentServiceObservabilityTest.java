package in.winvestco.payment_service.service;

import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.enums.PaymentStatus;
import in.winvestco.payment_service.dto.VerifyPaymentRequest;
import in.winvestco.payment_service.mapper.PaymentMapper;
import in.winvestco.payment_service.messaging.PaymentEventPublisher;
import in.winvestco.payment_service.model.Payment;
import in.winvestco.payment_service.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceObservabilityTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RazorpayService razorpayService;
    @Mock
    private PaymentEventPublisher eventPublisher;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private ObservationRegistry observationRegistry;

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private Counter counter;

    @Test
    void verifyPayment_ShouldRecordMetricsAndObservation() {
        // Arrange
        Long userId = 1L;
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order-1");
        request.setRazorpayPaymentId("pay-1");
        request.setRazorpaySignature("sig-1");

        Payment payment = Payment.builder()
                .id(1L)
                .userId(userId)
                .status(PaymentStatus.CREATED)
                .amount(new BigDecimal("100"))
                .razorpayOrderId("order-1")
                .paymentMethod(PaymentMethod.OTHER)
                .build();

        when(paymentRepository.findByRazorpayOrderId("order-1")).thenReturn(Optional.of(payment));
        when(razorpayService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        // Act
        paymentService.verifyPayment(userId, request);

        // Assert
        verify(meterRegistry).counter(eq("payment.success.count"), any(String[].class));
        verify(counter).increment();
        verify(observationRegistry).observationConfig();
    }
}

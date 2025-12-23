package in.winvestco.payment_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import in.winvestco.common.enums.PaymentStatus;
import in.winvestco.payment_service.dto.InitiatePaymentRequest;
import in.winvestco.payment_service.dto.PaymentResponse;
import in.winvestco.payment_service.dto.VerifyPaymentRequest;
import in.winvestco.payment_service.dto.RazorpayOrderResponse;
import in.winvestco.payment_service.mapper.PaymentMapper;
import in.winvestco.payment_service.messaging.PaymentEventPublisher;
import in.winvestco.payment_service.model.Payment;
import in.winvestco.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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

    @Spy
    private ObservationRegistry observationRegistry = ObservationRegistry.create();

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "expiryMinutes", 15);
        ReflectionTestUtils.setField(paymentService, "defaultCurrency", "INR");

        testPayment = Payment.builder()
                .id(1L)
                .userId(1L)
                .walletId(1L)
                .amount(new BigDecimal("1000"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .razorpayOrderId("order_123")
                .build();

        Counter mockCounter = mock(Counter.class);
        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(mockCounter);
    }

    @Test
    void initiatePayment_ShouldCreateOrderAndRecord() {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setAmount(new BigDecimal("1000"));
        request.setWalletId(1L);

        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(razorpayService.createOrder(any(), any(), any())).thenReturn("order_123");

        RazorpayOrderResponse response = paymentService.initiatePayment(1L, request);

        assertNotNull(response);
        assertEquals("order_123", response.getOrderId());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(eventPublisher).publishPaymentCreated(any(Payment.class));
    }

    @Test
    void verifyPayment_WhenValid_ShouldMarkSuccess() {
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("sig_123");

        when(paymentRepository.findByRazorpayOrderId(anyString())).thenReturn(Optional.of(testPayment));
        when(razorpayService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(new PaymentResponse());

        PaymentResponse response = paymentService.verifyPayment(1L, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, testPayment.getStatus());
        verify(eventPublisher).publishPaymentSuccess(testPayment);
    }

    @Test
    void verifyPayment_WhenInvalidOwnership_ShouldThrowException() {
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_123");

        testPayment.setUserId(2L);
        when(paymentRepository.findByRazorpayOrderId(anyString())).thenReturn(Optional.of(testPayment));

        assertThrows(RuntimeException.class, () -> paymentService.verifyPayment(1L, request));
    }
}

package in.winvestco.funds_service.messaging;

import in.winvestco.common.enums.PaymentMethod;
import in.winvestco.common.event.PaymentSuccessEvent;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentEventListener.
 * Tests wallet credit on successful payment.
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

        @Mock
        private WalletService walletService;

        @InjectMocks
        private PaymentEventListener paymentEventListener;

        private PaymentSuccessEvent paymentSuccessEvent;
        private Wallet creditedWallet;

        @BeforeEach
        void setUp() {
                paymentSuccessEvent = PaymentSuccessEvent.builder()
                                .paymentId(123L)
                                .userId(100L)
                                .amount(new BigDecimal("5000.00"))
                                .paymentMethod(PaymentMethod.UPI)
                                .description("Account deposit")
                                .completedAt(Instant.now())
                                .build();

                creditedWallet = Wallet.builder()
                                .id(1L)
                                .userId(100L)
                                .availableBalance(new BigDecimal("15000.00"))
                                .lockedBalance(BigDecimal.ZERO)
                                .currency("INR")
                                .build();
        }

        @Test
        @DisplayName("Should credit wallet on PaymentSuccessEvent")
        void handlePaymentSuccess_ShouldCreditWallet() {
                // Arrange
                when(walletService.creditFunds(eq(100L), eq(new BigDecimal("5000.00")),
                                eq("PAYMENT-PAY-123"), eq("PAYMENT"), anyString()))
                                .thenReturn(creditedWallet);

                // Act
                paymentEventListener.handlePaymentSuccess(paymentSuccessEvent);

                // Assert
                verify(walletService).creditFunds(
                                eq(100L),
                                eq(new BigDecimal("5000.00")),
                                eq("PAYMENT-PAY-123"),
                                eq("PAYMENT"),
                                contains("UPI"));
        }

        @Test
        @DisplayName("Should use default method when payment method is null")
        void handlePaymentSuccess_NullMethod_ShouldUseDefault() {
                // Arrange
                PaymentSuccessEvent eventWithNullMethod = PaymentSuccessEvent.builder()
                                .paymentId(456L)
                                .userId(100L)
                                .amount(new BigDecimal("3000.00"))
                                .paymentMethod(null)
                                .completedAt(Instant.now())
                                .build();

                when(walletService.creditFunds(anyLong(), any(), anyString(), anyString(), anyString()))
                                .thenReturn(creditedWallet);

                // Act
                paymentEventListener.handlePaymentSuccess(eventWithNullMethod);

                // Assert
                verify(walletService).creditFunds(
                                eq(100L),
                                eq(new BigDecimal("3000.00")),
                                anyString(),
                                eq("PAYMENT"),
                                contains("Razorpay")); // Default payment provider
        }

        @Test
        @DisplayName("Should include description in credit description")
        void handlePaymentSuccess_WithDescription_ShouldIncludeInCreditDesc() {
                // Arrange
                when(walletService.creditFunds(anyLong(), any(), anyString(), anyString(), anyString()))
                                .thenReturn(creditedWallet);

                // Act
                paymentEventListener.handlePaymentSuccess(paymentSuccessEvent);

                // Assert
                verify(walletService).creditFunds(
                                anyLong(),
                                any(),
                                anyString(),
                                anyString(),
                                argThat(desc -> desc.contains("Account deposit")));
        }

        @Test
        @DisplayName("Should rethrow exception on credit failure for retry")
        void handlePaymentSuccess_CreditError_ShouldRethrow() {
                // Arrange
                when(walletService.creditFunds(anyLong(), any(), anyString(), anyString(), anyString()))
                                .thenThrow(new RuntimeException("Database error"));

                // Act & Assert
                assertThatThrownBy(() -> paymentEventListener.handlePaymentSuccess(paymentSuccessEvent))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Database error");
        }

        @Test
        @DisplayName("Should use correct reference format")
        void handlePaymentSuccess_ShouldUseCorrectReferenceFormat() {
                // Arrange
                when(walletService.creditFunds(anyLong(), any(), anyString(), anyString(), anyString()))
                                .thenReturn(creditedWallet);

                // Act
                paymentEventListener.handlePaymentSuccess(paymentSuccessEvent);

                // Assert
                verify(walletService).creditFunds(
                                anyLong(),
                                any(),
                                eq("PAYMENT-PAY-123"), // Reference format: PAYMENT-{paymentId}
                                eq("PAYMENT"),
                                anyString());
        }
}

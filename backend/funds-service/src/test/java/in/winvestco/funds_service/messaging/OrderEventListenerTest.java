package in.winvestco.funds_service.messaging;

import in.winvestco.common.enums.LockStatus;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.event.OrderCancelledEvent;
import in.winvestco.common.event.OrderValidatedEvent;
import in.winvestco.common.messaging.idempotency.IdempotencyService;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.model.Wallet;
import in.winvestco.funds_service.repository.WalletRepository;
import in.winvestco.funds_service.service.FundsEventPublisher;
import in.winvestco.funds_service.service.FundsLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderEventListener.
 * Tests funds locking/release on order events.
 */
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private FundsLockService fundsLockService;

    @Mock
    private FundsEventPublisher fundsEventPublisher;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private OrderValidatedEvent validatedEvent;
    private OrderCancelledEvent cancelledEvent;
    private Wallet testWallet;
    private FundsLockDTO testLockDTO;

    @BeforeEach
    void setUp() {
        String correlationId = UUID.randomUUID().toString();

        validatedEvent = OrderValidatedEvent.builder()
                .orderId("ORD-123")
                .userId(100L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500.00"))
                .totalAmount(new BigDecimal("25000.00"))
                .correlationId(correlationId)
                .build();

        cancelledEvent = OrderCancelledEvent.builder()
                .orderId("ORD-123")
                .userId(100L)
                .cancelReason("User requested cancellation")
                .correlationId(correlationId)
                .build();

        testWallet = Wallet.builder()
                .id(1L)
                .userId(100L)
                .availableBalance(new BigDecimal("50000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .build();

        testLockDTO = new FundsLockDTO();
        testLockDTO.setId(1L);
        testLockDTO.setOrderId("ORD-123");
        testLockDTO.setAmount(new BigDecimal("25000.00"));
        testLockDTO.setStatus(LockStatus.LOCKED);
    }

    @Nested
    @DisplayName("handleOrderValidated tests")
    class HandleOrderValidatedTests {

        @Test
        @DisplayName("Should lock funds on OrderValidatedEvent")
        void handleOrderValidated_SufficientFunds_ShouldLock() {
            // Arrange
            when(idempotencyService.exists(anyString())).thenReturn(false);
            when(fundsLockService.lockFunds(eq(100L), eq("ORD-123"), eq(new BigDecimal("25000.00")), anyString()))
                    .thenReturn(testLockDTO);
            when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(testWallet));

            // Act
            orderEventListener.handleOrderValidated(validatedEvent);

            // Assert
            verify(fundsLockService).lockFunds(eq(100L), eq("ORD-123"), eq(new BigDecimal("25000.00")), anyString());
            verify(fundsEventPublisher).publishFundsLockedWithDetails(
                    eq(100L), eq(testWallet), eq(testLockDTO),
                    eq("RELIANCE"), eq(OrderSide.BUY), eq(OrderType.LIMIT),
                    eq(new BigDecimal("10")), eq(new BigDecimal("2500.00")));
            verify(idempotencyService).markAsProcessed(anyString(), anyString());
        }

        @Test
        @DisplayName("Should skip duplicate events using idempotency")
        void handleOrderValidated_DuplicateEvent_ShouldSkip() {
            // Arrange
            when(idempotencyService.exists(anyString())).thenReturn(true);

            // Act
            orderEventListener.handleOrderValidated(validatedEvent);

            // Assert
            verify(fundsLockService, never()).lockFunds(anyLong(), anyString(), any(), anyString());
        }

        @Test
        @DisplayName("Should publish OrderRejectedEvent on insufficient funds")
        void handleOrderValidated_InsufficientFunds_ShouldReject() {
            // Arrange
            when(idempotencyService.exists(anyString())).thenReturn(false);
            when(fundsLockService.lockFunds(anyLong(), anyString(), any(), anyString()))
                    .thenThrow(new InsufficientFundsException(new BigDecimal("25000.00"), new BigDecimal("10000.00")));

            // Act
            orderEventListener.handleOrderValidated(validatedEvent);

            // Assert
            verify(fundsEventPublisher).publishOrderRejected(
                    eq("ORD-123"), eq(100L), eq("RELIANCE"),
                    eq(OrderSide.BUY), eq(OrderType.LIMIT),
                    eq(new BigDecimal("10")), eq(new BigDecimal("2500.00")),
                    contains("Insufficient funds"));
            verify(idempotencyService).markAsProcessed(anyString(), contains("Rejected"));
        }

        @Test
        @DisplayName("Should rethrow other exceptions for retry")
        void handleOrderValidated_OtherError_ShouldRethrow() {
            // Arrange
            when(idempotencyService.exists(anyString())).thenReturn(false);
            when(fundsLockService.lockFunds(anyLong(), anyString(), any(), anyString()))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThatThrownBy(() -> orderEventListener.handleOrderValidated(validatedEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");

            verify(idempotencyService, never()).markAsProcessed(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("handleOrderCancelled tests")
    class HandleOrderCancelledTests {

        @Test
        @DisplayName("Should release funds on OrderCancelledEvent")
        void handleOrderCancelled_ShouldReleaseFunds() {
            // Arrange
            when(idempotencyService.exists(anyString())).thenReturn(false);
            when(fundsLockService.releaseFunds(eq("ORD-123"), anyString())).thenReturn(testLockDTO);

            // Act
            orderEventListener.handleOrderCancelled(cancelledEvent);

            // Assert
            verify(fundsLockService).releaseFunds(eq("ORD-123"), contains("Order cancelled"));
            verify(idempotencyService).markAsProcessed(anyString(), anyString());
        }

        @Test
        @DisplayName("Should skip duplicate cancellation events")
        void handleOrderCancelled_DuplicateEvent_ShouldSkip() {
            // Arrange
            when(idempotencyService.exists(anyString())).thenReturn(true);

            // Act
            orderEventListener.handleOrderCancelled(cancelledEvent);

            // Assert
            verify(fundsLockService, never()).releaseFunds(anyString(), anyString());
        }

        @Test
        @DisplayName("Should rethrow exceptions for retry")
        void handleOrderCancelled_Error_ShouldRethrow() {
            // Arrange
            when(idempotencyService.exists(anyString())).thenReturn(false);
            when(fundsLockService.releaseFunds(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Lock not found"));

            // Act & Assert
            assertThatThrownBy(() -> orderEventListener.handleOrderCancelled(cancelledEvent))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

package in.winvestco.funds_service.service;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.enums.LockStatus;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.event.FundsDepositedEvent;
import in.winvestco.common.event.FundsLockedEvent;
import in.winvestco.common.event.FundsReleasedEvent;
import in.winvestco.common.event.FundsWithdrawnEvent;
import in.winvestco.common.event.OrderRejectedEvent;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.model.FundsLock;
import in.winvestco.funds_service.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FundsEventPublisher.
 * Verifies correct event payloads and routing keys.
 */
@ExtendWith(MockitoExtension.class)
class FundsEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private FundsEventPublisher fundsEventPublisher;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private Wallet testWallet;
    private FundsLock testLock;
    private FundsLockDTO testLockDTO;

    @BeforeEach
    void setUp() {
        testWallet = Wallet.builder()
                .id(1L)
                .userId(100L)
                .availableBalance(new BigDecimal("10000.00"))
                .lockedBalance(new BigDecimal("1000.00"))
                .currency("INR")
                .build();

        testLock = FundsLock.builder()
                .id(1L)
                .walletId(1L)
                .orderId("ORD-123")
                .amount(new BigDecimal("1000.00"))
                .status(LockStatus.LOCKED)
                .reason("Order placed")
                .createdAt(Instant.now())
                .build();

        testLockDTO = new FundsLockDTO();
        testLockDTO.setId(1L);
        testLockDTO.setOrderId("ORD-123");
        testLockDTO.setAmount(new BigDecimal("1000.00"));
        testLockDTO.setStatus(LockStatus.LOCKED);
    }

    @Test
    @DisplayName("Should publish FundsLockedEvent with correct exchange and routing key")
    void publishFundsLocked_ShouldSendToCorrectExchange() {
        // Act
        fundsEventPublisher.publishFundsLocked(100L, testWallet, testLock);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FUNDS_EXCHANGE),
                eq(RabbitMQConfig.FUNDS_LOCKED_ROUTING_KEY),
                eventCaptor.capture());

        Object captured = eventCaptor.getValue();
        assertThat(captured).isInstanceOf(FundsLockedEvent.class);
        FundsLockedEvent event = (FundsLockedEvent) captured;
        assertThat(event.getOrderId()).isEqualTo("ORD-123");
        assertThat(event.getUserId()).isEqualTo(100L);
        assertThat(event.getLockedAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should publish FundsReleasedEvent with release reason")
    void publishFundsReleased_ShouldIncludeReleaseReason() {
        // Act
        fundsEventPublisher.publishFundsReleased(100L, testWallet, testLock, "Order cancelled by user");

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FUNDS_EXCHANGE),
                eq(RabbitMQConfig.FUNDS_RELEASED_ROUTING_KEY),
                eventCaptor.capture());

        FundsReleasedEvent event = (FundsReleasedEvent) eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo("ORD-123");
        assertThat(event.getReleaseReason()).isEqualTo("Order cancelled by user");
        assertThat(event.getReleasedAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should publish FundsDepositedEvent with correct balance info")
    void publishFundsDeposited_ShouldIncludeBalanceInfo() {
        // Arrange
        BigDecimal balanceBefore = new BigDecimal("5000.00");

        // Act
        fundsEventPublisher.publishFundsDeposited(
                100L, testWallet, new BigDecimal("5000.00"), balanceBefore, "DEP-123", "BANK_TRANSFER");

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FUNDS_EXCHANGE),
                eq(RabbitMQConfig.FUNDS_DEPOSITED_ROUTING_KEY),
                eventCaptor.capture());

        FundsDepositedEvent event = (FundsDepositedEvent) eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(100L);
        assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(event.getBalanceBefore()).isEqualByComparingTo(balanceBefore);
        assertThat(event.getNewBalance()).isEqualByComparingTo(testWallet.getAvailableBalance());
        assertThat(event.getDepositMethod()).isEqualTo("BANK_TRANSFER");
    }

    @Test
    @DisplayName("Should use default deposit method when null")
    void publishFundsDeposited_NullMethod_ShouldUseDefault() {
        // Act
        fundsEventPublisher.publishFundsDeposited(
                100L, testWallet, new BigDecimal("5000.00"), BigDecimal.ZERO, "DEP-123", null);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FUNDS_EXCHANGE),
                eq(RabbitMQConfig.FUNDS_DEPOSITED_ROUTING_KEY),
                eventCaptor.capture());

        FundsDepositedEvent event = (FundsDepositedEvent) eventCaptor.getValue();
        assertThat(event.getDepositMethod()).isEqualTo("BANK_TRANSFER");
    }

    @Test
    @DisplayName("Should publish FundsWithdrawnEvent with bank account info")
    void publishFundsWithdrawn_ShouldIncludeBankInfo() {
        // Arrange
        BigDecimal balanceBefore = new BigDecimal("15000.00");

        // Act
        fundsEventPublisher.publishFundsWithdrawn(
                100L, testWallet, new BigDecimal("5000.00"), balanceBefore,
                "WDR-123", "IMPS", "1234");

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FUNDS_EXCHANGE),
                eq(RabbitMQConfig.FUNDS_WITHDRAWN_ROUTING_KEY),
                eventCaptor.capture());

        FundsWithdrawnEvent event = (FundsWithdrawnEvent) eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(100L);
        assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(event.getWithdrawalMethod()).isEqualTo("IMPS");
        assertThat(event.getBankAccountLast4()).isEqualTo("1234");
    }

    @Test
    @DisplayName("Should use defaults for null withdrawal method and bank account")
    void publishFundsWithdrawn_NullValues_ShouldUseDefaults() {
        // Act
        fundsEventPublisher.publishFundsWithdrawn(
                100L, testWallet, new BigDecimal("5000.00"), BigDecimal.ZERO,
                "WDR-123", null, null);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FUNDS_EXCHANGE),
                eq(RabbitMQConfig.FUNDS_WITHDRAWN_ROUTING_KEY),
                eventCaptor.capture());

        FundsWithdrawnEvent event = (FundsWithdrawnEvent) eventCaptor.getValue();
        assertThat(event.getWithdrawalMethod()).isEqualTo("BANK_TRANSFER");
        assertThat(event.getBankAccountLast4()).isEqualTo("****");
    }

    @Test
    @DisplayName("Should publish FundsLockedEvent with trading details")
    void publishFundsLockedWithDetails_ShouldIncludeTradingInfo() {
        // Act
        fundsEventPublisher.publishFundsLockedWithDetails(
                100L, testWallet, testLockDTO,
                "RELIANCE", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("10"), new BigDecimal("2500.00"));

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FUNDS_EXCHANGE),
                eq(RabbitMQConfig.FUNDS_LOCKED_ROUTING_KEY),
                eventCaptor.capture());

        FundsLockedEvent event = (FundsLockedEvent) eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo("ORD-123");
        assertThat(event.getSymbol()).isEqualTo("RELIANCE");
        assertThat(event.getSide()).isEqualTo(OrderSide.BUY);
        assertThat(event.getOrderType()).isEqualTo(OrderType.LIMIT);
        assertThat(event.getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(event.getPrice()).isEqualByComparingTo(new BigDecimal("2500.00"));
    }

    @Test
    @DisplayName("Should publish OrderRejectedEvent for insufficient funds")
    void publishOrderRejected_ShouldSendToOrderExchange() {
        // Act
        fundsEventPublisher.publishOrderRejected(
                "ORD-123", 100L, "RELIANCE",
                OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("100"), new BigDecimal("2500.00"),
                "Insufficient funds");

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_REJECTED_ROUTING_KEY),
                eventCaptor.capture());

        OrderRejectedEvent event = (OrderRejectedEvent) eventCaptor.getValue();
        assertThat(event.getOrderId()).isEqualTo("ORD-123");
        assertThat(event.getRejectionReason()).isEqualTo("Insufficient funds");
        assertThat(event.getRejectedBy()).isEqualTo("FUNDS");
    }
}

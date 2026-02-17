package in.winvestco.funds_service.service;

import in.winvestco.common.event.FundsDepositedEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.funds_service.model.Wallet;
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
class FundsEventPublisherOutboxTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private FundsEventPublisher fundsEventPublisher;

    @Test
    void shouldCaptureFundsDepositedEventInOutbox() {
        // Given
        Long userId = 123L;
        Wallet wallet = Wallet.builder()
                .id(456L)
                .userId(userId)
                .availableBalance(new BigDecimal("1000.00"))
                .build();

        BigDecimal amount = new BigDecimal("500.00");
        BigDecimal balanceBefore = new BigDecimal("500.00");
        String referenceId = "DEP-001";
        String depositMethod = "BANK_TRANSFER";

        // When
        fundsEventPublisher.publishFundsDeposited(userId, wallet, amount, 
                balanceBefore, referenceId, depositMethod);

        // Then
        verify(outboxService).captureEvent(
                eq("Wallet"),
                eq("456"),
                eq("funds.exchange"),
                eq("funds.deposited"),
                any(FundsDepositedEvent.class)
        );
    }
}

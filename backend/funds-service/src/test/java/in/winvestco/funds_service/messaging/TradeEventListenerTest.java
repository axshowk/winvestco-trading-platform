package in.winvestco.funds_service.messaging;

import com.rabbitmq.client.Channel;
import in.winvestco.common.enums.LockStatus;
import in.winvestco.common.event.TradeFailedEvent;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.service.FundsLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TradeEventListener.
 * Tests funds release on trade failure (SAGA compensation).
 */
@ExtendWith(MockitoExtension.class)
class TradeEventListenerTest {

    @Mock
    private FundsLockService fundsLockService;

    @Mock
    private Channel channel;

    @InjectMocks
    private TradeEventListener tradeEventListener;

    private TradeFailedEvent tradeFailedEvent;
    private FundsLockDTO releasedLockDTO;

    @BeforeEach
    void setUp() {
        tradeFailedEvent = TradeFailedEvent.builder()
                .tradeId("TRADE-456")
                .orderId("ORD-123")
                .userId(100L)
                .symbol("RELIANCE")
                .quantity(new BigDecimal("10"))
                .failureReason("Exchange rejected order")
                .failedAt(Instant.now())
                .build();

        releasedLockDTO = new FundsLockDTO();
        releasedLockDTO.setId(1L);
        releasedLockDTO.setOrderId("ORD-123");
        releasedLockDTO.setAmount(new BigDecimal("25000.00"));
        releasedLockDTO.setStatus(LockStatus.RELEASED);
    }

    @Test
    @DisplayName("Should release funds on TradeFailedEvent")
    void handleTradeFailed_ShouldReleaseFundsAndAck() throws IOException {
        // Arrange
        when(fundsLockService.releaseFunds(eq("ORD-123"), anyString())).thenReturn(releasedLockDTO);

        // Act
        tradeEventListener.handleTradeFailed(tradeFailedEvent, channel, 1L);

        // Assert
        verify(fundsLockService).releaseFunds(eq("ORD-123"), contains("Trade failed"));
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Should nack message on funds release failure")
    void handleTradeFailed_ReleaseError_ShouldNack() throws IOException {
        // Arrange
        when(fundsLockService.releaseFunds(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        tradeEventListener.handleTradeFailed(tradeFailedEvent, channel, 1L);

        // Assert
        verify(channel).basicNack(1L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Should include failure reason in release description")
    void handleTradeFailed_ShouldIncludeFailureReason() throws IOException {
        // Arrange
        when(fundsLockService.releaseFunds(anyString(), anyString())).thenReturn(releasedLockDTO);

        // Act
        tradeEventListener.handleTradeFailed(tradeFailedEvent, channel, 1L);

        // Assert
        verify(fundsLockService).releaseFunds(
                eq("ORD-123"),
                argThat(reason -> reason.contains("Exchange rejected order")));
    }
}

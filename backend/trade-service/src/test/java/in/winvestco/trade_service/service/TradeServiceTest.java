package in.winvestco.trade_service.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.TradeStatus;
import in.winvestco.common.enums.OrderType;
import in.winvestco.trade_service.dto.CreateTradeRequest;
import in.winvestco.trade_service.dto.TradeDTO;
import in.winvestco.trade_service.exception.TradeNotFoundException;
import in.winvestco.trade_service.mapper.TradeMapper;
import in.winvestco.trade_service.model.Trade;
import in.winvestco.trade_service.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeMapper tradeMapper;

    @Mock
    private TradeValidationService validationService;

    @Mock
    private TradeEventPublisher eventPublisher;

    @Mock
    private MeterRegistry meterRegistry;

    @Spy
    private ObservationRegistry observationRegistry = ObservationRegistry.create();

    @InjectMocks
    private TradeService tradeService;

    private CreateTradeRequest tradeRequest;
    private Trade testTrade;

    @BeforeEach
    void setUp() {
        tradeRequest = CreateTradeRequest.builder()
                .orderId("ORDER-123")
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .tradeType(OrderType.LIMIT)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500"))
                .build();

        testTrade = Trade.builder()
                .tradeId(UUID.randomUUID().toString())
                .orderId("ORDER-123")
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .tradeType(OrderType.LIMIT)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500"))
                .status(TradeStatus.CREATED)
                .executedQuantity(BigDecimal.ZERO)
                .build();

        Timer mockTimer = mock(Timer.class);
        lenient().when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(mockTimer);
    }

    @Test
    void createTradeFromOrder_ShouldCreateAndPlaceTrade() {
        when(tradeRepository.existsByOrderId(anyString())).thenReturn(false);
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(any(Trade.class))).thenReturn(new TradeDTO());

        TradeDTO result = tradeService.createTradeFromOrder(tradeRequest);

        assertNotNull(result);
        verify(validationService).validate(tradeRequest);
        verify(tradeRepository, atLeastOnce()).save(any(Trade.class));
        verify(eventPublisher).publishTradeCreated(any(Trade.class));
        verify(eventPublisher).publishTradePlaced(any(Trade.class));
    }

    @Test
    void handleExecutionUpdate_ShouldUpdateQuantityAndStatus() {
        testTrade.setStatus(TradeStatus.PLACED);
        when(tradeRepository.findByTradeId(anyString())).thenReturn(Optional.of(testTrade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(any(Trade.class))).thenReturn(new TradeDTO());

        tradeService.handleExecutionUpdate(testTrade.getTradeId(), new BigDecimal("10"), new BigDecimal("2505"), false);

        assertEquals(new BigDecimal("10"), testTrade.getExecutedQuantity());
        assertEquals(TradeStatus.FILLED, testTrade.getStatus());
        verify(eventPublisher).publishTradeExecuted(eq(testTrade), eq(false));
    }

    @Test
    void cancelTrade_ShouldUpdateStatus() {
        testTrade.setStatus(TradeStatus.PLACED);
        when(tradeRepository.findByTradeId(anyString())).thenReturn(Optional.of(testTrade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(any(Trade.class))).thenReturn(new TradeDTO());

        tradeService.cancelTrade(testTrade.getTradeId(), 1L, "User cancelled");

        assertEquals(TradeStatus.CANCELLED, testTrade.getStatus());
        verify(eventPublisher).publishTradeCancelled(eq(testTrade), eq("USER"), anyString());
    }

    @Test
    void cancelTrade_WhenNotOwner_ShouldThrowException() {
        when(tradeRepository.findByTradeId(anyString())).thenReturn(Optional.of(testTrade));

        assertThrows(TradeNotFoundException.class,
                () -> tradeService.cancelTrade(testTrade.getTradeId(), 2L, "Not owner"));
    }
}

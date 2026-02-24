package in.winvestco.trade_service.service;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.TradeStatus;
import in.winvestco.trade_service.dto.CreateTradeRequest;
import in.winvestco.trade_service.dto.TradeDTO;
import in.winvestco.trade_service.exception.InvalidTradeStateException;
import in.winvestco.trade_service.exception.TradeNotFoundException;
import in.winvestco.trade_service.mapper.TradeMapper;
import in.winvestco.trade_service.model.Trade;
import in.winvestco.trade_service.repository.TradeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    private Trade testTrade;
    private CreateTradeRequest createRequest;
    private TradeDTO testTradeDTO;

    @BeforeEach
    void setUp() {
        testTrade = Trade.builder()
                .id(1L)
                .tradeId("T-123")
                .orderId("O-123")
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .tradeType(OrderType.LIMIT)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500"))
                .status(TradeStatus.CREATED)
                .executedQuantity(BigDecimal.ZERO)
                .build();

        createRequest = CreateTradeRequest.builder()
                .orderId("O-123")
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .tradeType(OrderType.LIMIT)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500"))
                .build();

        testTradeDTO = new TradeDTO();
        testTradeDTO.setTradeId("T-123");
        testTradeDTO.setStatus(TradeStatus.CREATED);

        Timer mockTimer = mock(Timer.class);
        lenient().when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(mockTimer);
    }

    @Test
    @DisplayName("createTradeFromOrder - successful creation")
    void createTradeFromOrder_NewOrder_ShouldCreateAndPlaceTrade() {
        when(tradeRepository.existsByOrderId(anyString())).thenReturn(false);
        when(tradeRepository.save(any(Trade.class))).thenAnswer(i -> i.getArguments()[0]);
        when(tradeMapper.toDTO(any(Trade.class))).thenReturn(testTradeDTO);

        TradeDTO result = tradeService.createTradeFromOrder(createRequest);

        assertNotNull(result);
        verify(validationService).validate(createRequest);
        verify(tradeRepository, atLeastOnce()).save(any(Trade.class));
        verify(eventPublisher).publishTradeCreated(any(Trade.class));
        verify(eventPublisher).publishTradePlaced(any(Trade.class));
    }

    @Test
    @DisplayName("createTradeFromOrder - duplicate order should return existing")
    void createTradeFromOrder_DuplicateOrder_ShouldReturnExisting() {
        when(tradeRepository.existsByOrderId(anyString())).thenReturn(true);
        when(tradeRepository.findByOrderId(anyString())).thenReturn(Optional.of(testTrade));
        when(tradeMapper.toDTO(testTrade)).thenReturn(testTradeDTO);

        TradeDTO result = tradeService.createTradeFromOrder(createRequest);

        assertEquals(testTradeDTO, result);
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    @DisplayName("placeTrade - valid state should transition to PLACED")
    void placeTrade_ValidState_ShouldTransitionToPlaced() {
        testTrade.setStatus(TradeStatus.VALIDATED);
        testTrade.setValidatedAt(Instant.now());

        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(testTrade)).thenReturn(testTradeDTO);

        tradeService.placeTrade(testTrade);

        assertEquals(TradeStatus.PLACED, testTrade.getStatus());
        verify(eventPublisher).publishTradePlaced(testTrade);
    }

    @Test
    @DisplayName("placeTrade - invalid state should throw exception")
    void placeTrade_InvalidState_ShouldThrowException() {
        testTrade.setStatus(TradeStatus.CREATED); // Must be VALIDATED to place

        assertThrows(InvalidTradeStateException.class, () -> tradeService.placeTrade(testTrade));
    }

    @Test
    @DisplayName("handleExecutionUpdate - full fill should transition to FILLED")
    void handleExecutionUpdate_FullFill_ShouldTransitionToFilled() {
        testTrade.setStatus(TradeStatus.PLACED);
        testTrade.setPlacedAt(Instant.now());

        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(any(Trade.class))).thenReturn(testTradeDTO);

        tradeService.handleExecutionUpdate("T-123", new BigDecimal("10"), new BigDecimal("2505"), false);

        assertEquals(TradeStatus.FILLED, testTrade.getStatus());
        assertEquals(new BigDecimal("10"), testTrade.getExecutedQuantity());
        verify(eventPublisher).publishTradeExecuted(eq(testTrade), eq(false));
    }

    @Test
    @DisplayName("handleExecutionUpdate - partial fill should transition to PARTIALLY_FILLED")
    void handleExecutionUpdate_PartialFill_ShouldTransitionToPartiallyFilled() {
        testTrade.setStatus(TradeStatus.PLACED);

        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(any(Trade.class))).thenReturn(testTradeDTO);

        tradeService.handleExecutionUpdate("T-123", new BigDecimal("4"), new BigDecimal("2505"), true);

        assertEquals(TradeStatus.PARTIALLY_FILLED, testTrade.getStatus());
        assertEquals(new BigDecimal("4"), testTrade.getExecutedQuantity());
        verify(eventPublisher).publishTradeExecuted(eq(testTrade), eq(true));
    }

    @Test
    @DisplayName("handleExecutionUpdate - terminal trade should be ignored")
    void handleExecutionUpdate_TerminalTrade_ShouldIgnore() {
        testTrade.setStatus(TradeStatus.CLOSED);
        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));
        when(tradeMapper.toDTO(testTrade)).thenReturn(testTradeDTO);

        TradeDTO result = tradeService.handleExecutionUpdate("T-123", new BigDecimal("10"), new BigDecimal("2500"),
                false);

        assertEquals(testTradeDTO, result);
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    @DisplayName("closeTrade - FILLED trade should transition to CLOSED")
    void closeTrade_Filled_ShouldTransitionToClosed() {
        testTrade.setStatus(TradeStatus.FILLED);

        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(testTrade)).thenReturn(testTradeDTO);

        tradeService.closeTrade("T-123");

        assertEquals(TradeStatus.CLOSED, testTrade.getStatus());
        verify(eventPublisher).publishTradeClosed(testTrade);
    }

    @Test
    @DisplayName("closeTrade - non-FILLED trade should throw exception")
    void closeTrade_NotFilled_ShouldThrowException() {
        testTrade.setStatus(TradeStatus.PLACED);
        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));

        assertThrows(InvalidTradeStateException.class, () -> tradeService.closeTrade("T-123"));
    }

    @Test
    @DisplayName("cancelTrade - active trade should transition to CANCELLED")
    void cancelTrade_ActiveAndOwner_ShouldTransitionToCancelled() {
        testTrade.setStatus(TradeStatus.PLACED);

        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(testTrade)).thenReturn(testTradeDTO);

        tradeService.cancelTrade("T-123", 1L, "User requested");

        assertEquals(TradeStatus.CANCELLED, testTrade.getStatus());
        verify(eventPublisher).publishTradeCancelled(eq(testTrade), anyString(), anyString());
    }

    @Test
    @DisplayName("cancelTrade - wrong owner should throw exception")
    void cancelTrade_WrongOwner_ShouldThrowException() {
        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));

        assertThrows(TradeNotFoundException.class, () -> tradeService.cancelTrade("T-123", 999L, "Reason"));
    }

    @Test
    @DisplayName("cancelTrade - terminal trade should throw exception")
    void cancelTrade_Terminal_ShouldThrowException() {
        testTrade.setStatus(TradeStatus.CLOSED);
        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));

        assertThrows(InvalidTradeStateException.class, () -> tradeService.cancelTrade("T-123", 1L, "Reason"));
    }

    @Test
    @DisplayName("failTrade - active trade should transition to FAILED")
    void failTrade_Active_ShouldTransitionToFailed() {
        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);
        when(tradeMapper.toDTO(testTrade)).thenReturn(testTradeDTO);

        tradeService.failTrade("T-123", "Reason", "ERR_CODE");

        assertEquals(TradeStatus.FAILED, testTrade.getStatus());
        assertEquals("Reason", testTrade.getFailureReason());
        verify(eventPublisher).publishTradeFailed(testTrade, "ERR_CODE");
    }

    @Test
    @DisplayName("failTrade - terminal trade should return current state")
    void failTrade_Terminal_ShouldReturnCurrent() {
        testTrade.setStatus(TradeStatus.CLOSED);
        when(tradeRepository.findByTradeId("T-123")).thenReturn(Optional.of(testTrade));
        when(tradeMapper.toDTO(testTrade)).thenReturn(testTradeDTO);

        TradeDTO result = tradeService.failTrade("T-123", "Reason", "ERR");

        assertEquals(testTradeDTO, result);
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    @DisplayName("getTradesForUser - should return page of DTOs")
    void getTradesForUser_ShouldReturnPage() {
        Page<Trade> tradePage = new PageImpl<>(List.of(testTrade));
        when(tradeRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(tradePage);

        tradeService.getTradesForUser(1L, PageRequest.of(0, 10));

        verify(tradeMapper).toDTO(testTrade);
    }

    @Test
    @DisplayName("getActiveTrades - should return list of DTOs")
    void getActiveTrades_ShouldReturnList() {
        when(tradeRepository.findActiveTradesByUserId(eq(1L), anyList())).thenReturn(List.of(testTrade));

        tradeService.getActiveTrades(1L);

        verify(tradeMapper).toDTOList(anyList());
    }
}

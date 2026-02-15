package in.winvestco.ledger_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.LedgerEntryEvent;
import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.model.LedgerEntry;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerEventPublisher Tests")
class LedgerEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LedgerEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<LedgerEntryEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<String> exchangeCaptor;

    @Captor
    private ArgumentCaptor<String> routingKeyCaptor;

    private LedgerEntry testEntry;

    @BeforeEach
    void setUp() {
        testEntry = LedgerEntry.builder()
                .id(1L)
                .walletId(1L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("1000.00"))
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("1000.00"))
                .referenceId("REF-123")
                .referenceType("TEST")
                .description("Test entry")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should publish ledger entry recorded event")
    void publishLedgerEntryRecorded_ShouldPublishEvent() {
        // When
        eventPublisher.publishLedgerEntryRecorded(testEntry);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture());

        // Verify exchange and routing key
        assertEquals(RabbitMQConfig.LEDGER_EXCHANGE, exchangeCaptor.getValue());
        assertEquals(RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY, routingKeyCaptor.getValue());

        // Verify event content
        LedgerEntryEvent publishedEvent = eventCaptor.getValue();
        assertEquals(testEntry.getId(), publishedEvent.getId());
        assertEquals(testEntry.getWalletId(), publishedEvent.getWalletId());
        assertEquals(testEntry.getEntryType(), publishedEvent.getEntryType());
        assertEquals(testEntry.getAmount(), publishedEvent.getAmount());
        assertEquals(testEntry.getBalanceBefore(), publishedEvent.getBalanceBefore());
        assertEquals(testEntry.getBalanceAfter(), publishedEvent.getBalanceAfter());
        assertEquals(testEntry.getReferenceId(), publishedEvent.getReferenceId());
        assertEquals(testEntry.getReferenceType(), publishedEvent.getReferenceType());
        assertEquals(testEntry.getDescription(), publishedEvent.getDescription());
        assertEquals(testEntry.getCreatedAt(), publishedEvent.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle null reference fields gracefully")
    void publishLedgerEntryRecorded_ShouldHandleNullReferences() {
        // Given
        LedgerEntry entryWithNulls = LedgerEntry.builder()
                .id(1L)
                .walletId(1L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("100.00"))
                .referenceId(null)
                .referenceType(null)
                .description(null)
                .createdAt(Instant.now())
                .build();

        // When
        eventPublisher.publishLedgerEntryRecorded(entryWithNulls);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.LEDGER_EXCHANGE),
                eq(RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY),
                eventCaptor.capture());

        LedgerEntryEvent event = eventCaptor.getValue();
        assertNull(event.getReferenceId());
        assertNull(event.getReferenceType());
        assertNull(event.getDescription());
        
        // Verify non-null fields are still set correctly
        assertEquals(entryWithNulls.getId(), event.getId());
        assertEquals(entryWithNulls.getWalletId(), event.getWalletId());
        assertEquals(entryWithNulls.getEntryType(), event.getEntryType());
    }

    @Test
    @DisplayName("Should publish events for all entry types")
    void publishLedgerEntryRecorded_ShouldWorkForAllEntryTypes() {
        // Given
        LedgerEntryType[] entryTypes = LedgerEntryType.values();

        for (LedgerEntryType entryType : entryTypes) {
            // Reset mock
            reset(rabbitTemplate);
            
            LedgerEntry entry = LedgerEntry.builder()
                    .id(1L)
                    .walletId(1L)
                    .entryType(entryType)
                    .amount(new BigDecimal("100.00"))
                    .balanceBefore(BigDecimal.ZERO)
                    .balanceAfter(new BigDecimal("100.00"))
                    .referenceId("REF-123")
                    .referenceType("TEST")
                    .description("Test entry")
                    .createdAt(Instant.now())
                    .build();

            // When
            eventPublisher.publishLedgerEntryRecorded(entry);

            // Then
            verify(rabbitTemplate, times(1)).convertAndSend(
                    eq(RabbitMQConfig.LEDGER_EXCHANGE),
                    eq(RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY),
                    eventCaptor.capture());

            LedgerEntryEvent event = eventCaptor.getValue();
            assertEquals(entryType, event.getEntryType());
        }
    }

    @Test
    @DisplayName("Should handle RabbitMQ publishing exceptions")
    void publishLedgerEntryRecorded_ShouldHandleRabbitMQExceptions() {
        // Given
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(
                        anyString(), anyString(), any(LedgerEntryEvent.class));

        // When & Then
        assertDoesNotThrow(() -> eventPublisher.publishLedgerEntryRecorded(testEntry));
        
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.LEDGER_EXCHANGE),
                eq(RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY),
                any(LedgerEntryEvent.class));
    }

    @Test
    @DisplayName("Should create event with exact same values as ledger entry")
    void publishLedgerEntryRecorded_ShouldCreateExactEventCopy() {
        // Given
        LedgerEntry entry = LedgerEntry.builder()
                .id(12345L)
                .walletId(67890L)
                .entryType(LedgerEntryType.TRADE_BUY)
                .amount(new BigDecimal("1500.1234"))
                .balanceBefore(new BigDecimal("5000.0000"))
                .balanceAfter(new BigDecimal("3500.1234"))
                .referenceId("ORDER-12345")
                .referenceType("ORDER")
                .description("Buy 100 shares of AAPL")
                .createdAt(java.time.Instant.parse("2024-01-15T10:30:00Z"))
                .build();

        // When
        eventPublisher.publishLedgerEntryRecorded(entry);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.LEDGER_EXCHANGE),
                eq(RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY),
                eventCaptor.capture());

        LedgerEntryEvent event = eventCaptor.getValue();
        
        // Verify all fields match exactly
        assertEquals(entry.getId(), event.getId());
        assertEquals(entry.getWalletId(), event.getWalletId());
        assertEquals(entry.getEntryType(), event.getEntryType());
        assertEquals(entry.getAmount(), event.getAmount());
        assertEquals(entry.getBalanceBefore(), event.getBalanceBefore());
        assertEquals(entry.getBalanceAfter(), event.getBalanceAfter());
        assertEquals(entry.getReferenceId(), event.getReferenceId());
        assertEquals(entry.getReferenceType(), event.getReferenceType());
        assertEquals(entry.getDescription(), event.getDescription());
        assertEquals(entry.getCreatedAt(), event.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle high precision decimal values")
    void publishLedgerEntryRecorded_ShouldHandleHighPrecisionDecimals() {
        // Given
        BigDecimal highPrecisionAmount = new BigDecimal("123456789.9876543210");
        BigDecimal highPrecisionBalanceBefore = new BigDecimal("999999999.1234567890");
        BigDecimal highPrecisionBalanceAfter = new BigDecimal("1123456789.1111111100");

        LedgerEntry entry = LedgerEntry.builder()
                .id(1L)
                .walletId(1L)
                .entryType(LedgerEntryType.DEPOSIT)
                .amount(highPrecisionAmount)
                .balanceBefore(highPrecisionBalanceBefore)
                .balanceAfter(highPrecisionBalanceAfter)
                .referenceId("REF-123")
                .referenceType("TEST")
                .description("Test entry")
                .createdAt(Instant.now())
                .build();

        // When
        eventPublisher.publishLedgerEntryRecorded(entry);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.LEDGER_EXCHANGE),
                eq(RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY),
                eventCaptor.capture());

        LedgerEntryEvent event = eventCaptor.getValue();
        
        // Verify precision is maintained
        assertEquals(0, highPrecisionAmount.compareTo(event.getAmount()));
        assertEquals(0, highPrecisionBalanceBefore.compareTo(event.getBalanceBefore()));
        assertEquals(0, highPrecisionBalanceAfter.compareTo(event.getBalanceAfter()));
    }

    @Test
    @DisplayName("Should log event publishing")
    void publishLedgerEntryRecorded_ShouldLogEventPublishing() {
        // This test verifies that logging occurs (would need to verify log output in integration test)
        // For unit test, we just verify the method completes without exception
        
        // When & Then
        assertDoesNotThrow(() -> eventPublisher.publishLedgerEntryRecorded(testEntry));
        
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(), anyString(), any(LedgerEntryEvent.class));
    }

    @Test
    @DisplayName("Should handle concurrent event publishing")
    void publishLedgerEntryRecorded_ShouldHandleConcurrentPublishing() throws InterruptedException {
        // Given
        int threadCount = 10;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // When - publish events from multiple threads
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    LedgerEntry entry = LedgerEntry.builder()
                            .id((long) index)
                            .walletId((long) index)
                            .entryType(LedgerEntryType.DEPOSIT)
                            .amount(new BigDecimal("100.00"))
                            .balanceBefore(BigDecimal.ZERO)
                            .balanceAfter(new BigDecimal("100.00"))
                            .referenceId("REF-" + index)
                            .referenceType("TEST")
                            .description("Test entry")
                            .createdAt(Instant.now())
                            .build();
                    eventPublisher.publishLedgerEntryRecorded(entry);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait for all threads to complete
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

        // Then
        assertEquals(threadCount, successCount.get());
        verify(rabbitTemplate, times(threadCount)).convertAndSend(
                eq(RabbitMQConfig.LEDGER_EXCHANGE),
                eq(RabbitMQConfig.LEDGER_ENTRY_RECORDED_ROUTING_KEY),
                any(LedgerEntryEvent.class));
    }
}

package in.winvestco.ledger_service.performance;

import in.winvestco.common.enums.LedgerEntryType;
import in.winvestco.ledger_service.dto.CreateLedgerEntryRequest;
import in.winvestco.ledger_service.model.LedgerEntry;
import in.winvestco.ledger_service.repository.LedgerEntryRepository;
import in.winvestco.ledger_service.service.LedgerService;
import in.winvestco.ledger_service.testdata.LedgerTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Ledger Performance Tests")
class LedgerPerformanceTest {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(20);
        ledgerEntryRepository.deleteAll();
    }

    @Test
    @DisplayName("Should handle high volume concurrent entry recording")
    void concurrentEntryRecording_ShouldMaintainPerformance() throws InterruptedException {
        // Given
        int threadCount = 50;
        int entriesPerThread = 100;
        int totalEntries = threadCount * entriesPerThread;
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalDuration = new AtomicLong(0);

        // When
        List<Future<?>> futures = new ArrayList<>();
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Future<?> future = executorService.submit(() -> {
                try {
                    long startTime = System.nanoTime();
                    
                    for (int i = 0; i < entriesPerThread; i++) {
                        CreateLedgerEntryRequest request = LedgerTestDataFactory.createTestRequest(
                                (long) (threadId * 1000 + i), // Unique wallet ID per thread
                                LedgerEntryType.DEPOSIT,
                                new BigDecimal("100.00"),
                                BigDecimal.ZERO,
                                new BigDecimal("100.00")
                        );
                        
                        try {
                            ledgerService.recordEntry(request);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                    
                    long duration = System.nanoTime() - startTime;
                    totalDuration.addAndGet(duration);
                    
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // Wait for completion
        assertTrue(latch.await(60, TimeUnit.SECONDS), "Test should complete within 60 seconds");
        
        // Verify all futures completed
        futures.forEach(future -> {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Future should complete without exception: " + e.getMessage());
            }
        });

        // Then
        assertEquals(totalEntries, successCount.get() + errorCount.get());
        assertTrue(errorCount.get() < totalEntries * 0.05, "Error rate should be less than 5%");
        
        // Performance assertions
        double avgDurationMs = totalDuration.get() / (double) threadCount / 1_000_000;
        assertTrue(avgDurationMs < 5000, "Average thread duration should be less than 5 seconds");
        
        // Verify database contains expected entries
        long dbCount = ledgerEntryRepository.count();
        assertEquals(successCount.get(), dbCount);
    }

    @Test
    @DisplayName("Should handle concurrent balance queries efficiently")
    void concurrentBalanceQueries_ShouldMaintainPerformance() throws InterruptedException {
        // Given
        int walletCount = 100;
        int entriesPerWallet = 50;
        
        // Create test data
        List<LedgerEntry> testEntries = new ArrayList<>();
        for (int w = 0; w < walletCount; w++) {
            BigDecimal balance = BigDecimal.ZERO;
            for (int e = 0; e < entriesPerWallet; e++) {
                BigDecimal amount = new BigDecimal("10.00");
                balance = balance.add(amount);
                
                LedgerEntry entry = LedgerTestDataFactory.createTestEntry(
                        (long) w, LedgerEntryType.DEPOSIT, amount,
                        balance.subtract(amount), balance
                );
                entry.setCreatedAt(Instant.now().minusSeconds((walletCount - w) * 60 + e));
                testEntries.add(entry);
            }
        }
        ledgerEntryRepository.saveAll(testEntries);

        // When
        int queryThreadCount = 20;
        int queriesPerThread = 100;
        CountDownLatch latch = new CountDownLatch(queryThreadCount);
        AtomicInteger queryCount = new AtomicInteger(0);
        AtomicLong totalQueryTime = new AtomicLong(0);

        List<Future<?>> futures = new ArrayList<>();
        
        for (int t = 0; t < queryThreadCount; t++) {
            Future<?> future = executorService.submit(() -> {
                try {
                    long startTime = System.nanoTime();
                    
                    for (int q = 0; q < queriesPerThread; q++) {
                        long walletId = ThreadLocalRandom.current().nextLong(0, walletCount);
                        Instant timestamp = Instant.now().minusSeconds(ThreadLocalRandom.current().nextLong(0, 3600));
                        
                        try {
                            ledgerService.getWalletBalanceAt(walletId, timestamp);
                            queryCount.incrementAndGet();
                        } catch (Exception e) {
                            // Log but don't fail the test for occasional query errors
                        }
                    }
                    
                    long duration = System.nanoTime() - startTime;
                    totalQueryTime.addAndGet(duration);
                    
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // Wait for completion
        assertTrue(latch.await(30, TimeUnit.SECONDS), "Query test should complete within 30 seconds");
        
        // Verify all futures completed
        futures.forEach(future -> {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Query future should complete without exception: " + e.getMessage());
            }
        });

        // Then
        int expectedQueries = queryThreadCount * queriesPerThread;
        assertTrue(queryCount.get() >= expectedQueries * 0.95, "At least 95% of queries should succeed");
        
        // Performance assertions
        double avgQueryTimeMs = totalQueryTime.get() / (double) queryCount.get() / 1_000_000;
        assertTrue(avgQueryTimeMs < 100, "Average query time should be less than 100ms");
    }

    @Test
    @DisplayName("Should handle large dataset pagination efficiently")
    void largeDatasetPagination_ShouldMaintainPerformance() {
        // Given
        int totalEntries = 10000;
        int walletId = 1L;
        
        // Create large dataset
        List<LedgerEntry> entries = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        
        for (int i = 0; i < totalEntries; i++) {
            BigDecimal amount = new BigDecimal("1.00");
            balance = balance.add(amount);
            
            LedgerEntry entry = LedgerTestDataFactory.createTestEntry(
                    walletId, LedgerEntryType.DEPOSIT, amount,
                    balance.subtract(amount), balance
            );
            entry.setCreatedAt(Instant.now().minusSeconds(totalEntries - i));
            entries.add(entry);
        }
        
        long startTime = System.currentTimeMillis();
        ledgerEntryRepository.saveAll(entries);
        long insertTime = System.currentTimeMillis() - startTime;
        
        // When
        int pageSize = 100;
        int totalPages = (int) Math.ceil((double) totalEntries / pageSize);
        
        startTime = System.currentTimeMillis();
        List<Page<LedgerEntry>> pages = new ArrayList<>();
        
        for (int page = 0; page < totalPages && page < 50; page++) { // Limit to first 50 pages for test performance
            PageRequest pageRequest = PageRequest.of(page, pageSize);
            Page<LedgerEntry> result = ledgerService.getEntriesForWallet(walletId, pageRequest);
            pages.add(result);
        }
        
        long queryTime = System.currentTimeMillis() - startTime;

        // Then
        assertTrue(insertTime < 10000, "Bulk insert should complete within 10 seconds");
        assertTrue(queryTime < 5000, "Pagination queries should complete within 5 seconds");
        
        // Verify pagination correctness
        for (int i = 0; i < pages.size() - 1; i++) {
            assertEquals(pageSize, pages.get(i).getContent().size());
        }
        
        // Verify ordering (newest first)
        if (!pages.isEmpty()) {
            List<LedgerEntry> firstPage = pages.get(0).getContent();
            for (int i = 0; i < firstPage.size() - 1; i++) {
                assertTrue(firstPage.get(i).getCreatedAt().isAfter(firstPage.get(i + 1).getCreatedAt()));
            }
        }
    }

    @Test
    @DisplayName("Should handle concurrent wallet state rebuilding efficiently")
    void concurrentStateRebuilding_ShouldMaintainPerformance() throws InterruptedException {
        // Given
        int walletCount = 50;
        int entriesPerWallet = 100;
        
        // Create test data
        List<LedgerEntry> testEntries = new ArrayList<>();
        for (int w = 0; w < walletCount; w++) {
            List<LedgerEntry> walletEntries = LedgerTestDataFactory.createTestEntriesSeries(
                    (long) w, entriesPerWallet);
            testEntries.addAll(walletEntries);
        }
        ledgerEntryRepository.saveAll(testEntries);

        // When
        int rebuildThreadCount = 10;
        CountDownLatch latch = new CountDownLatch(rebuildThreadCount);
        AtomicInteger rebuildCount = new AtomicInteger(0);
        AtomicLong totalRebuildTime = new AtomicLong(0);

        List<Future<?>> futures = new ArrayList<>();
        
        for (int t = 0; t < rebuildThreadCount; t++) {
            Future<?> future = executorService.submit(() -> {
                try {
                    long startTime = System.nanoTime();
                    
                    for (int w = 0; w < walletCount; w++) {
                        try {
                            ledgerService.rebuildWalletState((long) w);
                            rebuildCount.incrementAndGet();
                        } catch (Exception e) {
                            // Log but don't fail for occasional rebuild errors
                        }
                    }
                    
                    long duration = System.nanoTime() - startTime;
                    totalRebuildTime.addAndGet(duration);
                    
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // Wait for completion
        assertTrue(latch.await(60, TimeUnit.SECONDS), "Rebuild test should complete within 60 seconds");
        
        // Verify all futures completed
        futures.forEach(future -> {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Rebuild future should complete without exception: " + e.getMessage());
            }
        });

        // Then
        int expectedRebuilds = rebuildThreadCount * walletCount;
        assertTrue(rebuildCount.get() >= expectedRebuilds * 0.95, "At least 95% of rebuilds should succeed");
        
        // Performance assertions
        double avgRebuildTimeMs = totalRebuildTime.get() / (double) rebuildCount.get() / 1_000_000;
        assertTrue(avgRebuildTimeMs < 200, "Average rebuild time should be less than 200ms");
    }

    @Test
    @DisplayName("Should handle memory usage efficiently with large datasets")
    void memoryUsage_ShouldRemainStable() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create and process large dataset
        int iterations = 5;
        int entriesPerIteration = 2000;
        
        for (int iter = 0; iter < iterations; iter++) {
            // Create entries
            List<LedgerEntry> entries = LedgerTestDataFactory.createTestEntriesSeries(
                    (long) iter, entriesPerIteration);
            
            // Save to database
            ledgerEntryRepository.saveAll(entries);
            
            // Query and process entries
            Page<LedgerEntry> page = ledgerService.getEntriesForWallet(
                    (long) iter, PageRequest.of(0, 100));
            
            // Rebuild state
            ledgerService.rebuildWalletState((long) iter);
            
            // Clear references
            entries.clear();
        }
        
        // Force garbage collection
        System.gc();
        System.gc();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Then
        long maxMemoryIncrease = 100 * 1024 * 1024; // 100MB
        assertTrue(memoryIncrease < maxMemoryIncrease, 
                "Memory increase should be less than 100MB, was: " + (memoryIncrease / 1024 / 1024) + "MB");
    }
}

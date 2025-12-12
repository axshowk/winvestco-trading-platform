package in.winvestco.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Stress test endpoint for Virtual Threads verification.
 * <p>
 * This controller provides endpoints to test and verify that virtual threads
 * are working correctly under high concurrency scenarios.
 * </p>
 * 
 * <b>WARNING:</b> This should be disabled in production environments.
 */
@RestController
@RequestMapping("/api/stress-test")
public class StressTestController {

    /**
     * Runs a stress test simulating concurrent I/O-bound operations.
     * 
     * @param concurrentTasks Number of concurrent tasks to run (default: 1000)
     * @param sleepMs         Simulated I/O delay in milliseconds (default: 100)
     * @return Test results including timing and thread information
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> runStressTest(
            @RequestParam(defaultValue = "1000") int concurrentTasks,
            @RequestParam(defaultValue = "100") long sleepMs) {

        Instant start = Instant.now();

        List<CompletableFuture<String>> futures = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < concurrentTasks; i++) {
                final int taskId = i;
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        // Simulate I/O-bound operation (e.g., database call, HTTP request)
                        Thread.sleep(sleepMs);
                        return String.format("Task-%d completed on %s (virtual=%s)",
                                taskId,
                                Thread.currentThread().getName(),
                                Thread.currentThread().isVirtual());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "Task-" + taskId + " interrupted";
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for all tasks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        Duration elapsed = Duration.between(start, Instant.now());

        // Count virtual vs platform threads
        long virtualCount = futures.stream()
                .map(CompletableFuture::join)
                .filter(s -> s.contains("virtual=true"))
                .count();

        return ResponseEntity.ok(Map.of(
                "status", "completed",
                "concurrentTasks", concurrentTasks,
                "sleepPerTaskMs", sleepMs,
                "totalTimeMs", elapsed.toMillis(),
                "theoreticalSequentialTimeMs", concurrentTasks * sleepMs,
                "speedupFactor", String.format("%.2fx", (double) (concurrentTasks * sleepMs) / elapsed.toMillis()),
                "virtualThreadsUsed", virtualCount,
                "platformThreadsUsed", concurrentTasks - virtualCount,
                "currentThreadInfo", Map.of(
                        "name", Thread.currentThread().getName(),
                        "isVirtual", Thread.currentThread().isVirtual()),
                "sampleResults", futures.stream()
                        .limit(5)
                        .map(CompletableFuture::join)
                        .toList()));
    }

    /**
     * Quick health check to verify virtual threads are enabled.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getVirtualThreadInfo() {
        Thread currentThread = Thread.currentThread();

        return ResponseEntity.ok(Map.of(
                "currentThread", Map.of(
                        "name", currentThread.getName(),
                        "isVirtual", currentThread.isVirtual(),
                        "threadId", currentThread.threadId()),
                "javaVersion", System.getProperty("java.version"),
                "virtualThreadsSupported", true,
                "availableProcessors", Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Test endpoint that simulates a blocking database call.
     * Use this to verify request handling uses virtual threads.
     * 
     * @param delayMs Simulated delay in milliseconds
     */
    @GetMapping("/simulate-io")
    public ResponseEntity<Map<String, Object>> simulateBlockingIO(
            @RequestParam(defaultValue = "500") long delayMs) throws InterruptedException {

        Instant start = Instant.now();
        Thread thread = Thread.currentThread();

        // Simulate blocking I/O
        Thread.sleep(delayMs);

        return ResponseEntity.ok(Map.of(
                "operation", "simulated_blocking_io",
                "delayMs", delayMs,
                "actualDelayMs", Duration.between(start, Instant.now()).toMillis(),
                "handledBy", Map.of(
                        "threadName", thread.getName(),
                        "isVirtual", thread.isVirtual(),
                        "threadId", thread.threadId())));
    }
}

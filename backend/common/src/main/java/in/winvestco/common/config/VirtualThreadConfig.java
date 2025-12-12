package in.winvestco.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * Configuration for Virtual Threads (Java 21+).
 * <p>
 * This configuration enables virtual threads for all async operations in the
 * application.
 * Virtual threads are lightweight threads that allow for efficient handling of
 * blocking
 * I/O operations without the overhead of platform threads.
 * </p>
 * <p>
 * Benefits of Virtual Threads:
 * <ul>
 * <li>Near-unlimited concurrency for I/O-bound tasks</li>
 * <li>Reduced memory footprint compared to platform threads</li>
 * <li>No need for complex reactive programming patterns</li>
 * <li>Simplified code with blocking-style syntax</li>
 * </ul>
 * </p>
 * 
 * @see java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()
 */
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "true", matchIfMissing = false)
public class VirtualThreadConfig {

    /**
     * Primary async task executor using virtual threads.
     * <p>
     * This executor creates a new virtual thread for each task submitted.
     * Virtual threads are multiplexed onto a small number of carrier threads
     * managed by the JVM, making them extremely lightweight.
     * </p>
     *
     * @return AsyncTaskExecutor backed by virtual threads
     */
    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Application-level task executor for general async operations.
     * <p>
     * This can be used with @Async("applicationTaskExecutor") or as the default
     * executor for Spring's async processing.
     * </p>
     *
     * @return AsyncTaskExecutor backed by virtual threads
     */
    @Bean(name = "applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Dedicated executor for scheduled tasks.
     * <p>
     * Note: Virtual threads are not recommended for CPU-bound scheduled tasks
     * that run frequently. Use this for I/O-bound scheduled operations.
     * </p>
     *
     * @return AsyncTaskExecutor backed by virtual threads
     */
    @Bean(name = "scheduledTaskExecutor")
    public AsyncTaskExecutor scheduledTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}

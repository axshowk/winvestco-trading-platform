# 6. Java 21 Virtual Threads

**Date:** 2024-01-15

## Status

Accepted

## Context

A trading platform handles many concurrent I/O-bound operations:
- Database queries across multiple services
- HTTP calls between services (Feign clients)
- Message broker interactions (RabbitMQ, Kafka)
- External API calls (NSE India, Razorpay)

Traditional Java threading creates platform threads that are expensive (1-2MB stack each) and limited by OS resources. Under heavy load, thread pools become saturated or require extensive tuning.

Java 21 introduced **Virtual Threads** as a production-ready feature, offering lightweight threads managed by the JVM rather than the OS.

## Decision

We will use **Java 21 Virtual Threads** across all services:

### Configuration
All services are configured with:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### Benefits Demonstrated
Stress tests on User Service showed exceptional results:

| Concurrent Tasks | Total Time | Speedup Factor |
|------------------|------------|----------------|
| 5,000 | 2,262ms | 221x |
| 10,000 | 1,160ms | 862x |
| 20,000 | 816ms | 1,225x |
| 50,000 | 1,354ms | **1,846x** |

All tests achieved 100% virtual thread utilization with zero platform thread blocking.

## Consequences

### Positive
- Handle millions of concurrent connections without thread pool tuning
- Simpler code (straightforward blocking I/O, no reactive complexity)
- Lower memory footprint per request
- Better CPU utilization during I/O waits
- Seamless integration with existing Spring Boot code

### Negative
- Requires Java 21 (higher deployment requirement)
- Some libraries may not yet be virtual-thread-friendly
- Pinning issues with synchronized blocks (must use ReentrantLock)
- ThreadLocal usage patterns may need review

### Mitigations
- All services standardized on Java 21
- Avoid synchronized blocks in hot paths
- Monitor for pinning via JFR events
- Use ReentrantLock for critical sections

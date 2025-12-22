# 3. Dual Message Broker Strategy

**Date:** 2024-01-15

## Status

Accepted

## Context

The platform has two distinct messaging requirements:

1. **Market Data Streaming**: High-throughput, real-time stock price updates from NSE India that need to be consumed by multiple services with different consumption rates.

2. **Domain Events**: Reliable delivery of business events (order created, funds deposited, etc.) that may require complex routing, retries, and dead-letter handling.

Using a single message broker for both use cases would force compromises. Kafka excels at high-throughput streaming but has simpler routing. RabbitMQ excels at complex routing and reliability but isn't optimized for high-throughput streaming.

## Decision

We will use **two message brokers**, each optimized for its use case:

### Apache Kafka
- **Purpose**: Market data streaming
- **Use Cases**:
  - Real-time stock price updates
  - Index data (NIFTY 50, etc.)
  - OHLC candle data
- **Benefits for this use case**:
  - High throughput for continuous price updates
  - Consumer groups for parallel processing
  - Log compaction for latest price retention
  - Replay capability for missed data

### RabbitMQ  
- **Purpose**: Domain event messaging
- **Use Cases**:
  - User events (created, updated, login)
  - Order lifecycle events (created, validated, filled, cancelled)
  - Funds events (deposited, withdrawn, locked, released)
  - Trade events (created, placed, executed, closed)
  - Payment events (created, success, failed)
  - Report events (requested, completed)
- **Benefits for this use case**:
  - Flexible topic-based routing with exchanges
  - Built-in retry and dead-letter queues
  - Acknowledgment for reliability
  - Per-message TTL for expiry

## Consequences

### Positive
- Each broker is used for its strengths
- Market data streaming doesn't interfere with domain event processing
- Independent scaling of streaming vs. event infrastructure
- Clear separation of concerns in the codebase

### Negative
- Two messaging systems to operate and monitor
- Increased infrastructure complexity
- Developers need to understand both systems
- Higher resource requirements

### Mitigations
- Centralized configuration in `common` module
- Consistent event naming conventions
- Docker Compose handles both brokers for local development
- Grafana dashboards for unified monitoring

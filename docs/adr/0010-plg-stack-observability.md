# 10. PLG Stack for Observability

**Date:** 2024-01-15

## Status

Accepted

## Context

A microservices architecture with 12 services creates observability challenges:
- Where did a request fail in a distributed flow?
- Which service is causing latency?
- What's the current state of the system?
- How do we debug production issues?

Without proper observability, troubleshooting becomes nearly impossible.

## Decision

We adopt the **PLG Stack** (Prometheus, Loki, Grafana) for comprehensive observability:

### Prometheus (Metrics)
**Port: 9090**

Collects and stores time-series metrics:
- JVM metrics (memory, GC, threads)
- HTTP request metrics (latency, status codes)
- Custom business metrics
- Database connection pool stats
- Message broker metrics

All services expose `/actuator/prometheus` endpoint via Micrometer.

### Loki (Logs)
**Port: 3100**

Log aggregation optimized for cost-effectiveness:
- Collects logs from all services
- Labels-based indexing (service, level, trace_id)
- LogQL for querying
- Integrates with Grafana for visualization

Structured JSON logging via Logback.

### Grafana (Visualization)
**Port: 3000**

Unified dashboards and alerting:
- Pre-built dashboards per service
- Business metrics dashboards
- Log search and correlation
- Alert rules and notifications

### Distributed Tracing
- Correlation IDs propagated across services
- Trace context in log entries
- Micrometer integration for spans

## Consequences

### Positive
- Single pane of glass for all observability
- Correlate logs, metrics, and traces
- Quick root cause analysis
- Proactive alerting on anomalies
- Historical data for capacity planning

### Negative
- Additional infrastructure to maintain
- Storage costs for metrics and logs
- Learning curve for teams
- Need meaningful dashboards and alerts

### Mitigations
- Docker Compose for local observability stack
- Pre-configured Grafana dashboards
- Retention policies to manage storage
- Runbooks linked from alerts

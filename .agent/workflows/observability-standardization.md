---
description: Context and workflow for standardizing observability metrics across all microservices.
---

# Observability Standardization Context

This context file is designed for Antigravity agents working in parallel to roll out a standardized observability stack (Metrics, Logging, Tracing) across the `winvestco-trading-platform` microservices.

## 🏗️ Architecture & Stack Overview
The platform uses an integrated PLG (Prometheus, Loki, Grafana) + Jaeger stack:
- **Metrics**: Micrometer + Prometheus (`micrometer-registry-prometheus`).
- **Distributed Tracing**: OpenTelemetry OTLP + Jaeger.
- **Logging**: SLF4J + Logback + Loki (`loki-logback-appender`).
- **Global Config**: Actuator and Micrometer configurations are centralized in `winvestco-backend/common`.

## 🎯 The Objective
Not all modules are correctly generating custom business metrics. We need every core service to instrument explicitly using:
1. **Counters**: For tracking discrete events and calculating rates (e.g., successful/failed payments, total orders, events processed).
2. **Timers (Histograms)**: For capturing operation latency/duration (e.g., execution time, network latency).
3. **Gauges**: (Currently MISSING) For tracking point-in-time state (e.g., active elements, locked funds, queue sizes, active consumers).

*Note: Summaries are heavily discouraged; refactor any existing Summaries into Timers (or Gauges if measuring lag).*

## 🛠️ Step-by-Step Implementation Workflow for Agents
When picking up a module, follow these steps:

1. **Verify Dependencies**: Ensure the module depends on `common` (which brings in micrometer/actuator/otel) or explicitly contains them in its `pom.xml`.
2. **Setup MeterRegistry**: Inject `io.micrometer.core.instrument.MeterRegistry` into core service beans.
3. **Instrument Counters**: Add `.counter("metric.name", "tag_key", "tag_value").increment()` to track success/failure events.
4. **Instrument Timers**: Wrap method execution in a `.timer()` logic or use `@Timed` (requires AspectJ configuration). Alternatively, use `Observation` API to get tracing and metrics concurrently.
5. **Instrument Gauges**: Use `.gauge("metric.name", objectToWatch, methodToFetchValue)` to expose current state (e.g., `meterRegistry.gauge("active.orders", orderRepository, repo -> repo.countActive())`).
6. **Prometheus Config Check**: Verify the service is listed in `observability/prometheus/prometheus.yml` under `scrape_configs`.
7. **Mark Complete**: Update the checklist below to `[x]` to prevent duplicate work by parallel agents.

## 📋 Microservice Checklist & Status

### 🔴 No Custom Metrics Implemented (Must be added!)
- [ ] `api-gateway` (Metrics: request counts, latency, route metrics)
- [ ] `ledger-service` (Metrics: active transactions, ledger append time)
- [ ] `notification-service` (Metrics: messages sent/failed, queue lag)
- [ ] `report-service` (Metrics: reports generated, report generation latency)
- [ ] `risk-service` (Metrics: risk checks passed/failed, evaluation latency, active risk alerts)
- [ ] `schedule-service` (Metrics: jobs executed, job execution time)
- [ ] `user-service` (Metrics: active users, login success/failure)

### 🟡 Partially Implemented (Needs expansion & Gauges)
- [ ] `trade-service` (Needs Gauges for active trades, open positions)
- [ ] `order-service` (Needs Timers, Needs Gauges for active orders)
- [ ] `portfolio-service` (Needs Timers, Needs Gauges for portfolio values/holdings count)
- [ ] `payment-service` (Needs Timers, Needs Gauges for pending payments)
- [ ] `market-service` (Needs Timers, Needs Gauges for active market data subscriptions)
- [ ] `funds-service` (Needs Counters for total lock/release requests, Needs Gauges for currently locked balances)

## 📌 Rules for Parallel Execution
1. Always run `git pull` or refresh this file before picking a module.
2. Select an unchecked `[ ]` module, and temporarily check it off `[x] (Agent ID/Name)` while working to lock it.
3. Keep tags consistent (e.g., standardizing `service` tag, `status` tag, `type` tag).
4. Run tests and ensure the app still compiles (`mvn clean install -DskipTests` or run tests locally).

# Market Kafka Contract v1

This document defines the Phase 1 contract and topic foundation for Option A.

## Topics

- `market.quote.v1`
  - Purpose: canonical per-symbol quote updates for downstream services.
  - Key: uppercase `symbol` (for per-symbol ordering).
  - Value: `QuoteUpdatedEvent` (protobuf).
- `market.index.v1`
  - Purpose: index-level snapshots and optional constituent data.
  - Key: uppercase `index_symbol`.
  - Value: `IndexSnapshotEvent` (protobuf).
- `market.data.updates` (legacy)
  - Purpose: compatibility during migration only.

## Event Contracts

- Source protobuf file: `backend/common/src/main/proto/market_events.proto`.
- Java package: `in.winvestco.common.kafka.market`.
- Messages:
  - `QuoteUpdatedEvent`
  - `IndexSnapshotEvent`
  - `TradingStatusEvent` (optional status stream)

Common envelope-like fields present on each event:
- `event_id`
- `event_version`
- `event_time`
- `source`
- `trace_id`

## Delivery And Ordering

- Delivery semantics: at-least-once.
- Partitioning: by symbol/index key.
- Ordering guarantee: in-order per key within a partition.
- Consumer requirement: idempotent handling using `event_id` dedupe.

## Compatibility Rules

- Versioning model: topic version suffix (`*.v1`) and payload `event_version`.
- Allowed protobuf changes for v1:
  - add new optional fields only
  - never reuse or renumber field tags
  - never change field type or semantic meaning
- Breaking changes require a new versioned topic (`*.v2`).

## Retention And Replication Defaults

- Local/dev defaults:
  - quote topic partitions: 12, replicas: 1
  - index topic partitions: 6, replicas: 1
- Staging/prod should override via configuration per environment.

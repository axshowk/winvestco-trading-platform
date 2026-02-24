# Winvestco Project Analysis — Improvement Plan

## Priority Matrix

| Priority | Improvement | Effort | Impact |
|:---:|:---|:---:|:---:|
| 🔴 P0 | Re-enable CI/CD pipeline | Low | Very High |
| 🔴 P0 | Tests for risk/trade/payment/user/schedule services | High | Very High |
| 🔴 P1 | Add Resilience4j to all Feign clients | Medium | High |
| 🟡 P1 | Replace broad `catch(Exception)` | Medium | High |
| 🟡 P2 | Frontend auth guards + more tests | Medium | Medium |
| 🟡 P2 | Fix Kafka Zookeeper/KRaft inconsistency | Low | Medium |
| 🟢 P3 | Fix `.env.example` MySQL reference | Trivial | Low |
| 🟢 P3 | Add PostgreSQL volume persistence | Trivial | Medium |
| 🟢 P3 | Deduplicate SecurityConfig | Medium | Low |
| 🟢 P3 | Dependency version upgrades | Medium | Medium |
| 🟢 P3 | Clean up committed log files | Trivial | Low |

## Services Missing Tests

- **risk-service** — 0 test files, no test directory
- **schedule-service** — 0 test files, no test directory
- **user-service** — 0 test files (has auth, JWT, roles — most security-critical)
- **payment-service** — 0 test files (handles real money)
- **trade-service** — 0 test files (core trading execution)

## Key Findings

- CI disabled (workflow_dispatch only)
- Resilience4j only in market-service NseClient
- 50+ files with broad catch(Exception)
- Frontend: only 6 test files, no route guards
- .env.example references MySQL instead of PostgreSQL
- Docker Compose uses Zookeeper despite README saying KRaft
- Log files committed to repo

# 9. API Gateway Pattern

**Date:** 2024-01-15

## Status

Accepted

## Context

With 12 microservices, clients (frontend, mobile apps) face challenges:
- Need to know addresses of multiple services
- Must handle authentication with each service
- Cross-cutting concerns duplicated everywhere
- Service discovery complexity exposed to clients

Direct client-to-service communication creates tight coupling and operational complexity.

## Decision

We implement **Spring Cloud Gateway** as the single entry point for all API requests:

### Responsibilities

| Concern | Implementation |
|---------|----------------|
| **Routing** | Path-based routing to backend services |
| **Authentication** | JWT validation for protected endpoints |
| **Authorization** | Role-based access control |
| **Rate Limiting** | Per-user/IP request throttling |
| **Load Balancing** | Client-side via Eureka integration |
| **SSL Termination** | HTTPS at the edge |

### Route Configuration

```
/api/auth/**      → user-service
/api/users/**     → user-service  
/api/market/**    → market-service
/api/portfolios/**→ portfolio-service
/api/funds/**     → funds-service
/api/ledger/**    → ledger-service
/api/orders/**    → order-service
/api/trades/**    → trade-service
/api/payments/**  → payment-service
/api/v1/notifications/** → notification-service
/ws/notifications/** → notification-service (WebSocket)
```

### Security
- JWT tokens validated at gateway level
- Public endpoints explicitly whitelisted
- X-User-Id header injected for downstream services
- Webhook endpoints bypass JWT for external callbacks

## Consequences

### Positive
- Single entry point simplifies client integration
- Centralized security enforcement
- Cross-cutting concerns in one place
- Service discovery hidden from clients
- Simplified SSL/TLS certificate management

### Negative
- Single point of failure (requires HA setup)
- Additional network hop for all requests
- Gateway must be scaled for throughput
- Configuration complexity for routing rules

### Mitigations
- Horizontal scaling behind load balancer
- Circuit breaker for downstream failures
- Health checks for automatic failover
- Configuration versioned in code

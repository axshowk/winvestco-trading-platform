# 🚀 WINVESTCO Trading Platform

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.2-blue.svg)](https://reactjs.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

**A modern, cloud-native stock trading platform built with microservices architecture**

[Features](#-features) • [Architecture](#-architecture) • [Quick Start](#-quick-start) • [API Documentation](#-api-documentation) • [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Microservices Overview](#-microservices-overview)
- [Project Structure](#-project-structure)
- [Quick Start](#-quick-start)
  - [Prerequisites](#prerequisites)
  - [Running with Docker](#running-with-docker-compose-recommended)
  - [Running Locally](#running-locally-for-development)
- [Service Configuration](#-service-configuration)
- [API Documentation](#-api-documentation)
- [Environment Variables](#-environment-variables)
- [Testing](#-testing)
- [Performance Benchmarks](#-performance-benchmarks)
- [Deployment](#-deployment)
- [Architecture Decision Records](#-architecture-decision-records)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Features

### Core Trading Features
- **📈 Live Market Data** - Real-time stock prices and indices from NSE India
- **📊 Interactive Charts** - TradingView & Lightweight Charts with drawing tools (horizontal lines, trendlines)
- **🔐 User Authentication** - Secure JWT-based authentication with OAuth2/Google support
- **👤 User Management** - Complete user registration, login, and profile management
- **💼 Portfolio Management** - Track holdings, P&L calculations, and investment performance
- **💰 Funds Management** - Wallet system with deposits, withdrawals, and funds locking with CQRS read model
- **📒 Full Event Sourcing Ledger** - Immutable financial facts, point-in-time queries, and state rebuild capability (Source of Truth)
- **📋 Order Management** - Complete order lifecycle with advanced order types (LIMIT, MARKET, SL) and product types (CNC, MIS, NRML)
- **📈 Trade Execution** - Trade lifecycle management with state machine (CREATED → VALIDATED → PLACED → EXECUTED → CLOSED)
- **💳 Payment Gateway** - Razorpay integration for deposits with webhook verification
- **🔔 Real-time Notifications** - WebSocket-based push notifications
- **📄 Stock Details** - Comprehensive stock information with interactive charts
- **📄 Async Report Generation** - Generate P&L, Tax, and Transaction reports asynchronously via Event Sourcing
- **🌓 Dark/Light Mode** - Personalized UI experience with smooth theme transitions
- **🖥️ Trading Terminal** - Advanced stock-specific terminal view with professional charting
- **📱 Responsive Design** - Mobile-first, modern UI built with React

### Technical Highlights
- **☁️ Cloud-Native Architecture** - 13 microservices with service discovery and API gateway
- **🔄 Event-Driven Communication** - Kafka for market data streaming + RabbitMQ for domain events (30+ events)
- **🔀 SAGA Orchestration** - Choreography-based distributed transactions with compensation logic for order-to-trade lifecycle
- **📨 Message Queue Reliability** - Idempotency service, Outbox pattern, DLQ with retry interceptor for guaranteed delivery
- **💾 Redis Caching** - High-performance caching for market data and sessions
- **📝 Database Migrations** - Flyway for version-controlled schema management
- **🛡️ API Security** - OAuth2/JWT authentication with Spring Security, Redis-backed rate limiting, and secure headers (CSP, HSTS)
- **📖 API Documentation** - OpenAPI/Swagger UI for all REST endpoints with centralized aggregation capability
- **🐳 Optimized Docker Support** - Multi-stage builds, non-root users, health checks, and JVM tuning for all 13 services
- **⚡ Virtual Threads** - Java 21 Virtual Threads for optimal high-concurrency performance
- **📊 Observability** - Full PLG Stack (Prometheus, Loki, Grafana) + Jaeger for metrics, logging & distributed tracing
- **🔁 Event Sourcing Ready** - Domain events for all key business actions with correlation IDs and state rebuild capability
- **🛡️ Resilience4j Integration** - Circuit breakers, distributed rate limiters, retries with exponential backoff and jitter
- **🔧 Audit & Service Logging** - Aspect-Oriented Programming (AOP) for consistent logging across all services
- **🔧 Mock Execution Engine** - Simulated trade execution for development and testing
- **🌐 Environment-Specific Profiles** - 48 profile files (dev, docker, staging, prod) for secure and flexible deployment
- **📝 Structured Logging** - JSON-formatted logging with consistent fields across all services for better log aggregation
- **⚡ Redis-backed Rate Limiting** - Per-IP, per-User, and Combined rate limiting at the API Gateway (10 req/s, 20 burst)
- **🧪 Full-Stack Testing** - JUnit 5/Mockito for backend + Vitest/React Testing Library for frontend (70+ tests)
- **📊 Code Coverage Reporting** - JaCoCo (Backend) and Vitest (Frontend) for detailed coverage visualization
- **📨 Reliable Event Publishing** - Outbox pattern and Idempotency Service for guaranteed "at-least-once" delivery
- **🚀 Automated CI/CD** - GitHub Actions for automated building, testing, Docker image pushing, and SSH deployment

---

## 🏗 Architecture

```
┌───────────────────────────────────────────────────────────────────────────────────────────-┐
│                                    WINVESTCO PLATFORM                                      │
├─────────────────────────────────────────────────────────────────────────────────────────── ┤
│                                                                                            │
│  ┌─────────────┐              ┌────────────────────────────────────────────────────┐       │
│  │   React     │ ───────────► │                 API Gateway (8090)                 │       │
│  │  Frontend   │              │  • JWT Validation  • Rate Limiting  • OAuth2       │       │
│  │   (5173)    │              │  • Load Balancing  • Route Management              │       │
│  └─────────────┘              └─────────────────────────┬──────────────────────────┘       │
│                                                         │                                  │
│         ┌───────────────────────────────────────────────┼───────────────────────┐          │
│         │                                               │                       │          │
│         ▼                                               ▼                       ▼          │
│  ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐   ┌─────────────────┐  │
│  │ User Service     │   │ Market Service   │   │ Portfolio Service│   │ Funds Service   │  │
│  │     (8088)       │   │     (8084)       │   │     (8085)       │   │    (8086)       │  │
│  │                  │   │                  │   │                  │   │                 │  │
│  │ • Authentication │   │ • NSE India API  │   │ • Holdings Mgmt  │   │ • Wallet Mgmt   │  │
│  │ • Registration   │   │ • Live Data      │   │ • P&L Tracking   │   │ • Deposits      │  │
│  │ • Profile Mgmt   │   │ • Stock Quotes   │   │ • Event-Driven   │   │ • Withdrawals   │  │
│  │ • JWT Generation │   │ • Candle Data    │   │                  │   │ • Funds Locking │  │
│  └────────┬─────────┘   └────────┬─────────┘   └────────┬─────────┘   └────────┬────────┘  │
│           │                      │                      │                      │           │
│  ┌────────┴──────────────────────┴──────────────────────┴──────────────────────┴──────── ┐ │
│  │                                                                                       │ |
│  │  ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐ │
│  │  │ Ledger Service   │   │ Order Service    │   │ Trade Service    │   │ Report Service   │ │
│  │  │     (8087)       │   │     (8089)       │   │     (8092)       │   │     (8094)       │ │
│  │  │                  │   │                  │   │                  │   │ • Async Reporting│ │
│  │  │ • Immutable SOT  │   │ • Order Lifecycle│   │ • Trade Lifecycle│   │ • P&L/Tax Reports│ │
│  │  │ • Audit Trail    │   │ • Market/Limit   │   │ • State Machine  │   │ • Read Models    │ │
│  │  └──────────────────┘   └──────────────────┘   └──────────────────┘   └──────────────────┘ │
│  │                                                                                       │ │
│  │  ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐                   │ │
│  │  │ Notification Svc │   │ Payment Service  │   │ Schedule Service │                   │ │
│  │  │     (8091)       │   │     (8093)       │   │     (8095)       │                   │ │
│  │  │                  │   │                  │   │                  │                   │ │
│  │  │ • Push Notifs    │   │ • Razorpay       │   │ • Platform Cron  │                   │ │
│  │  │ • WebSocket      │   │ • Webhooks       │   │ • Task Mgmt      │                   │ │
│  │  │ • Preferences    │   │ • Payment Events │   │ • Market Sync    │                   │ │
│  │  └──────────────────┘   └──────────────────┘   └──────────────────┘                   │ │
│  │                                                                                       │ │
│  └───────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                            │
│  ┌───────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                             Infrastructure Layer                                      │ │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────────┐ ┌─────────────────────┐│ │
│  │  │Postgres │ │  Redis  │ │RabbitMQ │ │  Kafka  │ │ Zookeeper │ │   Eureka (8761)     ││ │
│  │  │  (5432) │ │ (6379)  │ │  (5672) │ │ (9092)  │ │  (2181)   │ │ Service Discovery   ││ │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └───────────┘ └─────────────────────┘│ │
│  └───────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                            │
│  ┌───────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                          Observability (PLG Stack)                                    │ │
│  │  ┌───────────────────┐ ┌───────────────────┐ ┌───────────────────────────────────────┐│ │
│  │  │ Prometheus (9090) │ │   Loki (3100)     │ │          Grafana (3000)               ││ │
│  │  │ Metrics Collection│ │ Log Aggregation   │ │  Dashboards & Visualization           ││ │
│  │  └───────────────────┘ └───────────────────┘ └───────────────────────────────────────┘│ │
│  └───────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                            │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠 Technology Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Core language with Virtual Threads |
| **Spring Boot** | 3.2.0 | Application framework |
| **Spring Cloud** | 2023.0.0 | Microservices ecosystem |
| **Spring Security** | 6.x | Authentication & Authorization |
| **Spring Cloud Gateway** | - | API Gateway with reactive support |
| **Netflix Eureka** | - | Service Discovery |
| **OpenFeign** | - | Declarative REST client for inter-service communication |
| **Resilience4j** | - | Circuit Breaker, Rate Limiter, Retry, Bulkhead patterns |
| **Micrometer / OTel** | - | Distributed Tracing & OpenTelemetry integration |
| **MapStruct** | 1.6.3 | Type-safe bean mapping (Stable release) |

### Data & Messaging
| Technology | Purpose |
|------------|---------|
| **PostgreSQL 16** | Primary database (separate DB per service) |
| **Redis** | Caching & session management |
| **Apache Kafka** | Market data streaming |
| **RabbitMQ** | Async messaging for domain events |
| **Flyway** | Database migrations |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| **React** | 19.2 | UI Library |
| **Vite** | 7.2 | Build tool |
| **React Router** | 7.10 | Client-side routing |
| **Framer Motion** | 12.x | Animations |
| **Lucide React** | 0.555 | Icons |
| **Lightweight Charts** | 4.2 | Financial charts with drawing tools |
| **Vitest / RTL** | - | Frontend unit and component testing |
| **TradingView** | - | Advanced stock charts |

### DevOps & Tools
| Technology | Purpose |
|------------|---------|
| **Docker** | Containerization |
| **Docker Compose** | Multi-container orchestration |
| **Maven** | Build & dependency management (multi-module) |
| **Lombok** | Boilerplate reduction |
| **MapStruct** | Object mapping |
| **SpringDoc OpenAPI** | API documentation |

### Observability (PLG + J Stack)
| Technology | Purpose |
|------------|---------|
| **Prometheus** | Metrics collection & alerting |
| **Loki** | Log aggregation & querying |
| **Grafana** | Visualization & dashboards |
| **Jaeger** | Distributed tracing for microservices |
| **Micrometer** | Application metrics & tracing |
| **Logback** | Structured logging (JSON format) |
| **AOP** | Audit logging & service tracing aspects |

---

## 🔲 Microservices Overview

| Service | Port | Description | Database |
|---------|------|-------------|----------|
| **Eureka Server** | 8761 | Service discovery & registry | - |
| **API Gateway** | 8090 | Central entry point, routing, JWT validation, Redis-backed rate limiting | - |
| **User Service** | 8088 | Authentication, registration, user management | `winvestco_user_db` |
| **Market Service** | 8084 | Real-time market data, NSE API integration, candles | `winvestco_market_db` |
| **Portfolio Service** | 8085 | Holdings management, P&L tracking | `winvestco_portfolio_db` |
| **Funds Service** | 8086 | Wallet management, deposits, withdrawals, fund locking | `winvestco_funds_db` |
| **Ledger Service** | 8087 | Immutable financial ledger (source of truth) | `winvestco_ledger_db` |
| **Order Service** | 8089 | Order lifecycle management (create, cancel, fill, expire) | `winvestco_order_db` |
| **Trade Service** | 8092 | Trade lifecycle, execution, state machine | `winvestco_trade_db` |
| **Payment Service** | 8093 | Razorpay integration, payment lifecycle, webhooks | `winvestco_payment_db` |
| **Notification Service** | 8091 | Push notifications, WebSocket, preferences | `winvestco_notification_db` |
| **Report Service** | 8094 | Async report generation (P&L, Tax, Transaction) | `winvestco_report_db` |
| **Schedule Service** | 8095 | Centralized platform-wide task scheduling | - |
| **Common Module** | - | Shared library (DTOs, enums, events, security, configs) | - |

### Domain Events (RabbitMQ)

| Category | Events |
|----------|--------|
| **User Events** | `UserCreatedEvent`, `UserUpdatedEvent`, `UserLoginEvent`, `UserStatusChangedEvent`, `UserRoleChangedEvent`, `UserPasswordChangedEvent` |
| **Order Events** | `OrderCreatedEvent`, `OrderValidatedEvent`, `OrderFilledEvent`, `OrderCancelledEvent`, `OrderExpiredEvent`, `OrderRejectedEvent` |
| **Funds Events** | `FundsDepositedEvent`, `FundsWithdrawnEvent`, `FundsLockedEvent`, `FundsReleasedEvent` |
| **Trade Events** | `TradeCreatedEvent`, `TradePlacedEvent`, `TradeExecutedEvent`, `TradeClosedEvent`, `TradeCancelledEvent`, `TradeFailedEvent` |
| **Payment Events** | `PaymentCreatedEvent`, `PaymentSuccessEvent`, `PaymentFailedEvent`, `PaymentExpiredEvent` |
| **Report Events** | `ReportCompletedEvent`, `ReportFailedEvent` |
| **Ledger Events** | `LedgerEntryEvent` |

The platform uses an event-driven architecture with 30 domain events for robust inter-service communication.

### SAGA Pattern Architecture

The platform implements choreography-based SAGA for distributed transactions with compensation logic:

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                           ORDER-TO-TRADE SAGA FLOW                                       │
├──────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌─────────────┐    OrderValidatedEvent    ┌─────────────┐    FundsLockedEvent          │
│  │   ORDER     │ ─────────────────────────►│   FUNDS     │ ────────────────────┐        │
│  │   SERVICE   │                           │   SERVICE   │                     │        │
│  │             │◄───────────────────────── │             │◄──────────────┐     │        │
│  │ NEW→VALIDATED│   OrderRejectedEvent     │ Lock Funds  │               │     │        │
│  │ →FUNDS_LOCKED│   (Insufficient Funds)   └─────────────┘               │     ▼        │
│  │ →PENDING     │                                                        │  ┌─────────┐ │
│  └──────┬───────┘                                                        │  │ TRADE   │ │
│         │                                                                │  │ SERVICE │ │
│         │ OrderFilledEvent                 ┌─────────────┐               │  │         │ │
│         └─────────────────────────────────►│ PORTFOLIO   │               │  │ CREATED │ │
│                                            │ SERVICE     │               │  │→VALIDATED│ │
│  ┌─────────────────────────────────────────│             │               │  │→PLACED  │ │
│  │ TradeExecutedEvent                      │Update       │               │  │→EXECUTED│ │
│  │ (triggers position update)              │Holdings     │               │  └────┬────┘ │
│  │                                         └─────────────┘               │       │      │
│  │                                                                       │       │      │
│  │  COMPENSATION FLOWS:                                                  │       │      │
│  │  ─────────────────                                                    │       │      │
│  │  • TradeFailedEvent    → FundsService releases locked funds           │       │      │
│  │  • OrderCancelledEvent → FundsService releases locked funds           │       │      │
│  │  • OrderCancelledEvent → TradeService cancels trade                   │       │      │
│  │                                                                       │       │      │
└──┴───────────────────────────────────────────────────────────────────────┴───────┴──────┘
```

### Message Queue Reliability Infrastructure

| Component | Purpose |
|-----------|---------|
| **BaseEvent** | Abstract base with `correlationId` and `timestamp` for tracing & deduplication |
| **IdempotencyService** | Tracks processed events to prevent duplicate handling |
| **OutboxService** | Captures events in DB within same transaction for guaranteed delivery |
| **OutboxProcessor** | Background job publishes pending outbox events to RabbitMQ |
| **RetryInterceptor** | Automatic retries with exponential backoff (1s initial, 2x multiplier, 10s max) |
| **Dead Letter Queue** | Failed messages republished to DLQ after max retry attempts |

---

## 📁 Project Structure

```
winvestco-trading-platform/
├── 📄 LICENSE                    # MIT License
├── 📄 README.md                  # This file
│
├── 📁 backend/                   # Backend microservices (Maven multi-module)
│   ├── 📄 pom.xml                # Parent POM
│   ├── 📄 docker-compose.yml     # Full stack deployment
│   ├── 📄 docker-compose-services.yml  # External services only
│   ├── 📄 .env.example           # Environment variables template
│   │
│   ├── 📁 common/                # Shared library module
│   │   ├── 📁 config/            # Common configurations (Redis, Cache, Security, RabbitMQ)
│   │   ├── 📁 dto/               # Shared DTOs
│   │   ├── 📁 enums/             # Enumerations (17 enums: Order, Trade, Payment, Wallet, Ledger types)
│   │   ├── 📁 event/             # Domain events (26 events with BaseEvent for correlation IDs)
│   │   ├── 📁 exception/         # Global exception handling
│   │   ├── 📁 interceptor/       # Rate limiting interceptors
│   │   ├── 📁 messaging/         # Message reliability (IdempotencyService, OutboxService)
│   │   ├── 📁 security/          # JWT & auth utilities
│   │   ├── 📁 service/           # Shared services (Redis, RateLimit)
│   │   └── 📁 util/              # Logging & utility classes
│   │
│   ├── 📁 eureka-server/         # Service Discovery (Port: 8761)
│   │
│   ├── 📁 api-gateway/           # API Gateway (Port: 8090)
│   │   ├── 📁 config/            # Gateway & security config
│   │   ├── 📁 filter/            # Custom gateway filters
│   │   └── 📄 Dockerfile
│   │
│   ├── 📁 user-service/          # User Management (Port: 8088)
│   │   ├── 📁 controller/        # REST controllers
│   │   ├── 📁 service/           # Authentication, User management
│   │   ├── 📁 repository/        # Data access layer
│   │   ├── 📁 model/             # JPA entities
│   │   ├── 📁 dto/               # Request/Response DTOs
│   │   ├── 📁 mapper/            # MapStruct mappers
│   │   ├── 📁 security/          # UserDetails, JWT filters
│   │   └── 📄 Dockerfile
│   │
│   ├── 📁 market-service/        # Market Data (Port: 8084)
│   │   ├── 📁 client/            # NSE India API client
│   │   ├── 📁 controller/        # REST controllers
│   │   ├── 📁 service/           # Market data processing
│   │   ├── 📁 scheduler/         # Scheduled data fetching
│   │   ├── 📁 messaging/         # Kafka publisher
│   │   └── 📄 Dockerfile
│   │
│   ├── 📁 portfolio-service/     # Portfolio Management (Port: 8085)
│   │   ├── 📁 controller/        # REST controllers
│   │   ├── 📁 service/           # Portfolio & Holdings logic
│   │   ├── 📁 repository/        # Data access layer
│   │   ├── 📁 model/             # JPA entities (Portfolio, Holding)
│   │   ├── 📁 messaging/         # RabbitMQ event listeners
│   │   └── 📄 Dockerfile
│   │
│   ├── 📁 funds-service/         # Funds Management (Port: 8086)
│   │   ├── 📁 controller/        # Wallet, Transaction, Lock controllers
│   │   ├── 📁 service/           # Wallet, Transaction, FundsLock services
│   │   ├── 📁 client/            # Feign client for Ledger Service
│   │   ├── 📁 messaging/         # Event publisher/listeners
│   │   ├── 📁 model/             # Wallet, Transaction, FundsLock entities
│   │   └── 📄 Dockerfile
│   │
│   ├── 📁 ledger-service/        # Immutable Ledger (Port: 8087)
│   │   ├── 📁 controller/        # Ledger API (read-only)
│   │   ├── 📁 service/           # Append-only ledger operations
│   │   ├── 📁 repository/        # Ledger entry repository
│   │   ├── 📁 model/             # LedgerEntry entity (immutable)
│   │   └── 📄 Dockerfile
│   │
│   ├── 📁 order-service/         # Order Management (Port: 8089)
│   │   ├── 📁 controller/        # Order REST controllers
│   │   ├── 📁 service/           # Order lifecycle, expiry scheduling
│   │   ├── 📁 client/            # Feign clients (Market, Funds)
│   │   ├── 📁 messaging/         # Event listeners/publishers
│   │   ├── 📁 model/             # Order entity
│   │   └── 📄 Dockerfile
│   │
│   ├── 📁 trade-service/         # Trade Execution (Port: 8092)
│   │   ├── 📁 controller/        # Trade REST controllers
│   │   ├── 📁 service/           # Trade lifecycle, execution simulation
│   │   ├── 📁 messaging/         # Order event listeners, trade event publishers
│   │   ├── 📁 model/             # Trade entity with state machine
│   │   └── 📄 Dockerfile
│   │
│   ├── 📁 payment-service/       # Payment Gateway (Port: 8093)
│   │   ├── 📁 controller/        # Payment & Webhook controllers
│   │   ├── 📁 service/           # Razorpay integration, payment lifecycle
│   │   ├── 📁 messaging/         # Payment event publishers
│   │   ├── 📁 model/             # Payment entity
│   │   └── 📄 Dockerfile
│   │
│   └── 📁 notification-service/  # Notifications (Port: 8091)
│       ├── 📁 controller/        # Notification REST & Preference controllers
│       ├── 📁 service/           # Notification, Preference, WebSocket services
│       ├── 📁 websocket/         # WebSocket handlers & config
│       ├── 📁 messaging/         # Event listeners for all domain events
│       ├── 📁 model/             # Notification, Preference entities
│       └── 📄 Dockerfile
│
│   ├── 📁 report-service/        # Async Report Generation (Port: 8094)
│   │   ├── 📁 controller/        # Report request controllers
│   │   ├── 📁 service/           # Report generation logic
│   │   ├── 📁 repository/        # Projection tables (Lead/Trade read models)
│   │   ├── 📁 messaging/         # Event listeners for report triggers
│   │   ├── 📁 model/             # Report & Projection entities
│   │   └── 📄 Dockerfile
│   │
│   └── 📁 schedule-service/      # Centralized Scheduling (Port: 8095)
│       ├── 📁 scheduler/         # Centralized platform schedulers
│       └── 📄 Dockerfile
│
├── 📁 frontend/                  # React Frontend (Vite)
│   ├── 📁 src/
│   │   ├── 📁 components/        # Reusable UI components
│   │   │   ├── Navbar.jsx
│   │   │   ├── Hero.jsx
│   │   │   ├── Footer.jsx
│   │   │   ├── Ticker.jsx
│   │   │   ├── TradingViewChart.jsx  # TradingView financial charts
│   │   │   ├── NotificationBell.jsx
│   │   │   └── NotificationToast.jsx
│   │   ├── 📁 pages/             # Page components
│   │   │   ├── Login.jsx
│   │   │   ├── Signup.jsx
│   │   │   ├── Profile.jsx
│   │   │   ├── Stocks.jsx
│   │   │   ├── StockDetails.jsx
│   │   │   ├── Funds.jsx         # Funds management (Deposit/Withdraw)
│   │   │   ├── Wallet.jsx        # Wallet page with balance & transactions
│   │   │   ├── Trades.jsx        # Trade history & management
│   │   │   ├── Portfolio.jsx
│   │   │   ├── Orders.jsx        # Order book & trade history (Zerodha-style)
│   │   │   └── MarketData.jsx
│   │   ├── 📁 services/          # API service modules
│   │   │   ├── fundsApi.js       # Funds/Wallet API client
│   │   │   ├── paymentApi.js     # Payment gateway API client
│   │   │   └── tradeApi.js       # Trade management API client
│   │   └── 📁 context/           # React context (Auth)
│   ├── 📄 package.json
│   ├── 📄 vite.config.js
│   └── 📄 Dockerfile
│
├── 📁 observability/             # PLG Stack configurations
│   ├── 📁 prometheus/            # Prometheus metrics configuration
│   ├── 📁 loki/                  # Loki log aggregation config
│   └── 📁 grafana/               # Grafana dashboards & provisioning
│
├── 📁 cicd/                      # CI/CD configurations
├── 📁 docs/                      # Additional documentation
│   └── 📁 adr/                   # Architecture Decision Records (11 ADRs)
└── 📁 infra/                     # Infrastructure as Code
```

---

## 🚀 Quick Start

### Prerequisites

Ensure you have the following installed:
- **Java 21** - [Download](https://adoptium.net/temurin/releases/?version=21)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Node.js 20+** - [Download](https://nodejs.org/)
- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop)
- **Git** - [Download](https://git-scm.com/downloads)

### Running with Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/axshowk/winvestco-trading-platform.git
   cd winvestco-trading-platform
   ```

2. **Configure environment variables**
   ```bash
   cd backend
   cp .env.example .env
   # Edit .env with your configuration (see Environment Variables section)
   ```

3. **Start all services**
   ```bash
   docker-compose up -d
   ```

4. **Access the application**
   - **Frontend**: http://localhost:5173
   - **API Gateway**: http://localhost:8090
   - **Eureka Dashboard**: http://localhost:8761
   - **Grafana**: http://localhost:3000 (admin/winvestco)
   - **Prometheus**: http://localhost:9090
   - **RabbitMQ Management**: http://localhost:15672 (guest/guest)

### Running Locally (For Development)

1. **Start external services only**
   ```bash
   cd backend
   docker-compose -f docker-compose-services.yml up -d
   ```

2. **Create databases** (if not auto-created)
   ```sql
   CREATE DATABASE winvestco_user_db;
   CREATE DATABASE winvestco_market_db;
   CREATE DATABASE winvestco_portfolio_db;
   CREATE DATABASE winvestco_funds_db;
   CREATE DATABASE winvestco_ledger_db;
   CREATE DATABASE winvestco_order_db;
   CREATE DATABASE winvestco_trade_db;
   CREATE DATABASE winvestco_payment_db;
   CREATE DATABASE winvestco_notification_db;
   ```

3. **Build the backend**
   ```bash
   mvn clean install -DskipTests
   ```

4. **Start services in order**
   ```bash
   # Terminal 1: Eureka Server
   cd eureka-server && mvn spring-boot:run

   # Terminal 2: API Gateway (wait for Eureka to be healthy)
   cd api-gateway && mvn spring-boot:run

   # Terminal 3: User Service
   cd user-service && mvn spring-boot:run

   # Terminal 4: Market Service
   cd market-service && mvn spring-boot:run

   # Terminal 5: Portfolio Service
   cd portfolio-service && mvn spring-boot:run

   # Terminal 6: Funds Service (requires Ledger Service)
   cd funds-service && mvn spring-boot:run

   # Terminal 7: Ledger Service
   cd ledger-service && mvn spring-boot:run

   # Terminal 8: Order Service
   cd order-service && mvn spring-boot:run

   # Terminal 9: Trade Service
   cd trade-service && mvn spring-boot:run

   # Terminal 10: Payment Service
   cd payment-service && mvn spring-boot:run

   # Terminal 11: Notification Service
   cd notification-service && mvn spring-boot:run

   # Terminal 12: Schedule Service
   cd schedule-service && mvn spring-boot:run
   ```

5. **Start the frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

---

## ⚙️ Service Configuration

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 5173 | React development server |
| API Gateway | 8090 | Entry point for all API requests |
| Eureka Server | 8761 | Service discovery dashboard |
| User Service | 8088 | User management APIs |
| Market Service | 8084 | Market data APIs |
| Portfolio Service | 8085 | Portfolio & holdings management |
| Funds Service | 8086 | Wallet & funds management |
| Ledger Service | 8087 | Immutable ledger (source of truth) |
| Order Service | 8089 | Order management |
| Trade Service | 8092 | Trade lifecycle & execution |
| Payment Service | 8093 | Razorpay payment gateway |
| Notification Service | 8091 | Notifications & WebSocket |
| Report Service | 8094 | Async report generation |
| Schedule Service | 8095 | Centralized platform scheduling |
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache & session store |
| RabbitMQ | 5672 / 15672 | Message broker / Management UI |
| Kafka | 9092 | Event streaming |
| Zookeeper | 2181 | Kafka coordination |
| Prometheus | 9090 | Metrics collection |
| Loki | 3100 | Log aggregation |
| Grafana | 3000 | Dashboards |

### API Gateway Routes

| Route | Target Service | Description |
|-------|----------------|-------------|
| `/api/auth/**` | user-service | Authentication endpoints |
| `/api/users/**` | user-service | User management |
| `/api/market/**` | market-service | Market data |
| `/api/portfolios/**` | portfolio-service | Portfolio management |
| `/api/funds/**` | funds-service | Funds/wallet management |
| `/api/ledger/**` | ledger-service | Ledger queries (read-only) |
| `/api/orders/**` | order-service | Order management |
| `/api/trades/**` | trade-service | Trade management |
| `/api/payments/**` | payment-service | Payment operations |
| `/api/payments/webhook/**` | payment-service | Razorpay webhooks (public) |
| `/api/v1/notifications/**` | notification-service | Notifications |
| `/api/reports/**` | report-service | Report generation |
| `/ws/notifications/**` | notification-service | WebSocket endpoint |
| `/api/admin/docs/**` | user-service | API documentation |

---

## 📚 API Documentation

### Interactive Documentation (Swagger UI)

When services are running, access OpenAPI documentation at:
- **User Service**: http://localhost:8088/swagger-ui.html
- **Market Service**: http://localhost:8084/swagger-ui.html
- **Portfolio Service**: http://localhost:8085/swagger-ui.html
- **Funds Service**: http://localhost:8086/swagger-ui.html
- **Ledger Service**: http://localhost:8087/swagger-ui.html
- **Order Service**: http://localhost:8089/swagger-ui.html
- **Trade Service**: http://localhost:8092/swagger-ui.html
- **Payment Service**: http://localhost:8093/api/payments/swagger-ui.html
- **Notification Service**: http://localhost:8091/swagger-ui.html
- **Report Service**: http://localhost:8094/swagger-ui.html

### Key API Endpoints

#### Authentication
```
POST /api/auth/login          # Login with email/password
POST /api/auth/validate       # Validate JWT token
GET  /api/auth/me             # Get current user info
```

#### User Management
```
POST /api/users/register      # Register new user
GET  /api/users/{id}          # Get user by ID
PUT  /api/users/{id}          # Update user
```

#### Market Data
```
GET  /api/market/indices/{symbol}  # Get index data (e.g., NIFTY 50)
GET  /api/market/stocks/all        # Get all stocks from all indices
GET  /api/market/candles/{symbol}  # Get OHLC candle data
```

#### Portfolio Management
```
GET  /api/portfolios/user/{userId}    # Get user's portfolio
GET  /api/portfolios/{portfolioId}    # Get portfolio by ID
POST /api/portfolios/{id}/holdings    # Add holding to portfolio
GET  /api/portfolios/{id}/holdings    # Get all holdings
PUT  /api/portfolios/holdings/{id}    # Update holding
DELETE /api/portfolios/holdings/{id}  # Remove holding
```

#### Funds/Wallet Management
```
GET  /api/funds/wallet/{userId}        # Get wallet
POST /api/funds/wallet/{userId}/deposit     # Deposit funds
POST /api/funds/wallet/{userId}/withdraw    # Withdraw funds
POST /api/funds/lock                   # Lock funds for order
POST /api/funds/lock/{lockId}/release  # Release locked funds
GET  /api/funds/transactions/{userId}  # Get transaction history
```

#### Order Management
```
POST /api/orders                      # Create order
GET  /api/orders/{orderId}            # Get order by ID
GET  /api/orders/user/{userId}        # Get user's orders
GET  /api/orders/user/{userId}/active # Get active orders
POST /api/orders/{orderId}/cancel     # Cancel order
```

#### Trade Management
```
GET  /api/trades/{tradeId}            # Get trade by ID
GET  /api/trades/order/{orderId}      # Get trade by order ID
GET  /api/trades                      # Get user's trades (paginated)
GET  /api/trades/active               # Get active trades
POST /api/trades/{tradeId}/cancel     # Cancel trade
POST /api/trades/{tradeId}/simulate-execution  # Simulate execution (testing)
```

#### Payment Gateway (Razorpay)
```
POST /api/payments/initiate           # Initiate payment (creates Razorpay order)
POST /api/payments/verify             # Verify payment after checkout
GET  /api/payments/{id}               # Get payment by ID
GET  /api/payments/history            # Get payment history
POST /api/payments/{id}/pending       # Mark payment as pending
POST /api/payments/webhook/razorpay   # Razorpay webhook (public)
```

#### Notifications
```
GET  /api/v1/notifications            # Get notifications
POST /api/v1/notifications/{id}/read  # Mark as read
POST /api/v1/notifications/read-all   # Mark all as read
DELETE /api/v1/notifications/{id}     # Delete notification
WS   /ws/notifications                # WebSocket for real-time
```

#### Report Management
```
POST /api/reports/request/pnl          # Request P&L statement
POST /api/reports/request/tax          # Request tax report
POST /api/reports/request/transaction  # Request transaction history
GET  /api/reports/user/{userId}        # Get user's report requests
GET  /api/reports/{id}/download        # Download report file
GET  /api/reports/{id}/status          # Check report generation status
```

### Sample Requests

**Login:**
```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'
```

**Get NIFTY 50 Data:**
```bash
curl http://localhost:8090/api/market/indices/NIFTY%2050
```

**Create Order:**
```bash
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "symbol": "RELIANCE",
    "side": "BUY",
    "type": "LIMIT",
    "quantity": 10,
    "price": 2500.00,
    "validity": "DAY"
  }'
```

---

## 🔐 Environment Variables

Create a `.env` file in the `backend/` directory based on `.env.example`:

```env
# JWT Configuration (REQUIRED)
JWT_SECRET=your-super-secret-jwt-key-minimum-256-bits
JWT_EXPIRATION=86400000  # 24 hours

# Google OAuth2 (Required for social login)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Database
POSTGRES_PASSWORD=your-secure-password

# Message Broker
RABBITMQ_PASSWORD=guest

# Redis
SPRING_REDIS_PASSWORD=

# Razorpay (Payment Gateway)
RAZORPAY_KEY_ID=your-razorpay-key-id
RAZORPAY_KEY_SECRET=your-razorpay-key-secret
```

> ⚠️ **Security Note**: Never commit `.env` files to version control. The `.env.example` template is provided for reference.

---

### Backend Testing

The backend follows a rigorous testing strategy with JUnit 5 and Mockito.

1. **Clean & Build**
   ```bash
   mvn clean install -DskipTests
   ```

2. **Run All Tests**
   ```bash
   mvn test
   ```

3. **Check Coverage**
   JaCoCo is integrated into the build process. Reports are generated in:
   `backend/[service-name]/target/site/jacoco/index.html`

### Frontend Testing

The frontend uses Vitest and React Testing Library for component and hook testing.

1. **Install Dependencies**
   ```bash
   cd frontend && npm install
   ```

2. **Run Tests**
   ```bash
   npm run test
   ```

3. **Run with Coverage**
   ```bash
   npm run test:coverage
   ```

**Current Coverage Status:** 70+ tests, ~57% statement coverage across core components.

---

## 🔐 Security Features

- **JWT Authentication**: Secure tokens with RSA signing and 24h expiration.
- **OAuth2 / Google SSO**: Integrated social login for user convenience.
- **API Gateway Rate Limiting**: Redis-backed protection with 3 strategies:
    - `DEFAULT`: Per-IP limiting.
    - `USER`: Per-authenticated user limiting.
    - `COMBINED`: Hybrid limiting for maximum protection.
- **Secure Headers**: Spring Security configured with CSP, HSTS, X-Content-Type-Options, and Frame Options.
- **Input Validation**: Strict `@Valid` enforcement on all DTOs and API endpoints.
- **SQL Injection Protection**: Complete isolation through JPA/Hibernate parameterized queries.
- **HTTPS Enforcement**: Configurable via `security.enforce-https` in the Gateway.

---

## 🚀 Deployment & CI/CD

The platform is deployment-ready for cloud environments with a full CI/CD pipeline using GitHub Actions.

### Environments
- **dev**: Local development profile.
- **docker**: Optimized for Docker Compose local testing.
- **staging**: SSH-linked automated deployment to staging servers.
- **prod**: Manual approval-gated production deployment.

### CI/CD Workflow
1. **GitHub Action (CI)**: Runs on every PR. Executes `mvn build` and `npm run test`.
2. **GitHub Action (Release)**: Builds Docker images for all 13 services and pushes to Docker Hub.
3. **GitHub Action (Deploy)**: Connects via SSH to the target server and runs `docker-compose pull && docker-compose up -d`.

Tests use H2 in-memory database and mock external services. Test configurations are in:
- `src/test/resources/application-test.yml`

### Test Coverage

The project includes **32+ test classes** across all microservices, with a major focus on the User Service using:
- **JUnit 5 / Mockito**: For robust unit testing
- **JaCoCo**: For coverage analysis and reporting
- **Outbox Pattern Testing**: Comprehensive tests for reliable event publishing

| Module | Test Classes |
|--------|--------------|
| **Common** | `GlobalExceptionHandlerTest`, `ResourceNotFoundExceptionTest`, `NonRetryableExceptionTest`, `ResilienceEventLoggerTest`, `LoggingUtilsTest` |
| **API Gateway** | `JwtAuthenticationFilterTest`, `RateLimiterConfigTest` |
| **User Service** | `AuthControllerTest`, `UserServiceTest`, `JwtServiceTest`, `UserRepositoryTest`, `RegisterRequestTest` |
| **Market Service** | `MarketDataServiceTest`, `NseClientResilienceTest` |
| **Funds Service** | `WalletServiceTest`, `LedgerClientFallbackTest` |
| **Ledger Service** | `LedgerServiceTest` |
| **Order Service** | `OrderServiceTest`, `OrderServiceObservabilityTest` |
| **Trade Service** | `TradeServiceTest`, `TradeServiceObservabilityTest`, `MockExecutionEngineTest` |
| **Portfolio Service** | `PortfolioServiceTest` |
| **Payment Service** | `PaymentServiceTest`, `PaymentServiceObservabilityTest` |
| **Notification Service** | `NotificationServiceTest` |
| **Report Service** | `ReportServiceTest` |

- **Unit Tests**: Comprehensive JUnit 5 & Mockito tests for all microservices
- **Integration Tests**: Work in progress
- **Test Coverage**: JaCoCo configured for code coverage analysis

---

## ⚡ Performance Benchmarks

The platform leverages **Java 21 Virtual Threads** for exceptional concurrency performance. Below are stress test results from the User Service.

### Virtual Threads Stress Test Results

| Test | Concurrent Tasks | I/O Delay | Total Time | Speedup Factor | Virtual Threads |
|------|------------------|-----------|------------|----------------|-----------------|
| Light Load | 5,000 | 100ms | 2,262ms | **221x** | 100% |
| Medium Load | 10,000 | 100ms | 1,160ms | **862x** | 100% |
| Heavy Load | 20,000 | 50ms | 816ms | **1,225x** | 100% |
| **Extreme Load** | **50,000** | 50ms | 1,354ms | **1,846x** 🚀 | 100% |

### HTTP Endpoint Performance

| Metric | Value |
|--------|-------|
| Requests Tested | 100 sequential |
| Success Rate | **100%** |
| Avg Response Time | 93.52ms |
| Min Response Time | 16.67ms |
| Max Response Time | 686.17ms |

### Key Findings

- ✅ **50,000 concurrent I/O operations** handled successfully
- ✅ **100% Virtual Threads** utilization (0 platform threads blocked)
- ✅ **1,846x speedup** over theoretical sequential execution
- ✅ Java 21 Virtual Threads are production-ready

### Running Stress Tests

```bash
# Check virtual threads info
curl http://localhost:8088/api/stress-test/info

# Run stress test (1000 concurrent tasks, 100ms simulated I/O each)
curl "http://localhost:8088/api/stress-test?concurrentTasks=1000&sleepMs=100"

# Extreme stress test (50000 tasks)
curl "http://localhost:8088/api/stress-test?concurrentTasks=50000&sleepMs=50"
```

> 💡 **Note**: The stress test endpoint (`/api/stress-test/**`) is available on User Service for development/testing purposes.

---

## 🚢 Deployment

### Docker Production Build

```bash
# Build all images
docker-compose build

# Push to registry (example)
docker-compose push
```

### Multi-Stage Dockerfile

Each service uses optimized multi-stage builds:
1. **Build Stage**: Maven build with Eclipse Temurin JDK 21
2. **Runtime Stage**: Minimal Alpine JRE image

### Health Checks

All services expose health endpoints via Spring Actuator:
```
GET /actuator/health
GET /actuator/info
GET /actuator/prometheus  # Metrics for Prometheus
```

### Observability

Access the PLG Stack + Jaeger dashboards:
- **Grafana**: http://localhost:3000 (admin/winvestco)
  - Golden Signals Dashboard for SRE monitoring (Latency, Traffic, Errors, Saturation)
  - Custom alerting rules configured
- **Prometheus**: http://localhost:9090
- **Loki**: http://localhost:3100 (via Grafana)
- **Jaeger**: http://localhost:16686 (Distributed Tracing)

---

## 📋 Architecture Decision Records

We document significant architectural decisions using Architecture Decision Records (ADRs). These records explain why the system is built the way it is.

| ADR | Decision |
|-----|----------|
| [ADR-0001](docs/adr/0001-record-architecture-decisions.md) | Record Architecture Decisions |
| [ADR-0002](docs/adr/0002-microservices-architecture.md) | Microservices Architecture |
| [ADR-0003](docs/adr/0003-dual-message-broker-strategy.md) | Dual Message Broker (Kafka + RabbitMQ) |
| [ADR-0004](docs/adr/0004-event-driven-architecture.md) | Event-Driven Architecture |
| [ADR-0005](docs/adr/0005-database-per-service.md) | Database per Service |
| [ADR-0006](docs/adr/0006-java-21-virtual-threads.md) | Java 21 Virtual Threads |
| [ADR-0007](docs/adr/0007-immutable-ledger-source-of-truth.md) | Immutable Ledger as Source of Truth |
| [ADR-0008](docs/adr/0008-resilience4j-fault-tolerance.md) | Resilience4j for Fault Tolerance |
| [ADR-0009](docs/adr/0009-api-gateway-pattern.md) | API Gateway Pattern |
| [ADR-0010](docs/adr/0010-plg-stack-observability.md) | PLG Stack for Observability |
| [ADR-0011](docs/adr/0011-environment-specific-profiles.md) | Environment-Specific Profiles |

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Guidelines
- Follow Java coding conventions
- Write unit tests for new features
- Update documentation as needed
- Use meaningful commit messages
- Ensure all services use the common module for shared code

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 📞 Contact & Support

- **Author**: [@axshowk](https://github.com/axshowk)
- **Repository**: [winvestco-trading-platform](https://github.com/axshowk/winvestco-trading-platform)

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

Made with ❤️ for the trading community

</div>

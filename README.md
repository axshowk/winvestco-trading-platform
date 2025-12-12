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
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Features

### Core Trading Features
- **📈 Live Market Data** - Real-time stock prices and indices from NSE India
- **📊 Interactive Charts** - TradingView-powered stock charts with technical analysis
- **🔐 User Authentication** - Secure JWT-based authentication with OAuth2 support
- **👤 User Management** - Complete user registration, login, and profile management
- **📱 Responsive Design** - Mobile-first, modern UI built with React

### Technical Highlights
- **☁️ Cloud-Native Architecture** - Microservices with service discovery and API gateway
- **🔄 Event-Driven Communication** - Kafka for real-time market data streaming
- **💾 Redis Caching** - High-performance caching for market data
- **🐰 Message Queuing** - RabbitMQ for reliable async communication
- **📝 Database Migrations** - Flyway for version-controlled schema management
- **🛡️ API Security** - OAuth2/JWT authentication with Spring Security
- **📖 API Documentation** - OpenAPI/Swagger UI for all REST endpoints
- **🐳 Docker Support** - Complete containerization with Docker Compose
- **⚡ Virtual Threads** - Java 21 Virtual Threads for optimal performance

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              WINVESTCO PLATFORM                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐         ┌──────────────────────────────────────────────┐  │
│  │   React     │ ───────►│              API Gateway (8090)              │  │
│  │  Frontend   │         │  • JWT Validation  • Rate Limiting           │  │
│  │   (5173)    │         │  • OAuth2 Client   • Load Balancing          │  │
│  └─────────────┘         └──────────────────────────────────────────────┘  │
│                                         │                                   │
│                         ┌───────────────┴───────────────────┐              │
│                         ▼                                   ▼              │
│           ┌──────────────────────┐           ┌──────────────────────┐      │
│           │   User Service (9090)│           │Market Service (8084) │      │
│           │                      │           │                      │      │
│           │  • Authentication    │           │  • NSE India API     │      │
│           │  • Registration      │           │  • Live Market Data  │      │
│           │  • Profile Mgmt      │           │  • Stock Quotes      │      │
│           │  • JWT Generation    │           │  • Index Data        │      │
│           └──────────┬───────────┘           └──────────┬───────────┘      │
│                      │                                  │                   │
│           ┌──────────┴──────────┐            ┌─────────┴─────────┐         │
│           ▼          ▼          ▼            ▼                   ▼         │
│      ┌────────┐ ┌────────┐ ┌────────┐   ┌────────┐          ┌────────┐    │
│      │Postgres│ │ Redis  │ │RabbitMQ│   │ Kafka  │          │ Redis  │    │
│      │  DB    │ │ Cache  │ │ Queue  │   │ Stream │          │ Cache  │    │
│      └────────┘ └────────┘ └────────┘   └────────┘          └────────┘    │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                      Eureka Server (8761)                            │  │
│  │                    Service Discovery & Registry                       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
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
| **Resilience4j** | - | Circuit Breaker pattern |
| **Micrometer** | - | Distributed Tracing |

### Data & Messaging
| Technology | Purpose |
|------------|---------|
| **PostgreSQL 16** | Primary database for user data |
| **Redis** | Caching & session management |
| **Apache Kafka** | Market data streaming |
| **RabbitMQ** | Async messaging for user events |
| **Flyway** | Database migrations |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| **React** | 19.2 | UI Library |
| **Vite** | 7.2 | Build tool |
| **React Router** | 7.10 | Client-side routing |
| **Framer Motion** | 12.x | Animations |
| **Lucide React** | - | Icons |
| **TradingView** | - | Stock charts |

### DevOps & Tools
| Technology | Purpose |
|------------|---------|
| **Docker** | Containerization |
| **Docker Compose** | Multi-container orchestration |
| **Maven** | Build & dependency management |
| **Lombok** | Boilerplate reduction |
| **MapStruct** | Object mapping |
| **SpringDoc OpenAPI** | API documentation |

---

## 📁 Project Structure

```
winvestco-trading-platform/
├── 📄 LICENSE                    # MIT License
├── 📄 README.md                  # This file
│
├── 📁 backend/                   # Backend microservices
│   ├── 📄 pom.xml               # Parent POM (Maven multi-module)
│   ├── 📄 docker-compose.yml    # Full stack deployment
│   ├── 📄 docker-compose-services.yml  # External services only
│   ├── 📄 .env.example          # Environment variables template
│   │
│   ├── 📁 common/               # Shared library module
│   │   ├── 📁 config/           # Common configurations
│   │   ├── 📁 dto/              # Shared DTOs
│   │   ├── 📁 enums/            # Enumerations (Role, AccountStatus)
│   │   ├── 📁 event/            # Domain events (UserCreated, etc.)
│   │   ├── 📁 exception/        # Global exception handling
│   │   ├── 📁 security/         # JWT & auth utilities
│   │   └── 📁 util/             # Logging & utility classes
│   │
│   ├── 📁 eureka-server/        # Service Discovery (Port: 8761)
│   │   └── 📁 src/main/java/    # Eureka server application
│   │
│   ├── 📁 api-gateway/          # API Gateway (Port: 8090)
│   │   ├── 📁 config/           # Gateway & security config
│   │   ├── 📁 filter/           # Custom gateway filters
│   │   └── 📄 Dockerfile        # Container definition
│   │
│   ├── 📁 user-service/         # User Management (Port: 9090)
│   │   ├── 📁 controller/       # REST controllers
│   │   ├── 📁 service/          # Business logic
│   │   ├── 📁 repository/       # Data access layer
│   │   ├── 📁 model/            # JPA entities
│   │   ├── 📁 dto/              # Request/Response DTOs
│   │   ├── 📁 mapper/           # MapStruct mappers
│   │   ├── 📁 security/         # UserDetails, JWT filters
│   │   └── 📄 Dockerfile        # Container definition
│   │
│   ├── 📁 market-service/       # Market Data (Port: 8084)
│   │   ├── 📁 client/           # NSE India API client
│   │   ├── 📁 controller/       # REST controllers
│   │   ├── 📁 service/          # Market data processing
│   │   ├── 📁 scheduler/        # Scheduled data fetching
│   │   ├── 📁 messaging/        # Kafka publisher
│   │   ├── 📁 config/           # NSE & Kafka config
│   │   └── 📄 Dockerfile        # Container definition
│   │
│   └── 📁 frontend/             # React Frontend
│       ├── 📁 src/
│       │   ├── 📁 components/   # Reusable UI components
│       │   │   ├── Navbar.jsx
│       │   │   ├── Hero.jsx
│       │   │   ├── Features.jsx     # Landing page features
│       │   │   ├── CoinShower.jsx   # Animation component
│       │   │   ├── Footer.jsx
│       │   │   ├── Ticker.jsx
│       │   │   └── TradingViewChart.jsx
│       │   └── 📁 pages/        # Page components
│       │       ├── Login.jsx
│       │       ├── Signup.jsx
│       │       ├── Profile.jsx
│       │       ├── Stocks.jsx
│       │       └── MarketData.jsx
│       ├── 📄 package.json
│       ├── 📄 vite.config.js
│       └── 📄 Dockerfile
│
├── 📁 cicd/                     # CI/CD configurations (future)
├── 📁 docs/                     # Additional documentation
├── 📁 infra/                    # Infrastructure as Code (future)
└── 📁 observability/            # Monitoring & logging configs
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

   ```bash
   docker-compose up -d
   ```

4. **Access the application**
   - **Frontend**: http://localhost:5173
   - **API Gateway**: http://localhost:8090
   - **Eureka Dashboard**: http://localhost:8761
   - **RabbitMQ Management**: http://localhost:15672 (guest/guest)

### Running Locally (For Development)

1. **Start external services only**
   ```bash
   cd backend
   docker-compose -f docker-compose-services.yml up -d
   ```

2. **Build the backend**
   ```bash
   mvn clean install -DskipTests
   ```

3. **Start services in order**
   ```bash
   # Terminal 1: Eureka Server
   cd eureka-server && mvn spring-boot:run

   # Terminal 2: API Gateway (wait for Eureka to be healthy)
   cd api-gateway && mvn spring-boot:run

   # Terminal 3: User Service
   cd user-service && mvn spring-boot:run

   # Terminal 4: Market Service
   cd market-service && mvn spring-boot:run
   ```

4. **Start the frontend**
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
| User Service | 9090 | User management APIs |
| Market Service | 8084 | Market data APIs |
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache & session store |
| RabbitMQ | 5672 / 15672 | Message broker / Management UI |
| Kafka | 9092 | Event streaming |
| Zookeeper | 2181 | Kafka coordination |

### API Gateway Routes

| Route | Target Service | Description |
|-------|----------------|-------------|
| `/api/auth/**` | user-service | Authentication endpoints |
| `/api/users/**` | user-service | User management |
| `/api/market/**` | market-service | Market data |
| `/api/admin/docs/**` | user-service | API documentation |

---

## 📚 API Documentation

### Interactive Documentation (Swagger UI)

When services are running, access OpenAPI documentation at:
- **User Service**: http://localhost:9090/swagger-ui.html
- **API Gateway Aggregated**: http://localhost:8090/swagger-ui.html

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
```

> ⚠️ **Security Note**: Never commit `.env` files to version control. The `.env.example` template is provided for reference.

---

## 🧪 Testing

### Running Unit Tests

```bash
# Run all tests
cd backend
mvn test

# Run tests for a specific module
mvn test -pl user-service
mvn test -pl market-service
```

### Test Configuration

Tests use H2 in-memory database and mock external services. Test configurations are in:
- `src/test/resources/application-test.yml`

### Test Coverage

The project includes:
- **Unit Tests**: Service layer tests with mocked dependencies
- **Controller Tests**: `@WebMvcTest` based API tests
- **Integration Tests**: Full context tests with testcontainers (planned)

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
curl http://localhost:9090/api/stress-test/info

# Run stress test (1000 concurrent tasks, 100ms simulated I/O each)
curl "http://localhost:9090/api/stress-test?concurrentTasks=1000&sleepMs=100"

# Extreme stress test (50000 tasks)
curl "http://localhost:9090/api/stress-test?concurrentTasks=50000&sleepMs=50"
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
```

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

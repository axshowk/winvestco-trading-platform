# 📊 Winvestco Observability Stack Guide

The Winvestco Trading Platform uses a comprehensive **PLG + J Stack** (Prometheus, Loki, Grafana, and Jaeger) to provide full visibility into our microservices architecture.

---

## 🚀 Quick Access URLs

When running the stack via `docker-compose up -d`, you can access the following dashboards:

| Tool | Port | URL | Description |
|------|------|-----|-------------|
| **Grafana** | 3000 | [http://localhost:3000](http://localhost:3000) | Central visualization and dashboards (Admin/winvestco) |
| **Prometheus**| 9090 | [http://localhost:9090](http://localhost:9090) | Raw metrics collection and alerting rules |
| **Jaeger** | 16686| [http://localhost:16686](http://localhost:16686) | Distributed tracing UI |
| **Eureka** | 8761 | [http://localhost:8761](http://localhost:8761) | Service discovery status |

---

## 📈 1. Metrics (Prometheus & Grafana)

Each microservice is equipped with **Spring Boot Actuator** and **Micrometer**, exposing metrics at `/actuator/prometheus`.

### How to use:
1.  Open **Grafana**.
2.  Navigate to **Dashboards** > **Browse**.
3.  Choose a dashboard (e.g., `Spring Boot Statistics` or `JVM Micrometer`).
4.  Filter by `service` or `instance` to see real-time CPU, Memory, and Request performance.

---

## 📝 2. Log Aggregation (Loki)

Logs are aggregated using **Loki4j** and **Logback**, formatted as JSON, and pushed to Loki.

### How to use:
1.  Open **Grafana**.
2.  Go to **Explore** (Compass icon on the sidebar).
3.  Select **Loki** as the Data Source from the dropdown.
4.  Use the **Label Browser** to filter by `service`:
    *   Example: `{service="order-service"}`
5.  Click **Run Query** to view live logs.
6.  *Tip:* Use the search bar to find specific keywords across multiple services.

---

## 🕵️ 3. Distributed Tracing (Jaeger)

Distributed tracing allows you to track a single user request as it travels through multiple services (e.g., from `API Gateway` → `Order Service` → `Funds Service`).

### How to use:
1.  Open **Jaeger UI**.
2.  Select a service from the **Service** dropdown (e.g., `api-gateway`).
3.  Click **Find Traces**.
4.  Click on a trace to see the timeline and latency of each step.
5.  *Tip:* Search by `correlationId` or `traceId` found in logs to jump directly to the trace.

---

## 🛠 4. Adding Observability to New Services

When creating a new microservice, ensure it includes the following:

### 1. Dependencies (Maven)
In `pom.xml`, include the common module and observability dependencies:
```xml
<dependency>
    <groupId>in.winvestco</groupId>
    <artifactId>common</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
```

### 2. Configuration (`application.yml`)
Enable management endpoints and OTLP tracing:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

### 3. Prometheus Scrape Configuration
Add your service to `observability/prometheus/prometheus.yml`:
```yaml
- job_name: 'new-service'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['new-service:PORT']
      labels:
        service: 'new-service'
```

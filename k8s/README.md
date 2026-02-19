# Winvestco Kubernetes Deployment Guide

This directory contains comprehensive Kubernetes configuration for deploying the Winvestco Trading Platform microservices architecture.

## Overview

The Kubernetes configuration follows best practices:
- **Kustomize** for environment-specific configurations (dev, staging, production)
- **ConfigMaps** and **Secrets** for configuration management
- **Persistent Volume Claims** for database persistence
- **Ingress** for external access
- **Observability Stack** (Prometheus, Grafana, Jaeger, Loki)

## Directory Structure

```
k8s/
├── base/                           # Base Kustomize configuration
│   ├── namespace.yaml              # Namespace definitions
│   ├── configmaps.yaml             # Application ConfigMaps
│   ├── secrets.yaml                # Kubernetes Secrets
│   ├── pvc.yaml                    # Persistent Volume Claims
│   ├── ingress.yaml                # Ingress configuration
│   └── kustomization.yaml          # Base Kustomization manifest
├── infrastructure/                 # Infrastructure services
│   ├── postgres.yaml               # PostgreSQL database
│   ├── redis.yaml                  # Redis cache
│   ├── rabbitmq.yaml               # RabbitMQ message broker
│   ├── kafka.yaml                  # Apache Kafka
│   └── zookeeper.yaml              # Zookeeper for Kafka
├── services/                       # Application microservices
│   ├── eureka-server.yaml          # Service discovery
│   ├── api-gateway.yaml            # API Gateway
│   ├── user-service.yaml           # Identity & RBAC
│   ├── market-service.yaml         # Market data & gRPC streaming
│   ├── portfolio-service.yaml      # Portfolio management
│   ├── funds-service.yaml          # Wallet & funds
│   ├── ledger-service.yaml         # Immutable financial ledger
│   ├── order-service.yaml          # Order lifecycle
│   ├── trade-service.yaml          # Trade execution engine
│   ├── notification-service.yaml   # Notification engine
│   ├── payment-service.yaml        # Payment gateway
│   ├── report-service.yaml         # Analytics & reports
│   ├── risk-service.yaml           # Risk management
│   ├── schedule-service.yaml       # Job scheduler
│   └── frontend.yaml               # React frontend
├── observability/                  # Monitoring and logging
│   ├── prometheus-config.yaml      # Prometheus configuration
│   ├── prometheus.yaml             # Metrics collection
│   ├── grafana-secret.yaml         # Grafana credentials
│   ├── grafana.yaml                # Dashboards & visualization
│   ├── jaeger.yaml                 # Distributed tracing
│   └── loki.yaml                   # Log aggregation
└── overlays/                       # Environment overlays
    ├── dev/                        # Development environment
    ├── staging/                    # Staging environment
    └── production/                 # Production environment
```

## Prerequisites

- Kubernetes cluster (v1.25+)
- kubectl configured
- Kustomize (v5.0+)
- Docker Hub account (for image pulling)
- NGINX Ingress Controller
- cert-manager (for TLS certificates)

## Quick Start

### 1. Deploy to Development

```bash
# Navigate to the k8s directory
cd k8s

# Deploy using Kustomize (dev environment)
kubectl apply -k overlays/dev/

# Or apply base configuration directly
kubectl apply -k base/
```

### 2. Deploy to Staging

```bash
kubectl apply -k overlays/staging/
```

### 3. Deploy to Production

```bash
kubectl apply -k overlays/production/
```

## Service Architecture

### Infrastructure Components

| Component | Port | Purpose |
|-----------|------|---------|
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache & session store |
| RabbitMQ | 5672 / 15672 | Message broker & management UI |
| Kafka | 9092 | Event streaming |
| Zookeeper | 2181 | Kafka coordination |

### Application Services

| Service | Port | Replicas (Dev/Staging/Prod) |
|---------|------|----------------------------|
| Eureka Server | 8761 | 1/2/3 |
| API Gateway | 8090 | 1/2/3 |
| User Service | 8088 | 1/2/3 |
| Market Service | 8084 | 1/2/3 |
| Portfolio Service | 8086 | 1/2/3 |
| Funds Service | 8085 | 1/2/3 |
| Ledger Service | 8082 | 1/2/3 |
| Order Service | 8081 | 1/2/3 |
| Trade Service | 8083 | 1/2/3 |
| Notification Service | 8089 | 1/2/3 |
| Payment Service | 8087 | 1/2/3 |
| Report Service | 8091 | 1/2/2 |
| Risk Service | 8092 | 1/2/3 |
| Schedule Service | 8095 | 1/1/2 |
| Frontend | 80 | 1/2/3 |

### Observability Stack

| Component | Port | Purpose |
|-----------|------|---------|
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Visualization dashboards |
| Jaeger | 16686 | Distributed tracing |
| Loki | 3100 | Log aggregation |

## Configuration

### Environment Variables

Base configuration is defined in `base/configmaps.yaml`:
- Database connection strings
- Message broker endpoints
- Service discovery URLs
- OpenTelemetry settings

### Secrets

Sensitive data is stored in `base/secrets.yaml`:
- Database passwords
- JWT secrets
- Payment gateway API keys
- Email/SMS credentials

**IMPORTANT**: Replace placeholder values in secrets before deployment!

### Ingress Configuration

External access is configured in `base/ingress.yaml`:
- API Gateway: `api.winvestco.com`
- Frontend: `app.winvestco.com`
- TLS certificates via cert-manager

## Scaling

### Horizontal Pod Autoscaler (HPA)

To add HPA for automatic scaling:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: winvestco
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

## Monitoring & Logging

### Accessing Observability Tools

```bash
# Port-forward Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n winvestco

# Port-forward Grafana
kubectl port-forward svc/grafana 3000:3000 -n winvestco

# Port-forward Jaeger
kubectl port-forward svc/jaeger 16686:16686 -n winvestco
```

### Default Credentials

- **Grafana**: admin / winvestco_grafana_secure_2024

## Troubleshooting

### Check Pod Status

```bash
kubectl get pods -n winvestco
kubectl describe pod <pod-name> -n winvestco
kubectl logs <pod-name> -n winvestco
```

### Verify Services

```bash
kubectl get svc -n winvestco
kubectl get ingress -n winvestco
```

### Check ConfigMaps and Secrets

```bash
kubectl get configmaps -n winvestco
kubectl get secrets -n winvestco
```

## Security Considerations

1. **Secrets Management**: Use external secret management (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault) for production
2. **Network Policies**: Implement NetworkPolicies for inter-service communication
3. **RBAC**: Configure Role-Based Access Control for service accounts
4. **Pod Security**: Enable Pod Security Standards (restricted profile)
5. **Resource Limits**: All containers have resource requests and limits defined

## CI/CD Integration

Example GitHub Actions workflow step:

```yaml
- name: Deploy to Kubernetes
  run: |
    kubectl apply -k k8s/overlays/production/
    kubectl rollout status deployment/api-gateway -n winvestco
    kubectl rollout status deployment/trade-service -n winvestco
```

## Maintenance

### Update Images

```bash
# Set new image tag
kubectl set image deployment/api-gateway api-gateway=myrepo/api-gateway:v2.0.0 -n winvestco

# Or update via Kustomize overlay and reapply
kubectl apply -k overlays/production/
```

### Database Migrations

Database migrations should be run as Kubernetes Jobs before deploying application updates.

### Backup & Recovery

PostgreSQL backups can be configured using:
- Velero for cluster-wide backups
- pg_dump CronJobs for database dumps
- Persistent Volume snapshots

## Resource Requirements

### Minimum Cluster Requirements

| Environment | Nodes | CPU | Memory | Storage |
|-------------|-------|-----|--------|---------|
| Development | 1-2 | 4 cores | 16 GB | 50 GB |
| Staging | 2-3 | 8 cores | 32 GB | 100 GB |
| Production | 3-5 | 16+ cores | 64+ GB | 500 GB |

## Support

For issues or questions regarding Kubernetes deployment:
1. Check pod logs: `kubectl logs <pod-name> -n winvestco`
2. Verify events: `kubectl get events -n winvestco`
3. Review resource usage: `kubectl top pods -n winvestco`

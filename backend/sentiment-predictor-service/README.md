# Sentiment Predictor Service

A FastAPI-based microservice for financial news sentiment analysis using FinBERT.

## Overview

This service provides REST API endpoints for analyzing sentiment in financial text using the FinBERT pre-trained model. It's designed to integrate with the `news-sentiment-service` as part of the WINVESTCO trading platform.

## Features

- Single text sentiment prediction
- Batch prediction for multiple texts
- Health check endpoints for Kubernetes probes
- Configurable model settings
- Prometheus metrics support

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/predict` | Analyze sentiment of a single text |
| `POST` | `/api/v1/predict/batch` | Analyze sentiment of multiple texts |
| `GET` | `/health` | Health check endpoint |
| `GET` | `/health/ready` | Readiness probe |
| `GET` | `/health/live` | Liveness probe |

## Running Locally

### Prerequisites

- Python 3.11+
- pip

### Installation

```bash
cd sentiment-predictor-service
pip install -r requirements.txt
```

### Running

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8096 --reload
```

### Testing

```bash
# Single prediction
curl -X POST http://localhost:8096/api/v1/predict \
  -H "Content-Type: application/json" \
  -d '{"text": "Stock market sees record gains"}'

# Batch prediction
curl -X POST http://localhost:8096/api/v1/predict/batch \
  -H "Content-Type: application/json" \
  -d '{"texts": ["Market crashes", "Strong earnings reported"]}'
```

## Docker

```bash
# Build
docker build -t sentiment-predictor-service .

# Run
docker run -p 8096:8096 sentiment-predictor-service
```

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `MODEL_NAME` | `ProsusAI/finbert` | Hugging Face model name |
| `DEVICE` | `cpu` | Device for inference (cpu/cuda) |
| `LOG_LEVEL` | `INFO` | Logging level |
| `MAX_BATCH_SIZE` | `100` | Maximum batch size for predictions |
| `PORT` | `8096` | Server port |

## Model Information

This service uses [FinBERT](https://huggingface.co/ProsusAI/finbert), a BERT-based model fine-tuned for financial sentiment analysis. It classifies text into three categories:
- **positive**: Bullish/optimistic sentiment
- **negative**: Bearish/pessimistic sentiment  
- **neutral**: No clear sentiment

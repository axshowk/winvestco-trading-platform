"""
FastAPI application entry point for Sentiment Predictor Service.
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .config import get_settings
from .model import get_model
from .schemas import (
    BatchPredictRequest,
    BatchPredictResponse,
    ErrorResponse,
    HealthResponse,
    PredictRequest,
    PredictResponse,
    SentimentScores,
)

# Configure logging
settings = get_settings()
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper()),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan handler.
    Loads the model on startup and cleans up on shutdown.
    """
    # Startup
    logger.info("Starting Sentiment Predictor Service...")
    logger.info(f"Model: {settings.model_name}")
    logger.info(f"Device: {settings.device}")
    
    try:
        model = get_model()
        model.load()
        logger.info("Model loaded successfully!")
    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        # Don't raise - allow service to start for health checks to report unhealthy
    
    yield
    
    # Shutdown
    logger.info("Shutting down Sentiment Predictor Service...")


# Create FastAPI application
app = FastAPI(
    title="Sentiment Predictor Service",
    description="FinBERT-based financial sentiment analysis API",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# =============================================================================
# Health Check Endpoints
# =============================================================================

@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["Health"],
    summary="Health check"
)
async def health_check() -> HealthResponse:
    """General health check endpoint."""
    model = get_model()
    return HealthResponse(
        status="healthy" if model.is_loaded else "unhealthy",
        service=settings.service_name,
        model_loaded=model.is_loaded,
        version="1.0.0"
    )


@app.get(
    "/health/ready",
    response_model=HealthResponse,
    tags=["Health"],
    summary="Readiness probe"
)
async def readiness_check() -> HealthResponse:
    """
    Kubernetes readiness probe.
    Returns 200 only if the model is loaded and ready.
    """
    model = get_model()
    if not model.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model not loaded"
        )
    
    return HealthResponse(
        status="healthy",
        service=settings.service_name,
        model_loaded=True,
        version="1.0.0",
        details={"model": settings.model_name}
    )


@app.get(
    "/health/live",
    response_model=HealthResponse,
    tags=["Health"],
    summary="Liveness probe"
)
async def liveness_check() -> HealthResponse:
    """
    Kubernetes liveness probe.
    Returns 200 if the service is running (regardless of model state).
    """
    model = get_model()
    return HealthResponse(
        status="healthy",
        service=settings.service_name,
        model_loaded=model.is_loaded,
        version="1.0.0"
    )


# =============================================================================
# Prediction Endpoints
# =============================================================================

@app.post(
    f"{settings.api_v1_prefix}/predict",
    response_model=PredictResponse,
    responses={
        500: {"model": ErrorResponse},
        503: {"model": ErrorResponse}
    },
    tags=["Prediction"],
    summary="Predict sentiment for a single text"
)
async def predict_sentiment(request: PredictRequest) -> PredictResponse:
    """
    Analyze the sentiment of a single text using FinBERT.
    
    Returns the predicted sentiment (positive, negative, or neutral),
    confidence score, and individual scores for each category.
    """
    model = get_model()
    
    if not model.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model not loaded. Service is starting up."
        )
    
    try:
        result = model.predict(request.text)
        return PredictResponse(
            text=result["text"],
            sentiment=result["sentiment"],
            confidence=result["confidence"],
            scores=SentimentScores(**result["scores"])
        )
    except Exception as e:
        logger.error(f"Prediction error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Prediction failed: {str(e)}"
        )


@app.post(
    f"{settings.api_v1_prefix}/predict/batch",
    response_model=BatchPredictResponse,
    responses={
        400: {"model": ErrorResponse},
        500: {"model": ErrorResponse},
        503: {"model": ErrorResponse}
    },
    tags=["Prediction"],
    summary="Predict sentiment for multiple texts"
)
async def predict_sentiment_batch(request: BatchPredictRequest) -> BatchPredictResponse:
    """
    Analyze the sentiment of multiple texts using FinBERT.
    
    More efficient than calling single prediction multiple times
    as it batches the inference.
    """
    model = get_model()
    
    if not model.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model not loaded. Service is starting up."
        )
    
    if len(request.texts) > settings.max_batch_size:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Batch size exceeds maximum of {settings.max_batch_size}"
        )
    
    try:
        results = model.predict_batch(request.texts)
        predictions = [
            PredictResponse(
                text=r["text"],
                sentiment=r["sentiment"],
                confidence=r["confidence"],
                scores=SentimentScores(**r["scores"])
            )
            for r in results
        ]
        
        return BatchPredictResponse(
            predictions=predictions,
            total=len(predictions)
        )
    except Exception as e:
        logger.error(f"Batch prediction error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Batch prediction failed: {str(e)}"
        )


# =============================================================================
# Root Endpoint
# =============================================================================

@app.get("/", include_in_schema=False)
async def root():
    """Root endpoint - redirects to documentation."""
    return {
        "service": "Sentiment Predictor Service",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health"
    }


# =============================================================================
# Exception Handlers
# =============================================================================

@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """Global exception handler for unhandled errors."""
    logger.error(f"Unhandled exception: {exc}")
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"error": "Internal server error", "detail": str(exc)}
    )

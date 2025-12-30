"""
Pydantic schemas for API request/response models.
"""

from typing import Optional
from pydantic import BaseModel, Field


class SentimentScores(BaseModel):
    """Sentiment scores for each category."""
    positive: float = Field(..., ge=0.0, le=1.0, description="Positive sentiment score")
    negative: float = Field(..., ge=0.0, le=1.0, description="Negative sentiment score")
    neutral: float = Field(..., ge=0.0, le=1.0, description="Neutral sentiment score")


class PredictRequest(BaseModel):
    """Request model for single text prediction."""
    text: str = Field(..., min_length=1, max_length=5000, description="Text to analyze")
    
    model_config = {
        "json_schema_extra": {
            "examples": [
                {"text": "Stock market sees record gains amid positive earnings reports"}
            ]
        }
    }


class PredictResponse(BaseModel):
    """Response model for single text prediction."""
    text: str = Field(..., description="Original input text (may be truncated)")
    sentiment: str = Field(..., description="Predicted sentiment: positive, negative, or neutral")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence score of the prediction")
    scores: SentimentScores = Field(..., description="Scores for each sentiment category")
    
    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "text": "Stock market sees record gains...",
                    "sentiment": "positive",
                    "confidence": 0.9234,
                    "scores": {
                        "positive": 0.9234,
                        "negative": 0.0321,
                        "neutral": 0.0445
                    }
                }
            ]
        }
    }


class BatchPredictRequest(BaseModel):
    """Request model for batch text prediction."""
    texts: list[str] = Field(
        ..., 
        min_length=1, 
        max_length=100,
        description="List of texts to analyze"
    )
    
    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "texts": [
                        "Market crashes due to inflation fears",
                        "Company announces strong quarterly results"
                    ]
                }
            ]
        }
    }


class BatchPredictResponse(BaseModel):
    """Response model for batch text prediction."""
    predictions: list[PredictResponse] = Field(..., description="List of predictions")
    total: int = Field(..., description="Total number of predictions")


class HealthResponse(BaseModel):
    """Response model for health check endpoints."""
    status: str = Field(..., description="Health status: healthy, unhealthy")
    service: str = Field(..., description="Service name")
    model_loaded: bool = Field(..., description="Whether the ML model is loaded")
    version: str = Field(default="1.0.0", description="Service version")
    details: Optional[dict] = Field(default=None, description="Additional health details")


class ErrorResponse(BaseModel):
    """Response model for error responses."""
    error: str = Field(..., description="Error message")
    detail: Optional[str] = Field(default=None, description="Detailed error information")

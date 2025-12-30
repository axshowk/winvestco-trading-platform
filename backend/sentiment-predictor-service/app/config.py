"""
Configuration settings for the Sentiment Predictor Service.
Uses Pydantic Settings for environment variable management.
"""

from functools import lru_cache
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # Model Configuration
    model_name: str = "ProsusAI/finbert"
    device: str = "cpu"  # cpu or cuda
    
    # API Configuration
    api_v1_prefix: str = "/api/v1"
    
    # Service Configuration
    service_name: str = "sentiment-predictor-service"
    port: int = 8096
    log_level: str = "INFO"
    
    # Batch Processing
    max_batch_size: int = 100
    max_text_length: int = 512  # FinBERT max token length
    
    # Model Caching
    cache_dir: str = "/app/model_cache"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()

"""
FinBERT Model wrapper for sentiment analysis.
Provides loading, caching, and inference functionality.
"""

import logging
from typing import Optional

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

from .config import get_settings

logger = logging.getLogger(__name__)


class SentimentModel:
    """
    Wrapper class for FinBERT sentiment analysis model.
    Implements singleton pattern for model caching.
    """
    
    _instance: Optional["SentimentModel"] = None
    _initialized: bool = False
    
    # FinBERT label mapping
    LABEL_MAP = {0: "positive", 1: "negative", 2: "neutral"}
    
    def __new__(cls) -> "SentimentModel":
        """Ensure only one instance of the model exists."""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        """Initialize the model (only once due to singleton pattern)."""
        if SentimentModel._initialized:
            return
            
        self.settings = get_settings()
        self.model = None
        self.tokenizer = None
        self.device = None
        self._is_loaded = False
        
        SentimentModel._initialized = True
    
    def load(self) -> None:
        """Load the FinBERT model and tokenizer."""
        if self._is_loaded:
            logger.info("Model already loaded, skipping...")
            return
            
        logger.info(f"Loading FinBERT model: {self.settings.model_name}")
        logger.info(f"Device: {self.settings.device}")
        
        try:
            # Set device
            self.device = torch.device(self.settings.device)
            
            # Load tokenizer
            logger.info("Loading tokenizer...")
            self.tokenizer = AutoTokenizer.from_pretrained(
                self.settings.model_name,
                cache_dir=self.settings.cache_dir
            )
            
            # Load model
            logger.info("Loading model weights...")
            self.model = AutoModelForSequenceClassification.from_pretrained(
                self.settings.model_name,
                cache_dir=self.settings.cache_dir
            )
            self.model.to(self.device)
            self.model.eval()
            
            self._is_loaded = True
            logger.info("FinBERT model loaded successfully!")
            
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            raise RuntimeError(f"Failed to load FinBERT model: {e}")
    
    @property
    def is_loaded(self) -> bool:
        """Check if the model is loaded."""
        return self._is_loaded
    
    def predict(self, text: str) -> dict:
        """
        Predict sentiment for a single text.
        
        Args:
            text: Input text to analyze
            
        Returns:
            Dictionary with sentiment, confidence, and scores
        """
        if not self._is_loaded:
            raise RuntimeError("Model not loaded. Call load() first.")
        
        # Tokenize input
        inputs = self.tokenizer(
            text,
            return_tensors="pt",
            truncation=True,
            max_length=self.settings.max_text_length,
            padding=True
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        
        # Run inference
        with torch.no_grad():
            outputs = self.model(**inputs)
            logits = outputs.logits
            
        # Apply softmax to get probabilities
        probabilities = torch.nn.functional.softmax(logits, dim=-1)
        probs = probabilities[0].cpu().numpy()
        
        # Get predicted class
        predicted_class = int(torch.argmax(probabilities, dim=-1).item())
        sentiment = self.LABEL_MAP[predicted_class]
        confidence = float(probs[predicted_class])
        
        return {
            "text": text[:200] + "..." if len(text) > 200 else text,
            "sentiment": sentiment,
            "confidence": round(confidence, 4),
            "scores": {
                "positive": round(float(probs[0]), 4),
                "negative": round(float(probs[1]), 4),
                "neutral": round(float(probs[2]), 4)
            }
        }
    
    def predict_batch(self, texts: list[str]) -> list[dict]:
        """
        Predict sentiment for multiple texts.
        
        Args:
            texts: List of input texts to analyze
            
        Returns:
            List of dictionaries with sentiment results
        """
        if not self._is_loaded:
            raise RuntimeError("Model not loaded. Call load() first.")
        
        if len(texts) > self.settings.max_batch_size:
            raise ValueError(f"Batch size exceeds maximum of {self.settings.max_batch_size}")
        
        # Tokenize all inputs
        inputs = self.tokenizer(
            texts,
            return_tensors="pt",
            truncation=True,
            max_length=self.settings.max_text_length,
            padding=True
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        
        # Run inference
        with torch.no_grad():
            outputs = self.model(**inputs)
            logits = outputs.logits
        
        # Apply softmax to get probabilities
        probabilities = torch.nn.functional.softmax(logits, dim=-1)
        probs_array = probabilities.cpu().numpy()
        
        # Process each prediction
        results = []
        for i, (text, probs) in enumerate(zip(texts, probs_array)):
            predicted_class = int(probs.argmax())
            sentiment = self.LABEL_MAP[predicted_class]
            confidence = float(probs[predicted_class])
            
            results.append({
                "text": text[:200] + "..." if len(text) > 200 else text,
                "sentiment": sentiment,
                "confidence": round(confidence, 4),
                "scores": {
                    "positive": round(float(probs[0]), 4),
                    "negative": round(float(probs[1]), 4),
                    "neutral": round(float(probs[2]), 4)
                }
            })
        
        return results


def get_model() -> SentimentModel:
    """Get the singleton model instance."""
    return SentimentModel()

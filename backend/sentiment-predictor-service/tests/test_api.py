"""
Unit tests for the Sentiment Predictor Service API.
"""

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def client():
    """Create test client."""
    return TestClient(app)


class TestHealthEndpoints:
    """Tests for health check endpoints."""
    
    def test_health_check(self, client):
        """Test the general health check endpoint."""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert "service" in data
        assert data["service"] == "sentiment-predictor-service"
    
    def test_liveness_check(self, client):
        """Test the liveness probe endpoint."""
        response = client.get("/health/live")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
    
    def test_root_endpoint(self, client):
        """Test the root endpoint."""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "service" in data
        assert data["service"] == "Sentiment Predictor Service"


class TestPredictionEndpoints:
    """Tests for prediction endpoints."""
    
    def test_predict_request_validation(self, client):
        """Test request validation for predict endpoint."""
        # Empty text should fail
        response = client.post(
            "/api/v1/predict",
            json={"text": ""}
        )
        assert response.status_code == 422
    
    def test_batch_predict_request_validation(self, client):
        """Test request validation for batch predict endpoint."""
        # Empty list should fail
        response = client.post(
            "/api/v1/predict/batch",
            json={"texts": []}
        )
        assert response.status_code == 422
    
    def test_batch_predict_max_size(self, client):
        """Test max batch size validation."""
        # Create a list larger than max batch size
        texts = ["test"] * 101
        response = client.post(
            "/api/v1/predict/batch",
            json={"texts": texts}
        )
        # Should fail due to batch size limit
        assert response.status_code in [400, 422]


class TestSchemaValidation:
    """Tests for Pydantic schema validation."""
    
    def test_predict_missing_text(self, client):
        """Test predict endpoint with missing text field."""
        response = client.post(
            "/api/v1/predict",
            json={}
        )
        assert response.status_code == 422
    
    def test_batch_predict_missing_texts(self, client):
        """Test batch predict endpoint with missing texts field."""
        response = client.post(
            "/api/v1/predict/batch",
            json={}
        )
        assert response.status_code == 422

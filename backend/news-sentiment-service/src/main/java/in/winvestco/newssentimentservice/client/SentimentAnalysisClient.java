package in.winvestco.newssentimentservice.client;

import in.winvestco.newssentimentservice.dto.BatchPredictRequest;
import in.winvestco.newssentimentservice.dto.BatchPredictResponse;
import in.winvestco.newssentimentservice.dto.SentimentPredictRequest;
import in.winvestco.newssentimentservice.dto.SentimentPrediction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * HTTP client for communicating with the FastAPI Sentiment Predictor Service.
 */
@Component
@Slf4j
public class SentimentAnalysisClient {

    private final RestTemplate restTemplate;

    @Value("${sentiment.service.url:http://localhost:8096}")
    private String sentimentServiceUrl;

    @Value("${sentiment.service.timeout:30000}")
    private int timeout;

    public SentimentAnalysisClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Analyze sentiment for a single text.
     *
     * @param text The text to analyze
     * @return Optional containing the prediction, or empty if the call fails
     */
    public Optional<SentimentPrediction> analyzeSentiment(String text) {
        String endpoint = sentimentServiceUrl + "/api/v1/predict";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            SentimentPredictRequest request = SentimentPredictRequest.builder()
                    .text(text)
                    .build();

            HttpEntity<SentimentPredictRequest> entity = new HttpEntity<>(request, headers);

            SentimentPrediction response = restTemplate.postForObject(
                    endpoint,
                    entity,
                    SentimentPrediction.class);

            return Optional.ofNullable(response);

        } catch (RestClientException e) {
            log.error("Failed to call sentiment service for single text: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Analyze sentiment for multiple texts in a batch.
     *
     * @param texts List of texts to analyze
     * @return Optional containing the batch response, or empty if the call fails
     */
    public Optional<BatchPredictResponse> analyzeBatch(List<String> texts) {
        String endpoint = sentimentServiceUrl + "/api/v1/predict/batch";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            BatchPredictRequest request = BatchPredictRequest.builder()
                    .texts(texts)
                    .build();

            HttpEntity<BatchPredictRequest> entity = new HttpEntity<>(request, headers);

            BatchPredictResponse response = restTemplate.postForObject(
                    endpoint,
                    entity,
                    BatchPredictResponse.class);

            return Optional.ofNullable(response);

        } catch (RestClientException e) {
            log.error("Failed to call sentiment service for batch: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if the sentiment service is healthy.
     *
     * @return true if the service is healthy, false otherwise
     */
    public boolean isServiceHealthy() {
        String endpoint = sentimentServiceUrl + "/health/ready";

        try {
            restTemplate.getForEntity(endpoint, String.class);
            return true;
        } catch (RestClientException e) {
            log.warn("Sentiment service health check failed: {}", e.getMessage());
            return false;
        }
    }
}

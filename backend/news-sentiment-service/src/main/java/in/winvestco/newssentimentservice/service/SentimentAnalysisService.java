package in.winvestco.newssentimentservice.service;

import in.winvestco.newssentimentservice.client.SentimentAnalysisClient;
import in.winvestco.newssentimentservice.dto.BatchPredictResponse;
import in.winvestco.newssentimentservice.dto.SentimentPrediction;
import in.winvestco.newssentimentservice.entity.NewsArticle;
import in.winvestco.newssentimentservice.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for analyzing sentiment of news articles.
 * Coordinates between the repository and the external sentiment predictor
 * service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisService {

    private final NewsArticleRepository newsArticleRepository;
    private final SentimentAnalysisClient sentimentClient;

    @Value("${sentiment.service.batch-size:50}")
    private int batchSize;

    /**
     * Analyze sentiment for all articles that haven't been processed yet.
     * Uses batch processing for efficiency.
     */
    @Transactional
    public void analyzeUnprocessedArticles() {
        log.info("Starting sentiment analysis for unprocessed articles...");

        // Check if sentiment service is available
        if (!sentimentClient.isServiceHealthy()) {
            log.warn("Sentiment service is not available. Skipping analysis.");
            return;
        }

        List<NewsArticle> unanalyzedArticles = newsArticleRepository.findBySentimentIsNull();

        if (unanalyzedArticles.isEmpty()) {
            log.info("No unanalyzed articles found.");
            return;
        }

        log.info("Found {} articles to analyze.", unanalyzedArticles.size());

        // Process in batches
        List<List<NewsArticle>> batches = partitionList(unanalyzedArticles, batchSize);

        int processedCount = 0;
        int failedCount = 0;

        for (List<NewsArticle> batch : batches) {
            try {
                processedCount += processBatch(batch);
            } catch (Exception e) {
                log.error("Error processing batch: {}", e.getMessage());
                failedCount += batch.size();
            }
        }

        log.info("Sentiment analysis completed. Processed: {}, Failed: {}",
                processedCount, failedCount);
    }

    /**
     * Process a batch of articles for sentiment analysis.
     */
    private int processBatch(List<NewsArticle> articles) {
        // Extract texts for analysis (using title + description)
        List<String> texts = articles.stream()
                .map(this::getTextForAnalysis)
                .collect(Collectors.toList());

        // Call sentiment service
        Optional<BatchPredictResponse> response = sentimentClient.analyzeBatch(texts);

        if (response.isEmpty() || response.get().getPredictions() == null) {
            log.warn("Empty response from sentiment service for batch of {} articles",
                    articles.size());
            return 0;
        }

        List<SentimentPrediction> predictions = response.get().getPredictions();

        // Update articles with sentiment data
        int updated = 0;
        for (int i = 0; i < articles.size() && i < predictions.size(); i++) {
            NewsArticle article = articles.get(i);
            SentimentPrediction prediction = predictions.get(i);

            article.setSentiment(prediction.getSentiment());
            article.setSentimentConfidence(prediction.getConfidence());
            article.setSentimentAnalyzedAt(LocalDateTime.now());

            newsArticleRepository.save(article);
            updated++;
        }

        log.debug("Updated {} articles with sentiment data", updated);
        return updated;
    }

    /**
     * Get the text to analyze for a news article.
     * Combines title and description for better context.
     */
    private String getTextForAnalysis(NewsArticle article) {
        StringBuilder text = new StringBuilder();

        if (article.getTitle() != null) {
            text.append(article.getTitle());
        }

        if (article.getDescription() != null && !article.getDescription().isEmpty()) {
            text.append(". ").append(article.getDescription());
        }

        return text.toString();
    }

    /**
     * Partition a list into smaller sublists of the given size.
     */
    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();

        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }

        return partitions;
    }

    /**
     * Analyze sentiment for a single article.
     *
     * @param articleId The ID of the article to analyze
     * @return true if analysis was successful, false otherwise
     */
    @Transactional
    public boolean analyzeArticle(Long articleId) {
        Optional<NewsArticle> articleOpt = newsArticleRepository.findById(articleId);

        if (articleOpt.isEmpty()) {
            log.warn("Article not found with ID: {}", articleId);
            return false;
        }

        NewsArticle article = articleOpt.get();
        String text = getTextForAnalysis(article);

        Optional<SentimentPrediction> prediction = sentimentClient.analyzeSentiment(text);

        if (prediction.isEmpty()) {
            log.warn("Failed to get sentiment for article ID: {}", articleId);
            return false;
        }

        article.setSentiment(prediction.get().getSentiment());
        article.setSentimentConfidence(prediction.get().getConfidence());
        article.setSentimentAnalyzedAt(LocalDateTime.now());

        newsArticleRepository.save(article);
        log.info("Updated article {} with sentiment: {}",
                articleId, prediction.get().getSentiment());

        return true;
    }
}

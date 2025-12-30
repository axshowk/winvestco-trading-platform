package in.winvestco.newssentimentservice.scheduler;

import in.winvestco.newssentimentservice.service.SentimentAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for automatic sentiment analysis of news articles.
 * Runs periodically to analyze any unprocessed articles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisScheduler {

    private final SentimentAnalysisService sentimentAnalysisService;

    /**
     * Run sentiment analysis 5 minutes after news scraping.
     * This gives time for the news scraper to complete first.
     * 
     * Fixed rate: 30 minutes (1800000 ms) - same as news scraper
     * Initial delay: 5 minutes (300000 ms) - offset from news scraping
     */
    @Scheduled(fixedRate = 1800000, initialDelay = 300000)
    public void scheduleSentimentAnalysis() {
        log.info("Triggering scheduled sentiment analysis task...");
        try {
            sentimentAnalysisService.analyzeUnprocessedArticles();
        } catch (Exception e) {
            log.error("Error during scheduled sentiment analysis: {}", e.getMessage(), e);
        }
    }
}

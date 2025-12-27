package in.winvestco.newssentimentservice.scheduler;

import in.winvestco.newssentimentservice.service.NewsScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsScraperScheduler {

    private final NewsScraperService newsScraperService;

    // Run every 30 minutes
    @Scheduled(fixedRate = 1800000)
    public void scheduleScraping() {
        log.info("Triggering scheduled news scraping task...");
        newsScraperService.scrapeNews();
    }
}

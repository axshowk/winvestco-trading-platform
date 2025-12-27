package in.winvestco.newssentimentservice.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import in.winvestco.newssentimentservice.entity.NewsArticle;
import in.winvestco.newssentimentservice.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsScraperService {

    private final NewsArticleRepository newsArticleRepository;
    private static final String GOOGLE_NEWS_URL = "https://news.google.com/rss/search?q=NSE+India+Stock+Market&hl=en-IN&gl=IN&ceid=IN:en";

    @Transactional
    public void scrapeNews() {
        log.info("Starting Google News scraping...");
        try {
            URL feedSource = new URL(GOOGLE_NEWS_URL);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedSource));

            List<NewsArticle> newArticles = new ArrayList<>();
            for (SyndEntry entry : feed.getEntries()) {
                String link = entry.getLink();
                if (!newsArticleRepository.existsByLink(link)) {
                    NewsArticle article = NewsArticle.builder()
                            .title(entry.getTitle())
                            .link(link)
                            .description(entry.getDescription() != null ? entry.getDescription().getValue() : "")
                            .pubDate(entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                            .source(entry.getSource() != null ? entry.getSource().getTitle() : "Google News")
                            .build();
                    newArticles.add(article);
                }
            }

            if (!newArticles.isEmpty()) {
                newsArticleRepository.saveAll(newArticles);
                log.info("Scraped and saved {} new articles.", newArticles.size());
            } else {
                log.info("No new articles found.");
            }

        } catch (Exception e) {
            log.error("Error scraping Google News", e);
        }
    }
}

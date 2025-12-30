-- Add sentiment analysis fields to news_articles table
ALTER TABLE news_articles ADD COLUMN sentiment VARCHAR(20);

ALTER TABLE news_articles
ADD COLUMN sentiment_confidence DOUBLE PRECISION;

ALTER TABLE news_articles ADD COLUMN sentiment_analyzed_at TIMESTAMP;

-- Create index for querying by sentiment
CREATE INDEX idx_news_articles_sentiment ON news_articles (sentiment);

-- Create partial index for efficient querying of unanalyzed articles
CREATE INDEX idx_news_articles_unanalyzed ON news_articles (id)
WHERE
    sentiment IS NULL;
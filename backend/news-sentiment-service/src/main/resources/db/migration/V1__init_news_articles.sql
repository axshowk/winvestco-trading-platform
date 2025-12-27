CREATE TABLE IF NOT EXISTS news_articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(1000) NOT NULL,
    link VARCHAR(1000) NOT NULL UNIQUE,
    description TEXT,
    pub_date TIMESTAMP NOT NULL,
    source VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_news_articles_pub_date ON news_articles (pub_date);
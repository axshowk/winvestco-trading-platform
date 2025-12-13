-- Flyway migration: Create candles table for OHLCV data storage
CREATE TABLE candles (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(50) NOT NULL,
    interval_type VARCHAR(10) NOT NULL, -- '5m', '15m', '1h', '1d'
    timestamp TIMESTAMP NOT NULL,
    open DECIMAL(15, 2) NOT NULL,
    high DECIMAL(15, 2) NOT NULL,
    low DECIMAL(15, 2) NOT NULL,
    close DECIMAL(15, 2) NOT NULL,
    volume BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_candles_symbol_interval_time UNIQUE (
        symbol,
        interval_type,
        timestamp
    )
);

-- Index for efficient queries by symbol, interval, and time range
CREATE INDEX idx_candles_symbol_interval_time ON candles (
    symbol,
    interval_type,
    timestamp DESC
);
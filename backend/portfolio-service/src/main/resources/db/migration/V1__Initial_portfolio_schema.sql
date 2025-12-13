-- V1__Initial_portfolio_schema.sql
-- Initial schema for portfolio service (PostgreSQL)

-- Create portfolios table (one portfolio per user)
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_invested NUMERIC(18, 4) DEFAULT 0,
    current_value NUMERIC(18, 4) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

-- Create indexes for portfolios table
CREATE INDEX idx_portfolios_user_id ON portfolios (user_id);

CREATE INDEX idx_portfolios_status ON portfolios (status);

-- Create holdings table
CREATE TABLE holdings (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(100),
    exchange VARCHAR(10) DEFAULT 'NSE',
    quantity NUMERIC(18, 4) NOT NULL,
    average_price NUMERIC(18, 4) NOT NULL,
    total_invested NUMERIC(18, 4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_holdings_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
    CONSTRAINT uk_holdings_portfolio_symbol UNIQUE (portfolio_id, symbol)
);

-- Create indexes for holdings table
CREATE INDEX idx_holdings_portfolio_id ON holdings (portfolio_id);

CREATE INDEX idx_holdings_symbol ON holdings (symbol);
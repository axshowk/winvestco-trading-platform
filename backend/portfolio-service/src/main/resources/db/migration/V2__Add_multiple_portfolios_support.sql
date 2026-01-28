-- V2__Add_multiple_portfolios_support.sql
-- Allow multiple portfolios per user and add portfolio type and default flag

-- 1. Remove the unique constraint on user_id
-- The constraint name is typically portfolios_user_id_key for a UNIQUE column
ALTER TABLE portfolios
DROP CONSTRAINT IF EXISTS portfolios_user_id_key;

-- 2. Add new columns
ALTER TABLE portfolios
ADD COLUMN portfolio_type VARCHAR(20) DEFAULT 'MAIN';

ALTER TABLE portfolios ADD COLUMN is_default BOOLEAN DEFAULT false;

-- 3. Update existing records to be the default MAIN portfolio
UPDATE portfolios
SET
    portfolio_type = 'MAIN',
    is_default = true
WHERE
    is_default = false;

-- 4. Add unique constraint for default portfolio (only one default per user)
CREATE UNIQUE INDEX idx_portfolios_user_default ON portfolios (user_id)
WHERE
    is_default = true;

-- 5. Add index for portfolio_type
CREATE INDEX idx_portfolios_type ON portfolios (portfolio_type);
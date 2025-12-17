-- Add Zerodha-style trading columns to orders table

-- Product type (CNC/MIS/NRML)
ALTER TABLE orders ADD COLUMN product_type VARCHAR(10) DEFAULT 'CNC';

-- Order variety (REGULAR/CO)
ALTER TABLE orders ADD COLUMN variety VARCHAR(15) DEFAULT 'REGULAR';

-- Trigger price for Cover Order stop-loss
ALTER TABLE orders ADD COLUMN trigger_price DECIMAL(18, 4);

-- Disclosed quantity for iceberg orders
ALTER TABLE orders ADD COLUMN disclosed_quantity DECIMAL(18, 4);

-- Parent order ID for child orders in Cover Orders
ALTER TABLE orders ADD COLUMN parent_order_id VARCHAR(36);

-- Index for parent order lookup (for Cover Orders)
CREATE INDEX idx_orders_parent_order_id ON orders (parent_order_id)
WHERE
    parent_order_id IS NOT NULL;
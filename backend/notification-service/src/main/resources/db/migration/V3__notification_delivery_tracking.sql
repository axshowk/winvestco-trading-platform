-- V3: Notification delivery tracking for guaranteed delivery
-- Adds delivery tracking with retry mechanism and dead letter queue

-- Notification deliveries table
CREATE TABLE notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    destination VARCHAR(512),
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_attempted_at TIMESTAMP,
    last_attempted_at TIMESTAMP,
    delivered_at TIMESTAMP,
    next_retry_at TIMESTAMP,
    error_message TEXT,
    error_code VARCHAR(50),
    CONSTRAINT uk_delivery_notification_channel UNIQUE (notification_id, channel)
);

-- Indexes for efficient retry processing
CREATE INDEX idx_deliveries_pending ON notification_deliveries (status, next_retry_at)
WHERE
    status IN ('PENDING', 'RETRYING');

CREATE INDEX idx_deliveries_channel_pending ON notification_deliveries (channel, status)
WHERE
    status IN ('PENDING', 'RETRYING');

CREATE INDEX idx_deliveries_notification ON notification_deliveries (notification_id);

CREATE INDEX idx_deliveries_failed ON notification_deliveries (status, last_attempted_at)
WHERE
    status = 'FAILED';

CREATE INDEX idx_deliveries_dead_letter ON notification_deliveries (status, last_attempted_at)
WHERE
    status = 'DEAD_LETTER';

-- Comments
COMMENT ON TABLE notification_deliveries IS 'Tracks delivery status for each notification-channel combination';

COMMENT ON COLUMN notification_deliveries.status IS 'PENDING, IN_PROGRESS, DELIVERED, RETRYING, FAILED, SKIPPED, DEAD_LETTER';

COMMENT ON COLUMN notification_deliveries.destination IS 'Channel-specific destination (email, phone, fcm_token)';

COMMENT ON COLUMN notification_deliveries.next_retry_at IS 'When to attempt next retry (exponential backoff)';
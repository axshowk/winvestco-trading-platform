-- V2: Multi-channel notification delivery support
-- Adds notification_channels table for per-user, per-type channel preferences

-- Notification channels table
CREATE TABLE notification_channels (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    websocket_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    fcm_token VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_channels_user_type UNIQUE (user_id, notification_type)
);

-- Index for efficient lookups
CREATE INDEX idx_notification_channels_user ON notification_channels (user_id);

CREATE INDEX idx_notification_channels_push ON notification_channels (push_enabled)
WHERE
    push_enabled = TRUE;

CREATE INDEX idx_notification_channels_email ON notification_channels (email_enabled)
WHERE
    email_enabled = TRUE;

CREATE INDEX idx_notification_channels_sms ON notification_channels (sms_enabled)
WHERE
    sms_enabled = TRUE;

-- Add priority column to notifications table
ALTER TABLE notifications
ADD COLUMN IF NOT EXISTS priority VARCHAR(20) DEFAULT 'MEDIUM';

-- Add index for priority-based queries
CREATE INDEX IF NOT EXISTS idx_notifications_priority ON notifications (priority, created_at DESC);

-- comment explaining the channels
COMMENT ON TABLE notification_channels IS 'User preferences for notification delivery channels per notification type';

COMMENT ON COLUMN notification_channels.websocket_enabled IS 'Real-time WebSocket delivery (requires active browser session)';

COMMENT ON COLUMN notification_channels.push_enabled IS 'Firebase Cloud Messaging push notifications (mobile/PWA)';

COMMENT ON COLUMN notification_channels.email_enabled IS 'Email delivery via SendGrid or AWS SES';

COMMENT ON COLUMN notification_channels.sms_enabled IS 'SMS delivery via Twilio (critical notifications only)';

COMMENT ON COLUMN notification_channels.fcm_token IS 'Firebase Cloud Messaging device token';
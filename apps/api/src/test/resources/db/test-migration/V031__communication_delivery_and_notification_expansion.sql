-- ============================================================================
-- Phase 7C: Communication Delivery Tracking & Notification Expansion (Test H2)
-- ============================================================================

-- 1. Communication Deliveries Table
CREATE TABLE IF NOT EXISTS communication_deliveries (
    id uuid NOT NULL PRIMARY KEY,
    studio_id uuid NULL,
    recipient_user_id uuid NULL,
    channel varchar(32) NOT NULL,
    event_type varchar(64) NOT NULL,
    recipient varchar(255) NOT NULL,
    subject_or_summary varchar(512) NULL,
    status varchar(32) NOT NULL,
    provider varchar(64) NOT NULL,
    provider_message_id varchar(255) NULL,
    attempt_count int NOT NULL DEFAULT 0,
    max_attempts int NOT NULL DEFAULT 3,
    last_error text NULL,
    next_retry_at timestamptz NULL,
    idempotency_key varchar(255) NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    delivered_at timestamptz NULL
);

CREATE INDEX IF NOT EXISTS idx_comm_deliveries_studio ON communication_deliveries (studio_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comm_deliveries_recipient ON communication_deliveries (recipient_user_id, created_at DESC);

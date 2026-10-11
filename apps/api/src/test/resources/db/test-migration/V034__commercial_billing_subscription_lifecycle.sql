-- ============================================================================
-- V034: Commercial Billing & Subscription Lifecycle Architecture (Test H2)
-- ============================================================================

CREATE TABLE IF NOT EXISTS billing_checkout_sessions (
    id uuid PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    plan_id uuid NOT NULL REFERENCES billing_plans(id),
    provider varchar(64) NOT NULL,
    provider_session_id varchar(128) NULL,
    idempotency_key varchar(128) NOT NULL,
    status varchar(32) NOT NULL,
    amount_minor bigint NOT NULL,
    currency varchar(3) NOT NULL DEFAULT 'INR',
    success_url varchar(1024) NOT NULL,
    cancel_url varchar(1024) NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_checkout_studio_idempotency UNIQUE (studio_id, idempotency_key),
    CONSTRAINT chk_checkout_amount CHECK (amount_minor >= 0),
    CONSTRAINT chk_checkout_status CHECK (status IN ('PENDING', 'COMPLETED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_checkout_sessions_studio ON billing_checkout_sessions (studio_id, created_at DESC);

ALTER TABLE studio_subscriptions ADD COLUMN IF NOT EXISTS source_type varchar(32) NOT NULL DEFAULT 'PAID_PROVIDER';
ALTER TABLE studio_subscriptions ADD COLUMN IF NOT EXISTS downgrade_plan_id uuid NULL REFERENCES billing_plans(id);
ALTER TABLE studio_subscriptions ADD COLUMN IF NOT EXISTS grace_period_end timestamptz NULL;

UPDATE studio_subscriptions SET source_type = 'MANUAL_ADMIN' WHERE provider = 'ADMIN_OVERRIDE';
UPDATE studio_subscriptions SET source_type = 'SYSTEM' WHERE provider = 'NONE';
